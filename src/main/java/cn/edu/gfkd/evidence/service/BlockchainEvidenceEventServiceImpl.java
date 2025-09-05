package cn.edu.gfkd.evidence.service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;
import cn.edu.gfkd.evidence.entity.SyncStatus;
import cn.edu.gfkd.evidence.enums.EvidenceEventType;
import cn.edu.gfkd.evidence.exception.BlockchainException;
import cn.edu.gfkd.evidence.generated.EvidenceStorageContract;
import cn.edu.gfkd.evidence.repository.SyncStatusRepository;
import cn.edu.gfkd.evidence.service.processor.BlockchainEventProcessor;
import cn.edu.gfkd.evidence.service.retry.RetryHandler;
import cn.edu.gfkd.evidence.service.storage.EventStorageService;
import cn.edu.gfkd.evidence.service.web3.Web3jService;
import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;

/**
 * 区块链事件统一服务实现
 * 
 * 主要职责： 1. 统一管理区块链事件的实时监听和历史同步 2. 提供同步状态管理功能 3. 处理事件处理器的注册和管理
 */
@Service @Slf4j
public class BlockchainEvidenceEventServiceImpl implements BlockchainEvidenceEventService {

    private final Web3jService web3jService;
    private final EventStorageService eventStorageService;
    private final RetryHandler retryHandler;
    private final EvidenceStorageContract evidenceStorageContract;
    private final SyncStatusRepository syncStatusRepository;
    private final ObjectMapper objectMapper;

    // 订阅管理
    private Disposable evidenceSubmittedSubscription;
    private Disposable evidenceStatusChangedSubscription;
    private Disposable evidenceRevokedSubscription;

    // 事件处理器列表
    private final List<BlockchainEventProcessor> eventProcessors = new CopyOnWriteArrayList<>();

    // 监听状态控制
    private final AtomicBoolean isListening = new AtomicBoolean(false);

    // 同步配置
    @Value("${blockchain.sync.batch-size:100}")
    private int batchSize;

    @Value("${blockchain.sync.delay-between-batches-ms:50}")
    private long delayBetweenBatchesMs;

    // 重试配置
    @Value("${blockchain.sync.max-retry-attempts:3}")
    private int maxRetryAttempts;

    @Value("${blockchain.sync.retry-delay-ms:1000}")
    private long retryDelayMs;

    public BlockchainEvidenceEventServiceImpl(Web3jService web3jService,
            EventStorageService eventStorageService, RetryHandler retryHandler,
            EvidenceStorageContract evidenceStorageContract,
            SyncStatusRepository syncStatusRepository, ObjectMapper objectMapper) {
        this.web3jService = web3jService;
        this.eventStorageService = eventStorageService;
        this.retryHandler = retryHandler;
        this.evidenceStorageContract = evidenceStorageContract;
        this.syncStatusRepository = syncStatusRepository;
        this.objectMapper = objectMapper;
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
            if (needsHistoricalSync()) {
                BigInteger currentBlock = getCurrentBlockNumber();
                BigInteger lastSyncedBlock = getLastSyncedBlockNumber();
                log.info("Syncing missing events from block {} to {}",
                        lastSyncedBlock.add(BigInteger.ONE), currentBlock);
                syncHistoricalEvents(lastSyncedBlock.add(BigInteger.ONE), currentBlock);
            }

            // 启动实时事件监听
            startRealTimeEventListening();

            log.info("Blockchain event listener started successfully");

        } catch (Exception e) {
            log.error("Failed to start blockchain event listener", e);
            isListening.set(false);
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

            // 取消所有的事件订阅
            disposeSubscriptions();

            log.info("Blockchain event listener stopped successfully");

        } catch (Exception e) {
            log.error("Failed to stop blockchain event listener", e);
            throw new BlockchainException("Failed to stop blockchain event listener", e);
        }
    }

    @Override
    public boolean isListening() {
        return isListening.get();
    }

    @Override
    public void syncHistoricalEvents(BigInteger startBlock, BigInteger endBlock) {
        log.info(
                "Starting historical events sync from block {} to {} (batch size: {}, delay: {}ms)",
                startBlock, endBlock, batchSize, delayBetweenBatchesMs);

        // 验证区块范围
        if (startBlock.compareTo(endBlock) > 0) {
            log.warn("Start block {} is greater than end block {}, nothing to sync", startBlock,
                    endBlock);
            return;
        }

        BigInteger totalBlocks = endBlock.subtract(startBlock).add(BigInteger.ONE);
        log.info("Total blocks to process: {}", totalBlocks);

        try {
            // 分批次同步
            syncInBatches(startBlock, endBlock);

            // 最终更新同步状态到结束区块
            updateSyncStatus(endBlock);

            log.info("Successfully completed historical events sync from block {} to {}",
                    startBlock, endBlock);

        } catch (Exception e) {
            log.error("Failed to sync historical events from block {} to {}", startBlock, endBlock,
                    e);
            throw new BlockchainException("Failed to sync historical events", e);
        }
    }

    /**
     * 分批次同步历史事件
     *
     * @param startBlock 起始区块
     * @param endBlock 结束区块
     */
    private void syncInBatches(BigInteger startBlock, BigInteger endBlock) {
        BigInteger current = startBlock;
        BigInteger batchSizeBig = BigInteger.valueOf(batchSize);
        int batchNumber = 1;
        int totalBatches = (int) Math.ceil(
                (double) endBlock.subtract(startBlock).add(BigInteger.ONE).intValue() / batchSize);

        while (current.compareTo(endBlock) <= 0) {
            BigInteger batchEnd = current.add(batchSizeBig).subtract(BigInteger.ONE);
            if (batchEnd.compareTo(endBlock) > 0) {
                batchEnd = endBlock;
            }

            try {
                log.info("Processing batch {}/{}: blocks {} to {}", batchNumber, totalBatches,
                        current, batchEnd);

                // 处理当前批次
                processBatch(current, batchEnd);

                // 更新进度到当前批次的结束区块
                updateSyncStatus(batchEnd);

                log.info("Completed batch {}/{}: blocks {} to {}", batchNumber, totalBatches,
                        current, batchEnd);

                // 准备下一批次
                current = batchEnd.add(BigInteger.ONE);
                batchNumber++;

                // 添加延迟避免过度消耗节点资源
                if (current.compareTo(endBlock) <= 0) {
                    try {
                        Thread.sleep(delayBetweenBatchesMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Batch processing interrupted");
                        break;
                    }
                }

            } catch (Exception e) {
                log.error("Failed to process batch {}/{}: blocks {} to {}", batchNumber,
                        totalBatches, current, batchEnd, e);
                // 继续下一批次，不要因为一个批次失败而停止整个同步过程
                current = batchEnd.add(BigInteger.ONE);
                batchNumber++;
            }
        }
    }

    /**
     * 处理单个批次的事件（带重试机制）
     *
     * @param startBlock 起始区块
     * @param endBlock 结束区块
     */
    private void processBatch(BigInteger startBlock, BigInteger endBlock) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt <= maxRetryAttempts) {
            try {
                if (attempt > 0) {
                    log.info("Retrying batch processing for blocks {} to {} (attempt {}/{})",
                            startBlock, endBlock, attempt, maxRetryAttempts);

                    // 重试前等待
                    if (attempt < maxRetryAttempts) {
                        try {
                            Thread.sleep(retryDelayMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("Retry delay interrupted for batch {} to {}", startBlock,
                                    endBlock);
                            return;
                        }
                    }
                }

                // 创建过滤器
                EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(startBlock),
                        DefaultBlockParameter.valueOf(endBlock),
                        evidenceStorageContract.getContractAddress());

                // 获取事件日志
                EthLog ethLogs = web3jService.getWeb3j().ethGetLogs(filter).send();
                @SuppressWarnings("unchecked")
                List<EthLog.LogResult<Log>> logResults = (List<EthLog.LogResult<Log>>) (List<?>) ethLogs
                        .getLogs();

                if (logResults.isEmpty()) {
                    log.debug("No events found in blocks {} to {}", startBlock, endBlock);
                    return;
                }

                log.info("Found {} events in blocks {} to {}", logResults.size(), startBlock,
                        endBlock);

                // 处理每个事件
                int processedCount = 0;
                int failedCount = 0;

                for (EthLog.LogResult<Log> logResult : logResults) {
                    try {
                        processLogResult(logResult.get());
                        processedCount++;
                    } catch (Exception e) {
                        log.error("Error processing log result in blocks {} to {}: {}", startBlock,
                                endBlock, e.getMessage());
                        failedCount++;
                        // 继续处理下一个事件，不要因为单个事件失败而停止整个批次
                    }
                }

                // 成功完成批次处理
                if (failedCount > 0) {
                    log.warn(
                            "Batch processing completed for blocks {} to {}: {} processed, {} failed",
                            startBlock, endBlock, processedCount, failedCount);
                } else {
                    log.info(
                            "Batch processing completed for blocks {} to {}: {} events processed successfully",
                            startBlock, endBlock, processedCount);
                }

                return; // 成功完成，退出重试循环

            } catch (Exception e) {
                lastException = e;
                attempt++;

                if (attempt <= maxRetryAttempts) {
                    log.warn("Attempt {} failed for batch {} to {}: {}", attempt, startBlock,
                            endBlock, e.getMessage());
                }
            }
        }

        // 所有重试都失败了
        log.error("Failed to process batch {} to {} after {} attempts. Last error: {}", startBlock,
                endBlock, maxRetryAttempts,
                lastException != null ? lastException.getMessage() : "unknown");

        if (lastException != null) {
            throw new BlockchainException(
                    "Failed to process batch after " + maxRetryAttempts + " attempts",
                    lastException);
        } else {
            throw new BlockchainException(
                    "Failed to process batch after " + maxRetryAttempts + " attempts");
        }
    }

    @Override @Transactional
    public void updateSyncStatus(BigInteger blockNumber) {
        log.debug("Updating sync status to block {}", blockNumber);

        retryHandler.executeWithRetryTransactional(() -> {
            SyncStatus syncStatus = getOrCreateSyncStatus();
            syncStatus.setLastBlockNumber(blockNumber);
            syncStatus.setLastSyncTimestamp(java.time.LocalDateTime.now());
            syncStatus.setSyncStatus("SYNCED");

            SyncStatus savedStatus = syncStatusRepository.save(syncStatus);
            log.info("Updated sync status: contract={}, blockNumber={}, syncStatus={}",
                    savedStatus.getContractAddress(), savedStatus.getLastBlockNumber(),
                    savedStatus.getSyncStatus());

            return null;
        }, "update sync status");
    }

    @Override
    public BigInteger getLastSyncedBlockNumber() {
        log.debug("Getting last synced block number");

        return retryHandler.executeWithRetry(() -> {
            SyncStatus syncStatus = getOrCreateSyncStatus();
            return syncStatus.getLastBlockNumber();
        }, "get last synced block number");
    }

    @Override
    public BigInteger getCurrentBlockNumber() {
        log.debug("Getting current block number");

        return retryHandler.executeWithRetry(() -> {
            try {
                org.web3j.protocol.core.methods.response.EthBlockNumber blockNumber = web3jService
                        .getWeb3j().ethBlockNumber().send();

                if (blockNumber == null) {
                    throw new BlockchainException("Block number response is null");
                }

                return blockNumber.getBlockNumber();
            } catch (IOException e) {
                throw new BlockchainException("Failed to get current block number", e);
            }
        }, "get current block number");
    }

    @Override
    public boolean needsHistoricalSync() {
        try {
            BigInteger currentBlock = getCurrentBlockNumber();
            BigInteger lastSyncedBlock = getLastSyncedBlockNumber();
            BigInteger blocksBehind = currentBlock.subtract(lastSyncedBlock);
            return blocksBehind.compareTo(BigInteger.TEN) > 0;
        } catch (Exception e) {
            log.warn("Failed to determine if historical sync is needed, assuming yes", e);
            return true;
        }
    }

    @Override
    public void setEventProcessor(BlockchainEventProcessor eventProcessor) {
        if (eventProcessor != null && !eventProcessors.contains(eventProcessor)) {
            eventProcessors.add(eventProcessor);
            log.info("Added event processor: {}", eventProcessor.getSupportedEventType());
        }
    }

    /**
     * 启动实时事件监听
     */
    private void startRealTimeEventListening() {
        log.info("Starting real-time blockchain event listening...");

        try {
            // 获取最后同步的区块号
            BigInteger startBlock = getLastSyncedBlockNumber();

            // 从最后同步区块的下一个区块开始监听，确保连续性
            DefaultBlockParameter fromBlock = startBlock.compareTo(BigInteger.ZERO) >= 0
                    ? DefaultBlockParameter.valueOf(startBlock.add(BigInteger.ONE))
                    : DefaultBlockParameterName.LATEST;

            log.info("Starting real-time event listening from block: {}",
                    fromBlock instanceof DefaultBlockParameterName ? "LATEST"
                            : startBlock.add(BigInteger.ONE));

            // 启动 EvidenceSubmitted 事件订阅
            evidenceSubmittedSubscription = evidenceStorageContract
                    .evidenceSubmittedEventFlowable(fromBlock, DefaultBlockParameterName.LATEST)
                    .subscribe(event -> {
                        try {
                            processRealTimeEvidenceSubmitted(event);
                        } catch (Exception e) {
                            log.error("Error processing real-time EvidenceSubmitted event", e);
                        }
                    }, error -> {
                        log.error("Error in EvidenceSubmitted event subscription", error);
                        // 订阅错误处理：记录错误并尝试重新订阅
                        handleSubscriptionError("EvidenceSubmitted", error);
                    });

            // 启动 EvidenceStatusChanged 事件订阅
            evidenceStatusChangedSubscription = evidenceStorageContract
                    .evidenceStatusChangedEventFlowable(fromBlock, DefaultBlockParameterName.LATEST)
                    .subscribe(event -> {
                        try {
                            processRealTimeEvidenceStatusChanged(event);
                        } catch (Exception e) {
                            log.error("Error processing real-time EvidenceStatusChanged event", e);
                        }
                    }, error -> {
                        log.error("Error in EvidenceStatusChanged event subscription", error);
                        // 订阅错误处理：记录错误并尝试重新订阅
                        handleSubscriptionError("EvidenceStatusChanged", error);
                    });

            // 启动 EvidenceRevoked 事件订阅
            evidenceRevokedSubscription = evidenceStorageContract
                    .evidenceRevokedEventFlowable(fromBlock, DefaultBlockParameterName.LATEST)
                    .subscribe(event -> {
                        try {
                            processRealTimeEvidenceRevoked(event);
                        } catch (Exception e) {
                            log.error("Error processing real-time EvidenceRevoked event", e);
                        }
                    }, error -> {
                        log.error("Error in EvidenceRevoked event subscription", error);
                        // 订阅错误处理：记录错误并尝试重新订阅
                        handleSubscriptionError("EvidenceRevoked", error);
                    });

            log.info("Real-time blockchain event listening started successfully");

        } catch (Exception e) {
            log.error("Failed to start real-time event listening", e);
            throw new BlockchainException("Failed to start real-time event listening", e);
        }
    }

    /**
     * 处理日志结果
     */
    private void processLogResult(Log logResult) throws Exception {
        if (logResult != null) {
            String txHash = logResult.getTransactionHash();

            if (txHash == null || txHash.isEmpty()) {
                throw new BlockchainException("Transaction hash is null or empty");
            }

            // 获取交易收据
            TransactionReceipt receipt = web3jService.getWeb3j().ethGetTransactionReceipt(txHash)
                    .send().getTransactionReceipt().orElseThrow(() -> new BlockchainException(
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
        String contractAddress = evidenceStorageContract.getContractAddress();
        BigInteger blockNumber = receipt.getBlockNumber();
        String transactionHash = receipt.getTransactionHash();

        // 获取区块时间戳
        BigInteger blockTimestamp = getBlockTimestamp(blockNumber);

        log.debug("Processing transaction receipt for: {}", transactionHash);

        // 处理 EvidenceSubmitted 事件
        try {
            List<EvidenceStorageContract.EvidenceSubmittedEventResponse> submittedEvents = EvidenceStorageContract
                    .getEvidenceSubmittedEvents(receipt);

            if (!submittedEvents.isEmpty()) {
                log.debug("Found {} EvidenceSubmitted events in receipt", submittedEvents.size());
                for (EvidenceStorageContract.EvidenceSubmittedEventResponse event : submittedEvents) {
                    processEvidenceSubmitted(event, blockNumber, transactionHash, blockTimestamp);
                }
            }
        } catch (Exception e) {
            log.debug("No EvidenceSubmitted events found in receipt: {}", e.getMessage());
        }

        // 处理 EvidenceStatusChanged 事件
        try {
            List<EvidenceStorageContract.EvidenceStatusChangedEventResponse> statusChangedEvents = EvidenceStorageContract
                    .getEvidenceStatusChangedEvents(receipt);

            if (!statusChangedEvents.isEmpty()) {
                log.debug("Found {} EvidenceStatusChanged events in receipt",
                        statusChangedEvents.size());
                for (EvidenceStorageContract.EvidenceStatusChangedEventResponse event : statusChangedEvents) {
                    processEvidenceStatusChanged(event, blockNumber, transactionHash,
                            blockTimestamp);
                }
            }
        } catch (Exception e) {
            log.debug("No EvidenceStatusChanged events found in receipt: {}", e.getMessage());
        }

        // 处理 EvidenceRevoked 事件
        try {
            List<EvidenceStorageContract.EvidenceRevokedEventResponse> revokedEvents = EvidenceStorageContract
                    .getEvidenceRevokedEvents(receipt);

            if (!revokedEvents.isEmpty()) {
                log.debug("Found {} EvidenceRevoked events in receipt", revokedEvents.size());
                for (EvidenceStorageContract.EvidenceRevokedEventResponse event : revokedEvents) {
                    processEvidenceRevoked(event, blockNumber, transactionHash, blockTimestamp);
                }
            }
        } catch (Exception e) {
            log.debug("No EvidenceRevoked events found in receipt: {}", e.getMessage());
        }
    }

    /**
     * 处理区块链事件
     */
    private void processBlockchainEvent(BlockchainEvent event) {
        log.debug("Processing blockchain event: id={}, eventType={}, txHash={}", event.getId(),
                event.getEventName(), event.getTransactionHash());

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

            log.debug("Successfully processed event: id={}, eventType={}", event.getId(),
                    event.getEventName());

        } catch (Exception e) {
            log.error("Failed to process blockchain event: id={}, eventType={}, txHash={}",
                    event.getId(), event.getEventName(), event.getTransactionHash(), e);

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
     * 获取或创建同步状态
     */
    private SyncStatus getOrCreateSyncStatus() {
        String contractAddress = evidenceStorageContract.getContractAddress();
        return syncStatusRepository.findById(contractAddress).orElseGet(() -> {
            SyncStatus newStatus = new SyncStatus(contractAddress, BigInteger.ZERO);
            return syncStatusRepository.save(newStatus);
        });
    }

    /**
     * 处理 EvidenceSubmitted 事件
     */
    private void processEvidenceSubmitted(
            EvidenceStorageContract.EvidenceSubmittedEventResponse event, BigInteger blockNumber,
            String transactionHash, BigInteger blockTimestamp) {
        try {
            log.debug("Processing EvidenceSubmitted event: evidenceId={}, submitter={}",
                    event.evidenceId, event.user);

            // 创建事件对象
            BlockchainEvent blockchainEvent = new BlockchainEvent(
                    evidenceStorageContract.getContractAddress(),
                    EvidenceEventType.EVIDENCE_SUBMITTED.name(), blockNumber, transactionHash,
                    blockTimestamp, objectMapper.writeValueAsString(event));

            // 保存事件
            BlockchainEvent savedEvent = eventStorageService.saveEvent(blockchainEvent);
            log.debug("Saved EvidenceSubmitted event: id={}, evidenceId={}", savedEvent.getId(),
                    event.evidenceId);

            // 处理事件
            processBlockchainEvent(savedEvent);

        } catch (Exception e) {
            log.error("Failed to process EvidenceSubmitted event: evidenceId={}", event.evidenceId,
                    e);
            throw new BlockchainException("Failed to process EvidenceSubmitted event", e);
        }
    }

    /**
     * 处理 EvidenceStatusChanged 事件
     */
    private void processEvidenceStatusChanged(
            EvidenceStorageContract.EvidenceStatusChangedEventResponse event,
            BigInteger blockNumber, String transactionHash, BigInteger blockTimestamp) {
        try {
            log.debug(
                    "Processing EvidenceStatusChanged event: evidenceId={}, oldStatus={}, newStatus={}",
                    event.evidenceId, event.oldStatus, event.newStatus);

            // 创建事件对象
            BlockchainEvent blockchainEvent = new BlockchainEvent(
                    evidenceStorageContract.getContractAddress(),
                    EvidenceEventType.EVIDENCE_STATUS_CHANGED.name(), blockNumber, transactionHash,
                    blockTimestamp, objectMapper.writeValueAsString(event));

            // 保存事件
            BlockchainEvent savedEvent = eventStorageService.saveEvent(blockchainEvent);
            log.debug("Saved EvidenceStatusChanged event: id={}, evidenceId={}", savedEvent.getId(),
                    event.evidenceId);

            // 处理事件
            processBlockchainEvent(savedEvent);

        } catch (Exception e) {
            log.error("Failed to process EvidenceStatusChanged event: evidenceId={}",
                    event.evidenceId, e);
            throw new BlockchainException("Failed to process EvidenceStatusChanged event", e);
        }
    }

    /**
     * 处理 EvidenceRevoked 事件
     */
    private void processEvidenceRevoked(EvidenceStorageContract.EvidenceRevokedEventResponse event,
            BigInteger blockNumber, String transactionHash, BigInteger blockTimestamp) {
        try {
            log.debug("Processing EvidenceRevoked event: evidenceId={}, revoker={}",
                    event.evidenceId, event.revoker);

            // 创建事件对象
            BlockchainEvent blockchainEvent = new BlockchainEvent(
                    evidenceStorageContract.getContractAddress(),
                    EvidenceEventType.EVIDENCE_REVOKED.name(), blockNumber, transactionHash,
                    blockTimestamp, objectMapper.writeValueAsString(event));

            // 保存事件
            BlockchainEvent savedEvent = eventStorageService.saveEvent(blockchainEvent);
            log.debug("Saved EvidenceRevoked event: id={}, evidenceId={}", savedEvent.getId(),
                    event.evidenceId);

            // 处理事件
            processBlockchainEvent(savedEvent);

        } catch (Exception e) {
            log.error("Failed to process EvidenceRevoked event: evidenceId={}", event.evidenceId,
                    e);
            throw new BlockchainException("Failed to process EvidenceRevoked event", e);
        }
    }

    /**
     * 处理实时 EvidenceSubmitted 事件
     */
    private void processRealTimeEvidenceSubmitted(
            EvidenceStorageContract.EvidenceSubmittedEventResponse event) throws Exception {
        log.debug(
                "Processing real-time EvidenceSubmitted event: evidenceId={}, submitter={}, timestamp={}",
                event.evidenceId, event.user, event.timestamp);
        processLogResult(event.log);
    }

    /**
     * 处理实时 EvidenceStatusChanged 事件
     */
    private void processRealTimeEvidenceStatusChanged(
            EvidenceStorageContract.EvidenceStatusChangedEventResponse event) throws Exception {
        log.debug(
                "Processing real-time EvidenceStatusChanged event: evidenceId={}, oldStatus={}, newStatus={}",
                event.evidenceId, event.oldStatus, event.newStatus);
        processLogResult(event.log);
    }

    /**
     * 处理实时 EvidenceRevoked 事件
     */
    private void processRealTimeEvidenceRevoked(
            EvidenceStorageContract.EvidenceRevokedEventResponse event) throws Exception {
        log.debug(
                "Processing real-time EvidenceRevoked event: evidenceId={}, revoker={}, reason={}",
                event.evidenceId, event.revoker, event.timestamp);

        processLogResult(event.log);
    }

    /**
     * 取消所有的事件订阅
     */
    private void disposeSubscriptions() {
        log.info("Disposing all event subscriptions...");

        try {
            if (evidenceSubmittedSubscription != null
                    && !evidenceSubmittedSubscription.isDisposed()) {
                evidenceSubmittedSubscription.dispose();
                log.debug("Disposed EvidenceSubmitted subscription");
            }

            if (evidenceStatusChangedSubscription != null
                    && !evidenceStatusChangedSubscription.isDisposed()) {
                evidenceStatusChangedSubscription.dispose();
                log.debug("Disposed EvidenceStatusChanged subscription");
            }

            if (evidenceRevokedSubscription != null && !evidenceRevokedSubscription.isDisposed()) {
                evidenceRevokedSubscription.dispose();
                log.debug("Disposed EvidenceRevoked subscription");
            }

            // 清除引用
            evidenceSubmittedSubscription = null;
            evidenceStatusChangedSubscription = null;
            evidenceRevokedSubscription = null;

            log.info("All event subscriptions disposed successfully");

        } catch (Exception e) {
            log.error("Error while disposing subscriptions", e);
        }
    }

    /**
     * 处理订阅错误
     */
    private void handleSubscriptionError(String eventType, Throwable error) {
        log.error("Subscription error for {} event: {}", eventType, error.getMessage());

        // 如果系统仍在运行，尝试重新订阅
        if (isListening.get()) {
            log.warn("Attempting to restart {} subscription", eventType);
            try {
                // 延迟重连，避免立即重试导致的问题
                Thread.sleep(5000);
                restartSubscription(eventType);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Subscription restart interrupted for {}", eventType);
            } catch (Exception e) {
                log.error("Failed to restart {} subscription", eventType, e);
            }
        }
    }

    /**
     * 重新启动指定类型的订阅
     */
    private void restartSubscription(String eventType) {
        if (!isListening.get()) {
            log.warn("System is not listening, skipping subscription restart for {}", eventType);
            return;
        }

        try {
            BigInteger startBlock = getLastSyncedBlockNumber().add(BigInteger.ONE);
            DefaultBlockParameter fromBlock = DefaultBlockParameter.valueOf(startBlock);

            switch (eventType) {
                case "EvidenceSubmitted":
                    if (evidenceSubmittedSubscription != null) {
                        evidenceSubmittedSubscription.dispose();
                    }
                    evidenceSubmittedSubscription = evidenceStorageContract
                            .evidenceSubmittedEventFlowable(fromBlock,
                                    DefaultBlockParameterName.LATEST)
                            .subscribe(event -> {
                                try {
                                    processRealTimeEvidenceSubmitted(event);
                                } catch (Exception e) {
                                    log.error("Error processing real-time EvidenceSubmitted event",
                                            e);
                                }
                            }, error -> {
                                log.error("Error in EvidenceSubmitted event subscription", error);
                                handleSubscriptionError("EvidenceSubmitted", error);
                            });
                    break;

                case "EvidenceStatusChanged":
                    if (evidenceStatusChangedSubscription != null) {
                        evidenceStatusChangedSubscription.dispose();
                    }
                    evidenceStatusChangedSubscription = evidenceStorageContract
                            .evidenceStatusChangedEventFlowable(fromBlock,
                                    DefaultBlockParameterName.LATEST)
                            .subscribe(event -> {
                                try {
                                    processRealTimeEvidenceStatusChanged(event);
                                } catch (Exception e) {
                                    log.error(
                                            "Error processing real-time EvidenceStatusChanged event",
                                            e);
                                }
                            }, error -> {
                                log.error("Error in EvidenceStatusChanged event subscription",
                                        error);
                                handleSubscriptionError("EvidenceStatusChanged", error);
                            });
                    break;

                case "EvidenceRevoked":
                    if (evidenceRevokedSubscription != null) {
                        evidenceRevokedSubscription.dispose();
                    }
                    evidenceRevokedSubscription = evidenceStorageContract
                            .evidenceRevokedEventFlowable(fromBlock,
                                    DefaultBlockParameterName.LATEST)
                            .subscribe(event -> {
                                try {
                                    processRealTimeEvidenceRevoked(event);
                                } catch (Exception e) {
                                    log.error("Error processing real-time EvidenceRevoked event",
                                            e);
                                }
                            }, error -> {
                                log.error("Error in EvidenceRevoked event subscription", error);
                                handleSubscriptionError("EvidenceRevoked", error);
                            });
                    break;

                default:
                    log.warn("Unknown event type for subscription restart: {}", eventType);
            }

            log.info("Successfully restarted {} subscription", eventType);

        } catch (Exception e) {
            log.error("Failed to restart {} subscription", eventType, e);
        }
    }
}
