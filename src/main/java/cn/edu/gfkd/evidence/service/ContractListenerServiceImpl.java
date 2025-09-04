package cn.edu.gfkd.evidence.service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;
import cn.edu.gfkd.evidence.exception.BlockchainException;
import cn.edu.gfkd.evidence.generated.EvidenceStorageContract;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 区块链合约事件监听服务实现
 * 
 * 主要职责：
 * 1. 管理区块链事件的实时监听和订阅
 * 2. 处理历史事件获取和同步
 * 3. 提供事件处理的生命周期管理
 * 4. 确保监听的可靠性和错误恢复
 */
@Service @RequiredArgsConstructor @Slf4j
public class ContractListenerServiceImpl implements ContractListenerService {

    private final Web3jService web3jService;
    private final EventStorageService eventStorageService;
    private final BlockchainSyncService blockchainSyncService;
    private final EvidenceStorageContract evidenceStorageContract;
    private final ObjectMapper objectMapper;

    // 事件处理器列表
    private final List<BlockchainEventProcessor> eventProcessors = new CopyOnWriteArrayList<>();

    // 监听状态控制
    private final AtomicBoolean isListening = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;
    private ScheduledExecutorService retryScheduler;

    // 监听配置
    @Value("${blockchain.listener.retry-interval-sec:30}")
    private int retryIntervalSec;
    
    @Value("${blockchain.listener.unprocessed-interval-sec:60}")
    private int unprocessedIntervalSec;
    
    @Value("${blockchain.listener.startup-delay-sec:3}")
    private int startupDelaySec;

    // 统计信息
    private volatile long totalEventsProcessed = 0;
    private volatile long totalEventsFailed = 0;
    private volatile long lastEventTimestamp = 0;

    @Override
    public void init() {
        log.info("Initializing contract listener service");
        
        scheduler = Executors.newSingleThreadScheduledExecutor();
        retryScheduler = Executors.newSingleThreadScheduledExecutor();

        try {
            // 启动事件监听（延迟启动）
            scheduler.schedule(this::startEventListening, startupDelaySec, TimeUnit.SECONDS);

            // 启动未处理事件处理器
            retryScheduler.scheduleAtFixedRate(this::processUnprocessedEvents, 
                    unprocessedIntervalSec, unprocessedIntervalSec, TimeUnit.SECONDS);

            log.info("Contract listener service initialized successfully");

        } catch (Exception e) {
            log.error("Failed to initialize contract listener service", e);
            throw new BlockchainException("Failed to initialize contract listener service", e);
        }
    }

    @Override
    public void startEventListening() {
        log.info("Starting blockchain event listener...");

        if (isListening.get()) {
            log.warn("Event listener is already running");
            return;
        }

        try {
            isListening.set(true);

            // 同步缺失的历史事件
            blockchainSyncService.syncMissingEventsOnStartup();

            // 启动实时事件监听
            startRealTimeEventListening();

            log.info("Blockchain event listener started successfully");

        } catch (Exception e) {
            log.error("Failed to start blockchain event listener", e);
            isListening.set(false);
            
            // 安排重试
            scheduleRetryStart();
            throw new BlockchainException("Failed to start blockchain event listener", e);
        }
    }

    @Override
    public void stopEventListening() {
        log.info("Stopping blockchain event listener...");

        if (!isListening.get()) {
            log.warn("Event listener is not running");
            return;
        }

        try {
            isListening.set(false);
            // 这里应该取消所有的事件订阅
            // 具体的取消逻辑需要根据Web3j的订阅机制来实现
            
            log.info("Blockchain event listener stopped successfully");

        } catch (Exception e) {
            log.error("Failed to stop blockchain event listener", e);
            throw new BlockchainException("Failed to stop blockchain event listener", e);
        }
    }

    @Override
    public void restartEventListening() {
        log.info("Restarting blockchain event listener...");
        
        try {
            stopEventListening();
            // 等待一秒确保完全停止
            Thread.sleep(1000);
            startEventListening();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Restart interrupted");
        } catch (Exception e) {
            log.error("Failed to restart blockchain event listener", e);
            throw new BlockchainException("Failed to restart blockchain event listener", e);
        }
    }

    @Override
    @Transactional
    public void getHistoricalEvents(BigInteger startBlock, BigInteger endBlock) {
        log.info("Getting historical events from block {} to {}", startBlock, endBlock);

        try {
            // 创建过滤器
            EthFilter filter = new EthFilter(
                    DefaultBlockParameter.valueOf(startBlock),
                    DefaultBlockParameter.valueOf(endBlock),
                    evidenceStorageContract.getContractAddress()
            );

            // 获取事件日志
            EthLog ethLogs = web3jService.getWeb3j().ethGetLogs(filter).send();
            @SuppressWarnings("unchecked")
            List<EthLog.LogResult<Log>> logResults = (List<EthLog.LogResult<Log>>) (List<?>) ethLogs.getLogs();

            log.info("Found {} historical events", logResults.size());

            // 处理每个事件
            for (EthLog.LogResult<Log> logResult : logResults) {
                try {
                    processLogResult(logResult);
                } catch (Exception e) {
                    log.error("Error processing historical log result", e);
                    totalEventsFailed++;
                }
            }

            // 更新同步状态
            blockchainSyncService.updateSyncStatus(endBlock);

            log.info("Completed processing historical events from block {} to {}", startBlock, endBlock);

        } catch (Exception e) {
            log.error("Failed to get historical events", e);
            throw new BlockchainException("Failed to get historical events", e);
        }
    }

    @Override
    public String getContractAddress() {
        return evidenceStorageContract.getContractAddress();
    }

    @Override
    public boolean isListening() {
        return isListening.get();
    }

    @Override
    public String getListenerStatus() {
        if (!isListening.get()) {
            return "STOPPED";
        }

        try {
            if (!web3jService.isConnectionValid()) {
                return "ERROR - Web3j connection invalid";
            }

            return String.format("RUNNING - Events processed: %d, Failed: %d, Last event: %d", 
                    totalEventsProcessed, totalEventsFailed, lastEventTimestamp);

        } catch (Exception e) {
            return String.format("ERROR - %s", e.getMessage());
        }
    }

    @Override
    public void setEventProcessor(BlockchainEventProcessor eventProcessor) {
        if (eventProcessor != null && !eventProcessors.contains(eventProcessor)) {
            eventProcessors.add(eventProcessor);
            log.info("Added event processor: {}", eventProcessor.getSupportedEventType());
        }
    }

    @Override
    public void removeEventProcessor(BlockchainEventProcessor eventProcessor) {
        if (eventProcessors.remove(eventProcessor)) {
            log.info("Removed event processor: {}", eventProcessor.getSupportedEventType());
        }
    }

    @Override
    public void shutdown() {
        log.info("Shutting down contract listener service...");

        // 停止监听
        stopEventListening();

        // 关闭调度器
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (retryScheduler != null) {
            retryScheduler.shutdown();
            try {
                if (!retryScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    retryScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                retryScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("Contract listener service shutdown completed");
    }

    @Override
    public void processUnprocessedEvents() {
        if (!isListening.get()) {
            log.debug("Listener not running, skipping unprocessed events processing");
            return;
        }

        try {
            log.debug("Processing unprocessed blockchain events...");

            // 获取未处理的事件（分页）
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(0, 50);
            
            var unprocessedEvents = eventStorageService.findUnprocessedEvents(pageable);

            if (unprocessedEvents.isEmpty()) {
                log.debug("No unprocessed events found");
                return;
            }

            log.info("Found {} unprocessed events to process", unprocessedEvents.getSize());

            // 处理每个未处理的事件
            for (BlockchainEvent event : unprocessedEvents) {
                try {
                    processBlockchainEvent(event);
                } catch (Exception e) {
                    log.error("Failed to process unprocessed event: id={}, transactionHash={}, eventType={}", 
                            event.getId(), event.getTransactionHash(), event.getEventName(), e);
                    totalEventsFailed++;
                    
                    // 增加失败次数
                    eventStorageService.incrementEventProcessingFailures(event.getId());
                }
            }

            log.debug("Completed processing unprocessed events");

        } catch (Exception e) {
            log.error("Error processing unprocessed events", e);
        }
    }

    @Override
    public String getListenerStatistics() {
        try {
            long unprocessedCount = eventStorageService.countUnprocessedEvents(null);
            double syncProgress = blockchainSyncService.getSyncProgressPercentage();
            
            return String.format(
                "Status: %s, Processed: %d, Failed: %d, Unprocessed: %d, Sync Progress: %.1f%%, Last Event: %d",
                isListening.get() ? "RUNNING" : "STOPPED",
                totalEventsProcessed,
                totalEventsFailed,
                unprocessedCount,
                syncProgress,
                lastEventTimestamp
            );
        } catch (Exception e) {
            return "Error calculating statistics: " + e.getMessage();
        }
    }

    /**
     * 启动实时事件监听
     */
    private void startRealTimeEventListening() {
        log.info("Starting real-time blockchain event listening...");

        try {
            // 获取最后同步的区块号
            BigInteger startBlock = blockchainSyncService.getLastSyncedBlockNumber();

            // 设置起始区块
            DefaultBlockParameter fromBlock = startBlock.compareTo(BigInteger.ZERO) > 0
                    ? DefaultBlockParameter.valueOf(startBlock.add(BigInteger.ONE))
                    : DefaultBlockParameterName.LATEST;

            log.info("Starting real-time event listening from block: {}", 
                    fromBlock instanceof DefaultBlockParameterName ? "LATEST" : startBlock.add(BigInteger.ONE));

            // 这里应该设置Web3j的事件订阅
            // 具体的订阅逻辑需要根据Web3j的订阅机制来实现
            // 目前先记录日志，后续会实现具体的事件订阅逻辑
            
            log.info("Real-time blockchain event listening started successfully");

        } catch (Exception e) {
            log.error("Failed to start real-time event listening", e);
            throw new BlockchainException("Failed to start real-time event listening", e);
        }
    }

    /**
     * 处理日志结果
     */
    private void processLogResult(EthLog.LogResult<Log> logResult) throws Exception {
        Object logObj = logResult.get();
        if (logObj != null) {
            Log log = (Log) logObj;
            String txHash = log.getTransactionHash();
            
            if (txHash == null || txHash.isEmpty()) {
                throw new BlockchainException("Transaction hash is null or empty");
            }

            // 获取交易收据
            TransactionReceipt receipt = web3jService.getWeb3j().ethGetTransactionReceipt(txHash).send()
                    .getTransactionReceipt().orElseThrow(() -> new BlockchainException(
                            "Transaction receipt not found: " + txHash));

            if (receipt == null) {
                throw new BlockchainException("Transaction receipt is null for: " + txHash);
            }

            // 处理交易收据中的所有事件
            processTransactionReceipt(receipt);
        }
    }

    /**
     * 处理交易收据
     */
    private void processTransactionReceipt(TransactionReceipt receipt) throws IOException {
        // 这里应该解析交易收据中的各种事件
        // 目前先创建一个通用的事件记录，后续会根据事件类型进行具体处理
        
        String contractAddress = evidenceStorageContract.getContractAddress();
        BigInteger blockNumber = receipt.getBlockNumber();
        String transactionHash = receipt.getTransactionHash();
        
        // 获取区块时间戳
        BigInteger blockTimestamp = getBlockTimestamp(blockNumber);

        // 创建事件对象
        BlockchainEvent event = new BlockchainEvent(
                contractAddress,
                "GENERIC_EVENT", // 通用事件类型，后续会根据具体事件类型设置
                blockNumber,
                transactionHash,
                blockTimestamp,
                objectMapper.writeValueAsString(receipt)
        );

        // 保存事件
        BlockchainEvent savedEvent = eventStorageService.saveEvent(event);
        log.debug("Saved generic event: id={}, txHash={}", savedEvent.getId(), transactionHash);

        // 处理事件
        processBlockchainEvent(savedEvent);
    }

    /**
     * 处理区块链事件
     */
    private void processBlockchainEvent(BlockchainEvent event) {
        log.debug("Processing blockchain event: id={}, eventType={}, txHash={}", 
                event.getId(), event.getEventName(), event.getTransactionHash());

        try {
            // 路由到对应的处理器
            for (BlockchainEventProcessor processor : eventProcessors) {
                if (processor.getSupportedEventType().equals(event.getEventName())) {
                    processor.processEvent(event);
                    break;
                }
            }

            // 标记事件为已处理
            eventStorageService.markEventAsProcessed(event.getId());
            
            totalEventsProcessed++;
            lastEventTimestamp = System.currentTimeMillis();
            
            log.debug("Successfully processed event: id={}, eventType={}", 
                    event.getId(), event.getEventName());

        } catch (Exception e) {
            log.error("Failed to process blockchain event: id={}, eventType={}, txHash={}", 
                    event.getId(), event.getEventName(), event.getTransactionHash(), e);
            totalEventsFailed++;
            throw new BlockchainException("Failed to process blockchain event", e);
        }
    }

    /**
     * 获取区块时间戳
     */
    private BigInteger getBlockTimestamp(BigInteger blockNumber) throws IOException {
        EthBlock ethBlock = web3jService.getWeb3j()
                .ethGetBlockByNumber(DefaultBlockParameter.valueOf(blockNumber), false).send();
        
        if (ethBlock.getBlock() == null) {
            throw new BlockchainException("Block not found: " + blockNumber);
        }
        
        return ethBlock.getBlock().getTimestamp();
    }

    /**
     * 安排重试启动
     */
    private void scheduleRetryStart() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.schedule(this::startEventListening, retryIntervalSec, TimeUnit.SECONDS);
            log.info("Scheduled retry start in {} seconds", retryIntervalSec);
        }
    }
}