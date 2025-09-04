package cn.edu.gfkd.evidence.service;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;
import cn.edu.gfkd.evidence.exception.BlockchainException;
import cn.edu.gfkd.evidence.repository.BlockchainEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 区块链事件存储服务实现
 * 
 * 主要职责：
 * 1. 提供区块链事件的CRUD操作
 * 2. 管理事件处理状态
 * 3. 支持事务性操作和重试机制
 * 4. 提供事件查询和统计功能
 */
@Service @RequiredArgsConstructor @Slf4j
public class EventStorageServiceImpl implements EventStorageService {

    private final BlockchainEventRepository blockchainEventRepository;
    private final RetryHandler retryHandler;

    // 同步锁对象，用于保护数据库操作
    private final Object dbLock = new Object();

    @Override
    @Transactional
    public BlockchainEvent saveEvent(BlockchainEvent event) {
        log.debug("Saving blockchain event: contract={}, eventType={}, blockNumber={}, txHash={}", 
                event.getContractAddress(), event.getEventName(), 
                event.getBlockNumber(), event.getTransactionHash());

        return retryHandler.executeWithRetrySync(
            () -> {
                // 检查事件是否已存在，避免重复处理
                boolean exists = blockchainEventRepository.existsByContractAddressAndBlockNumberAndTransactionHashAndEventName(
                    event.getContractAddress(), 
                    event.getBlockNumber(), 
                    event.getTransactionHash(), 
                    event.getEventName()
                );
                
                if (exists) {
                    log.debug("Event already exists, skipping: txHash={}, eventType={}", 
                            event.getTransactionHash(), event.getEventName());
                    // 返回已存在的事件
                    return blockchainEventRepository.findByContractAddressAndBlockNumberAndTransactionHashAndEventName(
                        event.getContractAddress(), 
                        event.getBlockNumber(), 
                        event.getTransactionHash(), 
                        event.getEventName()
                    ).orElse(event);
                }
                
                // 设置创建时间
                if (event.getCreatedAt() == null) {
                    event.setCreatedAt(LocalDateTime.now());
                }
                
                BlockchainEvent savedEvent = blockchainEventRepository.save(event);
                log.info("Saved blockchain event with id: {} for evidenceId: {}", 
                        savedEvent.getId(), event.getEventName());
                
                return savedEvent;
            },
            "save blockchain event",
            dbLock
        );
    }

    @Override
    @Transactional
    public List<BlockchainEvent> saveEvents(List<BlockchainEvent> events) {
        log.debug("Batch saving {} blockchain events", events.size());
        
        return retryHandler.executeWithRetryTransactional(
            () -> {
                List<BlockchainEvent> savedEvents = blockchainEventRepository.saveAll(events);
                log.info("Successfully saved {} blockchain events", savedEvents.size());
                return savedEvents;
            },
            "batch save blockchain events"
        );
    }

    @Override
    public List<BlockchainEvent> findByTransactionHash(String transactionHash) {
        log.debug("Finding events by transaction hash: {}", transactionHash);
        
        return retryHandler.executeWithRetry(
            () -> blockchainEventRepository.findByTransactionHash(transactionHash),
            "find events by transaction hash"
        );
    }

    @Override
    public List<BlockchainEvent> findByTransactionHashAndEventType(String transactionHash, String eventType) {
        log.debug("Finding events by transaction hash: {} and event type: {}", transactionHash, eventType);
        
        return retryHandler.executeWithRetry(
            () -> blockchainEventRepository.findByTransactionHashAndEventName(transactionHash, eventType),
            "find events by transaction hash and event type"
        );
    }

    @Override
    public Page<BlockchainEvent> findUnprocessedEvents(Pageable pageable) {
        log.debug("Finding unprocessed events with pagination: {}", pageable);
        
        return retryHandler.executeWithRetry(
            () -> blockchainEventRepository.findByIsProcessedFalse(pageable),
            "find unprocessed events"
        );
    }

    @Override
    public Page<BlockchainEvent> findUnprocessedEventsByEventType(String eventType, Pageable pageable) {
        log.debug("Finding unprocessed events by type: {} with pagination: {}", eventType, pageable);
        
        return retryHandler.executeWithRetry(
            () -> blockchainEventRepository.findByEventNameAndIsProcessedFalse(eventType, pageable),
            "find unprocessed events by type"
        );
    }

    @Override
    @Transactional
    public void markEventAsProcessed(Long eventId) {
        log.debug("Marking event as processed: id={}", eventId);
        
        retryHandler.executeWithRetryTransactional(
            () -> {
                BlockchainEvent event = blockchainEventRepository.findById(eventId)
                    .orElseThrow(() -> new BlockchainException("Event not found with id: " + eventId));
                
                event.setIsProcessed(true);
                event.setProcessedAt(LocalDateTime.now());
                event.setProcessingFailures(0); // 重置失败次数
                
                BlockchainEvent updatedEvent = blockchainEventRepository.save(event);
                log.info("Marked event as processed: id={}, transactionHash={}, eventType={}", 
                        updatedEvent.getId(), updatedEvent.getTransactionHash(), updatedEvent.getEventName());
                
                return null;
            },
            "mark event as processed"
        );
    }

    @Override
    @Transactional
    public void markEventAsProcessed(String transactionHash, String eventType) {
        log.debug("Marking events as processed: transactionHash={}, eventType={}", transactionHash, eventType);
        
        retryHandler.executeWithRetryTransactional(
            () -> {
                List<BlockchainEvent> events = blockchainEventRepository.findByTransactionHash(transactionHash);
                
                for (BlockchainEvent event : events) {
                    if (event.getEventName().equals(eventType) && !event.getIsProcessed()) {
                        event.setIsProcessed(true);
                        event.setProcessedAt(LocalDateTime.now());
                        event.setProcessingFailures(0); // 重置失败次数
                        
                        blockchainEventRepository.save(event);
                        log.info("Marked event as processed: id={}, transactionHash={}, eventType={}", 
                                event.getId(), transactionHash, eventType);
                    }
                }
                
                return null;
            },
            "mark events as processed by transaction hash"
        );
    }

    @Override
    public String getEventRawData(Long eventId) {
        log.debug("Getting raw data for event: id={}", eventId);
        
        return retryHandler.executeWithRetry(
            () -> blockchainEventRepository.findById(eventId)
                .map(BlockchainEvent::getRawData)
                .orElseThrow(() -> new BlockchainException("Event not found with id: " + eventId)),
            "get event raw data"
        );
    }

    @Override
    @Transactional
    public int deleteEventsBefore(LocalDateTime beforeTime) {
        log.debug("Deleting events before: {}", beforeTime);
        
        return retryHandler.executeWithRetryTransactional(
            () -> {
                int deletedCount = blockchainEventRepository.deleteByCreatedAtBefore(beforeTime);
                log.info("Deleted {} events before {}", deletedCount, beforeTime);
                return deletedCount;
            },
            "delete events before time"
        );
    }

    @Override
    public long countUnprocessedEvents(String eventType) {
        log.debug("Counting unprocessed events for type: {}", eventType);
        
        return retryHandler.executeWithRetry(
            () -> eventType != null 
                ? blockchainEventRepository.countByEventNameAndIsProcessedFalse(eventType)
                : blockchainEventRepository.countByIsProcessedFalse(),
            "count unprocessed events"
        );
    }

    @Override
    public int getEventProcessingFailures(Long eventId) {
        log.debug("Getting processing failures for event: id={}", eventId);
        
        return retryHandler.executeWithRetry(
            () -> blockchainEventRepository.findById(eventId)
                .map(BlockchainEvent::getProcessingFailures)
                .orElse(0),
            "get event processing failures"
        );
    }

    @Override
    @Transactional
    public void incrementEventProcessingFailures(Long eventId) {
        log.debug("Incrementing processing failures for event: id={}", eventId);
        
        retryHandler.executeWithRetryTransactional(
            () -> {
                BlockchainEvent event = blockchainEventRepository.findById(eventId)
                    .orElseThrow(() -> new BlockchainException("Event not found with id: " + eventId));
                
                int currentFailures = event.getProcessingFailures() != null ? event.getProcessingFailures() : 0;
                event.setProcessingFailures(currentFailures + 1);
                
                blockchainEventRepository.save(event);
                log.debug("Event processing failures incremented to: {} for id: {}", 
                        event.getProcessingFailures(), eventId);
                
                return null;
            },
            "increment event processing failures"
        );
    }

    @Override
    public boolean eventExists(String contractAddress, BigInteger blockNumber, String transactionHash, String eventType) {
        log.debug("Checking if event exists: contract={}, block={}, txHash={}, type={}", 
                contractAddress, blockNumber, transactionHash, eventType);
        
        return retryHandler.executeWithRetry(
            () -> blockchainEventRepository.existsByContractAddressAndBlockNumberAndTransactionHashAndEventName(
                contractAddress, blockNumber, transactionHash, eventType),
            "check if event exists"
        );
    }
}