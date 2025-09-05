package cn.edu.gfkd.evidence.service;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.edu.gfkd.evidence.service.processor.BlockchainEventProcessor;
import cn.edu.gfkd.evidence.service.storage.EventStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;
import cn.edu.gfkd.evidence.exception.BlockchainException;
import lombok.extern.slf4j.Slf4j;

/**
 * 区块链事件协调器服务
 * 
 * 主要职责：
 * 1. 协调整个区块链事件的处理流程
 * 2. 管理事件处理器的注册和路由
 * 3. 提供事件处理的统一入口
 * 4. 处理系统启动和关闭的生命周期
 */
@Service @Slf4j
public class EventOrchestratorService {

    private final BlockchainEvidenceEventService blockchainEvidenceEventService;
    private final EventStorageService eventStorageService;
    private final List<BlockchainEventProcessor> eventProcessors;
    // 系统状态控制
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;

    // 协调器配置
    @Value("${orchestrator.health-check-interval-sec:30}")
    private int healthCheckIntervalSec;

    @Value("${orchestrator.startup-delay-sec:5}")
    private int startupDelaySec;

    @Value("${orchestrator.max-retry-attempts:3}")
    private int maxRetryAttempts;

    private volatile long systemStartTime = 0;

    public EventOrchestratorService(
            BlockchainEvidenceEventService blockchainEvidenceEventService,
            EventStorageService eventStorageService,
            List<BlockchainEventProcessor> eventProcessors) {
        this.blockchainEvidenceEventService = blockchainEvidenceEventService;
        this.eventStorageService = eventStorageService;
        this.eventProcessors = eventProcessors;
    }

    /**
     * 初始化事件协调器
     */
    public void init() {
        log.info("Initializing event orchestrator service");

        if (isRunning.get()) {
            log.warn("Event orchestrator is already initialized");
            return;
        }

        try {
            systemStartTime = System.currentTimeMillis();
            scheduler = Executors.newSingleThreadScheduledExecutor();

            // 注册所有事件处理器到监听服务
            registerEventProcessors();

            // 初始化各个处理器
            initializeEventProcessors();

            // 延迟启动整个系统
            scheduler.schedule(this::startSystem, startupDelaySec, TimeUnit.SECONDS);

            // 启动健康检查
            scheduler.scheduleAtFixedRate(this::performHealthCheck, 
                    healthCheckIntervalSec, healthCheckIntervalSec, TimeUnit.SECONDS);

            log.info("Event orchestrator service initialized successfully");

        } catch (Exception e) {
            log.error("Failed to initialize event orchestrator service", e);
            throw new BlockchainException("Failed to initialize event orchestrator service", e);
        }
    }

    /**
     * 启动整个事件处理系统
     */
    public void startSystem() {
        log.info("Starting blockchain event processing system");

        if (isRunning.get()) {
            log.warn("System is already running");
            return;
        }

        try {
            isRunning.set(true);

            // 启动合约监听服务
            blockchainEvidenceEventService.startEventListening();

            // 历史事件同步现在在 blockchainEventService.startEventListening() 中自动处理

            log.info("Blockchain event processing system started successfully");

        } catch (Exception e) {
            log.error("Failed to start blockchain event processing system", e);
            isRunning.set(false);
            throw new BlockchainException("Failed to start blockchain event processing system", e);
        }
    }

    /**
     * 停止整个事件处理系统
     */
    public void stopSystem() {
        log.info("Stopping blockchain event processing system");

        if (!isRunning.get()) {
            log.warn("System is not running");
            return;
        }

        try {
            isRunning.set(false);

            // 停止合约监听服务
            blockchainEvidenceEventService.stopEventListening();

            // 销毁事件处理器
            destroyEventProcessors();

            log.info("Blockchain event processing system stopped successfully");

        } catch (Exception e) {
            log.error("Failed to stop blockchain event processing system", e);
            throw new BlockchainException("Failed to stop blockchain event processing system", e);
        }
    }

    /**
     * 重启整个事件处理系统
     */
    public void restartSystem() {
        log.info("Restarting blockchain event processing system");
        
        try {
            stopSystem();
            // 等待系统完全停止
            Thread.sleep(2000);
            startSystem();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("System restart interrupted");
        } catch (Exception e) {
            log.error("Failed to restart blockchain event processing system", e);
            throw new BlockchainException("Failed to restart blockchain event processing system", e);
        }
    }

    /**
     * 处理区块链事件（统一入口）
     */
    @Transactional
    public void processBlockchainEvent(BlockchainEvent event) {
        log.debug("Processing blockchain event through orchestrator: id={}, eventType={}, txHash={}", 
                event.getId(), event.getEventName(), event.getTransactionHash());

        try {
            // 首先保存事件到数据库（如果还没有保存）
            BlockchainEvent savedEvent = ensureEventSaved(event);

            // 路由到对应的事件处理器
            BlockchainEventProcessor processor = findEventProcessor(savedEvent);
            if (processor == null) {
                log.warn("No processor found for event type: {}", savedEvent.getEventName());
                return;
            }

            // 处理事件
            processor.processEvent(savedEvent);
            
            // 标记事件为已处理
            eventStorageService.markEventAsProcessed(savedEvent.getId());
            
            log.debug("Successfully processed event: id={}, eventType={}", 
                    savedEvent.getId(), savedEvent.getEventName());

        } catch (Exception e) {
            log.error("Failed to process blockchain event: id={}, eventType={}, txHash={}", 
                    event.getId(), event.getEventName(), event.getTransactionHash(), e);
            
            // 增加事件处理失败次数
            eventStorageService.incrementEventProcessingFailures(event.getId());
            
            throw new BlockchainException("Failed to process blockchain event", e);
        }
    }

    /**
     * 处理未处理的事件
     */
    public void processUnprocessedEvents() {
        if (!isRunning.get()) {
            log.debug("System not running, skipping unprocessed events processing");
            return;
        }

        try {
            log.debug("Processing unprocessed events through orchestrator");

            // 获取未处理的事件
            var unprocessedEvents = eventStorageService.findUnprocessedEvents(
                org.springframework.data.domain.PageRequest.of(0, 100)
            );

            if (unprocessedEvents.isEmpty()) {
                log.debug("No unprocessed events found");
                return;
            }

            log.info("Found {} unprocessed events to process", unprocessedEvents.size());

            // 处理每个未处理的事件
            for (BlockchainEvent event : unprocessedEvents) {
                try {
                    processBlockchainEvent(event);
                } catch (Exception e) {
                    log.error("Failed to process unprocessed event: id={}, txHash={}, eventType={}", 
                            event.getId(), event.getTransactionHash(), event.getEventName(), e);
                    // 继续处理下一个事件
                }
            }

            log.debug("Completed processing unprocessed events");

        } catch (Exception e) {
            log.error("Error processing unprocessed events", e);
        }
    }

    /**
     * 获取系统状态信息
     */
    public String getSystemStatus() {
        try {
            if (!isRunning.get()) {
                return "STOPPED";
            }

            StringBuilder status = new StringBuilder();
            status.append("RUNNING\n");
            status.append("System uptime: ").append(getSystemUptime()).append("ms\n");
            status.append("Event service: ").append(blockchainEvidenceEventService.isListening() ? "RUNNING" : "STOPPED").append("\n");
            try {
                BigInteger currentBlock = blockchainEvidenceEventService.getCurrentBlockNumber();
                BigInteger lastSyncedBlock = blockchainEvidenceEventService.getLastSyncedBlockNumber();
                BigInteger blocksBehind = currentBlock.subtract(lastSyncedBlock);
                status.append("Blockchain sync: Last block ").append(lastSyncedBlock)
                      .append(", Current block ").append(currentBlock)
                      .append(", Behind ").append(blocksBehind).append(" blocks\n");
            } catch (Exception e) {
                status.append("Blockchain sync: Status unavailable\n");
            }
            status.append("Active processors: ").append(eventProcessors.size()).append("\n");


            return status.toString();

        } catch (Exception e) {
            return "ERROR - " + e.getMessage();
        }
    }

    /**
     * 检查系统是否正在运行
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * 关闭协调器服务
     */
    public void shutdown() {
        log.info("Shutting down event orchestrator service");

        // 停止系统
        stopSystem();

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

        log.info("Event orchestrator service shutdown completed");
    }

    /**
     * 注册事件处理器到监听服务
     */
    private void registerEventProcessors() {
        log.info("Registering {} event processors", eventProcessors.size());
        
        for (BlockchainEventProcessor processor : eventProcessors) {
            blockchainEvidenceEventService.setEventProcessor(processor);
            log.info("Registered processor for event type: {}", processor.getSupportedEventType());
        }
    }

    /**
     * 初始化事件处理器
     */
    private void initializeEventProcessors() {
        log.info("Initializing event processors");
        
        for (BlockchainEventProcessor processor : eventProcessors) {
            try {
                processor.initialize();
                log.info("Initialized processor: {}", processor.getSupportedEventType());
            } catch (Exception e) {
                log.error("Failed to initialize processor: {}", processor.getSupportedEventType(), e);
                throw new BlockchainException("Failed to initialize processor: " + processor.getSupportedEventType(), e);
            }
        }
    }

    /**
     * 销毁事件处理器
     */
    private void destroyEventProcessors() {
        log.info("Destroying event processors");
        
        for (BlockchainEventProcessor processor : eventProcessors) {
            try {
                processor.destroy();
                log.info("Destroyed processor: {}", processor.getSupportedEventType());
            } catch (Exception e) {
                log.error("Failed to destroy processor: {}", processor.getSupportedEventType(), e);
            }
        }
    }

    /**
     * 确保事件已保存到数据库
     */
    private BlockchainEvent ensureEventSaved(BlockchainEvent event) {
        // 如果事件已经有ID，说明已经保存过
        if (event.getId() != null) {
            return event;
        }

        // 保存事件到数据库
        return eventStorageService.saveEvent(event);
    }

    /**
     * 查找适合的事件处理器
     */
    private BlockchainEventProcessor findEventProcessor(BlockchainEvent event) {
        for (BlockchainEventProcessor processor : eventProcessors) {
            if (processor.canProcess(event)) {
                return processor;
            }
        }
        return null;
    }

    /**
     * 执行健康检查
     */
    private void performHealthCheck() {
        if (!isRunning.get()) {
            return;
        }

        try {
            log.debug("Performing system health check");

            // 检查各个组件的健康状态
            boolean allHealthy = true;

            // 检查区块链事件服务
            if (!blockchainEvidenceEventService.isListening()) {
                log.warn("Blockchain evidence event service is not running");
                allHealthy = false;
            }

            // 检查区块链同步状态
            try {
                BigInteger currentBlock = blockchainEvidenceEventService.getCurrentBlockNumber();
                BigInteger lastSyncedBlock = blockchainEvidenceEventService.getLastSyncedBlockNumber();
                BigInteger blocksBehind = currentBlock.subtract(lastSyncedBlock);
                if (blocksBehind.compareTo(BigInteger.valueOf(100)) > 0) {
                    log.warn("Blockchain sync is too far behind: {} blocks", blocksBehind);
                    allHealthy = false;
                }
            } catch (Exception e) {
                log.warn("Failed to check blockchain sync status", e);
                allHealthy = false;
            }

            // 检查事件处理器
            for (BlockchainEventProcessor processor : eventProcessors) {
                if (!processor.isHealthy()) {
                    log.warn("Event processor {} is not healthy", processor.getSupportedEventType());
                    allHealthy = false;
                }
            }

            if (allHealthy) {
                log.debug("System health check passed");
            } else {
                log.warn("System health check failed, some components are unhealthy");
            }

        } catch (Exception e) {
            log.error("Error during health check", e);
        }
    }

    /**
     * 获取系统运行时间
     */
    private long getSystemUptime() {
        return systemStartTime > 0 ? System.currentTimeMillis() - systemStartTime : 0;
    }

}