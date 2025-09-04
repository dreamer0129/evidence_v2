package cn.edu.gfkd.evidence.service;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.web3j.utils.Numeric;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import com.fasterxml.jackson.databind.ObjectMapper;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;
import cn.edu.gfkd.evidence.entity.EvidenceEntity;
import cn.edu.gfkd.evidence.entity.SyncStatus;
import cn.edu.gfkd.evidence.exception.BlockchainException;
import cn.edu.gfkd.evidence.exception.EvidenceNotFoundException;
import cn.edu.gfkd.evidence.generated.EvidenceStorageContract;
import cn.edu.gfkd.evidence.repository.BlockchainEventRepository;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import cn.edu.gfkd.evidence.repository.SyncStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service @RequiredArgsConstructor @Slf4j
public class EvidenceEventListener {

    // SQLite database lock retry configuration
    private static final int MAX_DB_RETRIES = 3;
    private static final long DB_RETRY_DELAY_MS = 100;

    private final Web3j web3j;
    private final BlockchainEventRepository blockchainEventRepository;
    private final EvidenceRepository evidenceRepository;
    private final SyncStatusRepository syncStatusRepository;
    private final ObjectMapper objectMapper;
    private final EvidenceStorageContract evidenceStorageContract;

    // Synchronization object for database operations
    private final Object dbLock = new Object();

    @Value("${blockchain.node.timeout:5000}")
    private long nodeTimeout;

    private ScheduledExecutorService scheduler;

    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        try {
            // Start event listening after a delay
            scheduler.schedule(this::startEventListening, 3, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("Failed to initialize blockchain event listener", e);
            throw new BlockchainException("Failed to initialize blockchain event listener", e);
        }
    }

    /**
     * Helper method to execute database operations with retry logic for SQLite locks
     */
    private <T> T executeWithRetry(DbOperation<T> operation, String operationName) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_DB_RETRIES) {
            try {
                synchronized (dbLock) {
                    return operation.execute();
                }
            } catch (Exception e) {
                lastException = e;
                attempt++;

                if (attempt < MAX_DB_RETRIES) {
                    log.warn("Attempt {} failed for {}: {}. Retrying in {}ms...", attempt,
                            operationName, e.getMessage(), DB_RETRY_DELAY_MS);
                    try {
                        Thread.sleep(DB_RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BlockchainException(
                                "Interrupted during retry for " + operationName, ie);
                    }
                } else {
                    log.error("All {} attempts failed for {}: {}", MAX_DB_RETRIES, operationName,
                            e.getMessage());
                }
            }
        }

        throw new BlockchainException(
                "Failed to execute " + operationName + " after " + MAX_DB_RETRIES + " attempts",
                lastException);
    }

    @FunctionalInterface
    private interface DbOperation<T> {
        T execute() throws Exception;
    }

    private BigInteger getBlockTimestamp(BigInteger blockNumber) throws IOException {
        EthBlock ethBlock = web3j
                .ethGetBlockByNumber(DefaultBlockParameter.valueOf(blockNumber), false).send();
        if (ethBlock.getBlock() == null) {
            throw new BlockchainException("Block not found: " + blockNumber);
        }
        return ethBlock.getBlock().getTimestamp();
    }

    public String getContractAddress() {
        return evidenceStorageContract.getContractAddress();
    }

    @Transactional
    public void syncPastEvents(BigInteger startBlock, BigInteger endBlock) {
        try {
            log.info("Syncing past events from block {} to {}", startBlock, endBlock);

            EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(startBlock),
                    DefaultBlockParameter.valueOf(endBlock),
                    evidenceStorageContract.getContractAddress());

            EthLog ethLogs = web3j.ethGetLogs(filter).send();
            @SuppressWarnings("unchecked")
            List<EthLog.LogResult<Log>> logResults = (List<EthLog.LogResult<Log>>) (List<?>) ethLogs
                    .getLogs();

            for (EthLog.LogResult<Log> logResult : logResults) {
                try {
                    processLogResult(logResult);
                } catch (Exception e) {
                    log.error("Error processing log result", e);
                }
            }

            updateSyncStatus(endBlock);

            log.info("Completed syncing past events from block {} to {}", startBlock, endBlock);

        } catch (Exception e) {
            log.error("Failed to sync past events", e);
            throw new BlockchainException("Failed to sync past events", e);
        }
    }

    private void processLogResult(EthLog.LogResult<Log> logResult) throws Exception {
        Object logObj = logResult.get();
        if (logObj != null) {
            // The actual object returned is of type Log which extends EthLog.Log
            Log log = (Log) logObj;
            String txHash = log.getTransactionHash();
            if (txHash == null || txHash.isEmpty()) {
                throw new BlockchainException("Transaction hash is null or empty");
            }
            TransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash).send()
                    .getTransactionReceipt().orElseThrow(() -> new BlockchainException(
                            "Transaction receipt not found: " + txHash));
            if (receipt == null) {
                throw new BlockchainException("Transaction receipt is null for: " + txHash);
            }

            processAllEventTypes(receipt);
        }
    }

    private void processAllEventTypes(TransactionReceipt receipt) throws IOException {
        // Process EvidenceSubmitted events
        try {
            List<EvidenceStorageContract.EvidenceSubmittedEventResponse> submittedEvents = EvidenceStorageContract
                    .getEvidenceSubmittedEvents(receipt);
            submittedEvents.forEach(this::processEvidenceSubmitted);
        } catch (Exception e) {
            log.debug("No EvidenceSubmitted events found in receipt");
        }

        // Process EvidenceStatusChanged events
        try {
            List<EvidenceStorageContract.EvidenceStatusChangedEventResponse> statusChangedEvents = EvidenceStorageContract
                    .getEvidenceStatusChangedEvents(receipt);
            statusChangedEvents.forEach(this::processEvidenceStatusChanged);
        } catch (Exception e) {
            log.debug("No EvidenceStatusChanged events found in receipt");
        }

        // Process EvidenceVerified events
        try {
            List<EvidenceStorageContract.EvidenceVerifiedEventResponse> verifiedEvents = EvidenceStorageContract
                    .getEvidenceVerifiedEvents(receipt);
            verifiedEvents.forEach(this::processEvidenceVerified);
        } catch (Exception e) {
            log.debug("No EvidenceVerified events found in receipt");
        }

        // Process EvidenceRevoked events
        try {
            List<EvidenceStorageContract.EvidenceRevokedEventResponse> revokedEvents = EvidenceStorageContract
                    .getEvidenceRevokedEvents(receipt);
            revokedEvents.forEach(this::processEvidenceRevoked);
        } catch (Exception e) {
            log.debug("No EvidenceRevoked events found in receipt");
        }
    }

    private void processEvidenceSubmitted(
            EvidenceStorageContract.EvidenceSubmittedEventResponse event) {
        try {
            BigInteger blockTimestamp = getBlockTimestamp(event.log.getBlockNumber());

            executeWithRetry(() -> {
                BlockchainEvent blockchainEvent = createBlockchainEvent(event, "EvidenceSubmitted",
                        blockTimestamp);
                blockchainEventRepository.save(blockchainEvent);
                return null;
            }, "save EvidenceSubmitted event");

            // 直接处理证据提交事件
            processEvidenceSubmittedSync(event.evidenceId, event.user, Numeric.toHexString(event.hashValue), 
                    event.timestamp, event.log.getBlockNumber(), event.log.getTransactionHash(), blockTimestamp);

            log.info("EvidenceSubmitted event processed for evidenceId: {}", event.evidenceId);
        } catch (Exception e) {
            log.error("Error processing EvidenceSubmitted event", e);
            throw new BlockchainException("Failed to process EvidenceSubmitted event", e);
        }
    }

    private void processEvidenceStatusChanged(
            EvidenceStorageContract.EvidenceStatusChangedEventResponse event) {
        try {
            BigInteger blockTimestamp = getBlockTimestamp(event.log.getBlockNumber());

            executeWithRetry(() -> {
                BlockchainEvent blockchainEvent = createBlockchainEvent(event,
                        "EvidenceStatusChanged", blockTimestamp);
                blockchainEventRepository.save(blockchainEvent);
                return null;
            }, "save EvidenceStatusChanged event");

            // 直接处理证据状态变更事件
            processEvidenceStatusChangedSync(event.evidenceId, event.oldStatus, event.newStatus, null);

            log.info("EvidenceStatusChanged event processed for evidenceId: {}", event.evidenceId);
        } catch (Exception e) {
            log.error("Error processing EvidenceStatusChanged event", e);
            throw new BlockchainException("Failed to process EvidenceStatusChanged event", e);
        }
    }

    private void processEvidenceVerified(
            EvidenceStorageContract.EvidenceVerifiedEventResponse event) {
        try {
            BigInteger blockTimestamp = getBlockTimestamp(event.log.getBlockNumber());

            executeWithRetry(() -> {
                BlockchainEvent blockchainEvent = createBlockchainEvent(event, "EvidenceVerified",
                        blockTimestamp);
                blockchainEventRepository.save(blockchainEvent);
                return null;
            }, "save EvidenceVerified event");

            // 直接处理证据验证事件
            processEvidenceVerifiedSync(event.evidenceId, event.isValid);

            log.info("EvidenceVerified event processed for evidenceId: {}", event.evidenceId);
        } catch (Exception e) {
            log.error("Error processing EvidenceVerified event", e);
            throw new BlockchainException("Failed to process EvidenceVerified event", e);
        }
    }

    private void processEvidenceRevoked(
            EvidenceStorageContract.EvidenceRevokedEventResponse event) {
        try {
            BigInteger blockTimestamp = getBlockTimestamp(event.log.getBlockNumber());

            executeWithRetry(() -> {
                BlockchainEvent blockchainEvent = createBlockchainEvent(event, "EvidenceRevoked",
                        blockTimestamp);
                blockchainEventRepository.save(blockchainEvent);
                return null;
            }, "save EvidenceRevoked event");

            // 直接处理证据撤销事件
            processEvidenceRevokedSync(event.evidenceId, event.revoker);

            log.info("EvidenceRevoked event processed for evidenceId: {}", event.evidenceId);
        } catch (Exception e) {
            log.error("Error processing EvidenceRevoked event", e);
            throw new BlockchainException("Failed to process EvidenceRevoked event", e);
        }
    }

    private BlockchainEvent createBlockchainEvent(Object event, String eventType,
            BigInteger blockTimestamp) throws Exception {
        String rawData = objectMapper.writeValueAsString(event);

        // Cast to BaseEventResponse to access public log field directly
        org.web3j.protocol.core.methods.response.BaseEventResponse baseEvent = (org.web3j.protocol.core.methods.response.BaseEventResponse) event;
        org.web3j.protocol.core.methods.response.Log log = baseEvent.log;

        return new BlockchainEvent(evidenceStorageContract.getContractAddress(), eventType,
                log.getBlockNumber(), log.getTransactionHash(),
                blockTimestamp, rawData);
    }

    private void updateSyncStatus(BigInteger blockNumber) {
        executeWithRetry(() -> {
            SyncStatus syncStatus = getOrCreateSyncStatus();
            syncStatus.setLastBlockNumber(blockNumber);
            syncStatus.setLastSyncTimestamp(LocalDateTime.now());
            syncStatus.setSyncStatus("SYNCED");
            syncStatusRepository.save(syncStatus);
            return null;
        }, "update sync status");
    }

    private SyncStatus getOrCreateSyncStatus() {
        return executeWithRetry(() -> {
            String contractAddress = evidenceStorageContract.getContractAddress();
            return syncStatusRepository.findById(contractAddress).orElseGet(() -> {
                SyncStatus newStatus = new SyncStatus(contractAddress, BigInteger.ZERO);
                return syncStatusRepository.save(newStatus);
            });
        }, "get or create sync status");
    }

    public void startEventListening() {
        log.info("Starting blockchain event listener...");

        try {
            syncMissingEventsOnStartup();
            startRealTimeEventListening();

        } catch (Exception e) {
            log.error("Failed to start blockchain event listener", e);
            scheduler.schedule(this::startEventListening, 30, TimeUnit.SECONDS);
        }
    }

    private void startRealTimeEventListening() {
        log.info("Starting real-time blockchain event listening...");

        try {
            // Get the last synced block number from SyncStatus
            SyncStatus syncStatus = getOrCreateSyncStatus();
            BigInteger startBlock = syncStatus.getLastBlockNumber();

            // Start from the block after the last synced block to ensure continuity
            DefaultBlockParameter fromBlock = startBlock.compareTo(BigInteger.ZERO) > 0
                    ? DefaultBlockParameter.valueOf(startBlock.add(BigInteger.ONE))
                    : DefaultBlockParameterName.LATEST;

            log.info("Starting real-time event listening from block: {}",
                    fromBlock instanceof DefaultBlockParameterName ? "LATEST"
                            : startBlock.add(BigInteger.ONE));

            // Start EvidenceSubmitted event subscription
            evidenceStorageContract
                    .evidenceSubmittedEventFlowable(fromBlock, DefaultBlockParameterName.LATEST)
                    .subscribe(event -> {
                        try {
                            processEvidenceSubmitted(event);
                        } catch (Exception e) {
                            log.error("Error processing EvidenceSubmitted event", e);
                        }
                    }, error -> {
                        log.error("Error in EvidenceSubmitted event subscription", error);
                    });

            // Start EvidenceStatusChanged event subscription
            evidenceStorageContract
                    .evidenceStatusChangedEventFlowable(fromBlock, DefaultBlockParameterName.LATEST)
                    .subscribe(event -> {
                        try {
                            processEvidenceStatusChanged(event);
                        } catch (Exception e) {
                            log.error("Error processing EvidenceStatusChanged event", e);
                        }
                    }, error -> {
                        log.error("Error in EvidenceStatusChanged event subscription", error);
                    });

            // Start EvidenceRevoked event subscription
            evidenceStorageContract
                    .evidenceRevokedEventFlowable(fromBlock, DefaultBlockParameterName.LATEST)
                    .subscribe(event -> {
                        try {
                            processEvidenceRevoked(event);
                        } catch (Exception e) {
                            log.error("Error processing EvidenceRevoked event", e);
                        }
                    }, error -> {
                        log.error("Error in EvidenceRevoked event subscription", error);
                    });

            log.info("Real-time blockchain event listening started successfully");

        } catch (Exception e) {
            log.error("Failed to start real-time event listening", e);
            throw new BlockchainException("Failed to start real-time event listening", e);
        }
    }

    private void syncMissingEventsOnStartup() {
        try {
            BigInteger currentBlock = getCurrentBlockNumber();
            SyncStatus syncStatus = getOrCreateSyncStatus();
            BigInteger lastSyncedBlock = syncStatus.getLastBlockNumber();

            BigInteger blocksBehind = currentBlock.subtract(lastSyncedBlock);
            if (blocksBehind.compareTo(BigInteger.TEN) > 0) {
                log.info("Detected {} blocks behind current block. Syncing missing events...",
                        blocksBehind);

                syncInBatches(lastSyncedBlock.add(BigInteger.ONE), currentBlock,
                        BigInteger.valueOf(1000));
            }

        } catch (Exception e) {
            log.error("Failed to sync missing events on startup", e);
        }
    }

    private void syncInBatches(BigInteger startBlock, BigInteger endBlock, BigInteger batchSize) {
        log.info("Starting sync of blocks {} to {} with batch size {}", startBlock, endBlock,
                batchSize);
        BigInteger current = startBlock;

        while (current.compareTo(endBlock) <= 0) {
            BigInteger batchEnd = current.add(batchSize).subtract(BigInteger.ONE);
            if (batchEnd.compareTo(endBlock) > 0) {
                batchEnd = endBlock;
            }

            try {
                syncPastEvents(current, batchEnd);
                log.info("Synced blocks {} to {}", current, batchEnd);

                current = batchEnd.add(BigInteger.ONE);
                // Add small delay to avoid overwhelming the node
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }

            } catch (Exception e) {
                log.error("Failed to sync blocks {} to {}", current, batchEnd, e);
                current = batchEnd.add(BigInteger.ONE);
            }
        }

        log.info("Completed sync from block {} to {}", startBlock, endBlock);
    }

    private BigInteger getCurrentBlockNumber() {
        try {
            org.web3j.protocol.core.methods.response.EthBlockNumber blockNumber = web3j
                    .ethBlockNumber().send();
            if (blockNumber == null) {
                throw new BlockchainException("Block number response is null");
            }
            return blockNumber.getBlockNumber();
        } catch (IOException e) {
            throw new BlockchainException("Failed to get current block number", e);
        }
    }

    public void shutdown() {
        log.info("Shutting down EvidenceEventListener...");

        // Shutdown scheduler
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("Scheduler did not terminate gracefully, forcing shutdown");
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("Scheduler shutdown interrupted, forcing immediate shutdown");
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("EvidenceEventListener shutdown completed");
    }

    // 同步处理方法实现
    private void processEvidenceSubmittedSync(String evidenceId, String user, String hashValue, 
            BigInteger timestamp, BigInteger blockNumber, String transactionHash, BigInteger blockTimestamp) {
        executeWithRetry(() -> {
            // 检查证据是否已存在
            if (evidenceRepository.existsByEvidenceId(evidenceId)) {
                log.info("Evidence {} already exists, skipping", evidenceId);
                return null;
            }

            // 尝试从智能合约获取完整证据数据
            EvidenceEntity evidence = getCompleteEvidenceFromContract(evidenceId);
            if (evidence == null) {
                // 如果合约调用失败，使用事件数据创建基本证据记录
                evidence = new EvidenceEntity(evidenceId, user, "", "", 0L, timestamp, "SHA256", 
                        hashValue, blockNumber, transactionHash, blockTimestamp, "");
            } else {
                // 更新区块链特定字段
                evidence.setTransactionHash(transactionHash);
            }

            evidence.setStatus("effective");
            evidenceRepository.save(evidence);
            log.info("Created new evidence record for evidenceId: {}", evidenceId);
            return null;
        }, "processEvidenceSubmittedSync");
    }

    private void processEvidenceStatusChangedSync(String evidenceId, String oldStatus, String newStatus, String user) {
        executeWithRetry(() -> {
            EvidenceEntity evidence = evidenceRepository.findByEvidenceId(evidenceId)
                    .orElseThrow(() -> new EvidenceNotFoundException("Evidence not found: " + evidenceId));

            evidence.setStatus(newStatus);

            if ("revoked".equals(newStatus) && user != null) {
                evidence.setRevokedAt(LocalDateTime.now());
                evidence.setRevokerAddress(user);
            }

            evidenceRepository.save(evidence);
            log.info("Updated evidence status from {} to {} for evidenceId: {}", oldStatus, newStatus, evidenceId);
            return null;
        }, "processEvidenceStatusChangedSync");
    }

    private void processEvidenceVerifiedSync(String evidenceId, Boolean isValid) {
        log.info("Evidence verified for evidenceId: {}, isValid: {}", evidenceId, isValid != null ? isValid : "unknown");
        // 验证操作不需要存储状态，只是记录日志
    }

    private void processEvidenceRevokedSync(String evidenceId, String revoker) {
        executeWithRetry(() -> {
            EvidenceEntity evidence = evidenceRepository.findByEvidenceId(evidenceId)
                    .orElseThrow(() -> new EvidenceNotFoundException("Evidence not found: " + evidenceId));

            evidence.setStatus("revoked");
            evidence.setRevokedAt(LocalDateTime.now());
            evidence.setRevokerAddress(revoker);

            evidenceRepository.save(evidence);
            log.info("Revoked evidence for evidenceId: {}", evidenceId);
            return null;
        }, "processEvidenceRevokedSync");
    }

    private EvidenceEntity getCompleteEvidenceFromContract(String evidenceId) {
        try {
            EvidenceStorageContract.Evidence contractEvidence = evidenceStorageContract
                    .getEvidence(evidenceId).send();

            if (contractEvidence != null && contractEvidence.exists) {
                return new EvidenceEntity(
                        contractEvidence.evidenceId != null ? contractEvidence.evidenceId : "",
                        contractEvidence.userId != null ? contractEvidence.userId : "",
                        contractEvidence.metadata != null ? contractEvidence.metadata.fileName : "",
                        contractEvidence.metadata != null ? contractEvidence.metadata.mimeType : "",
                        contractEvidence.metadata != null ? contractEvidence.metadata.size.longValue() : 0L,
                        contractEvidence.metadata != null ? contractEvidence.metadata.size : BigInteger.ZERO,
                        contractEvidence.hash != null ? contractEvidence.hash.algorithm : "SHA256",
                        contractEvidence.hash != null ? Numeric.toHexString(contractEvidence.hash.value) : "",
                        contractEvidence.blockHeight != null ? contractEvidence.blockHeight : BigInteger.ZERO,
                        "", // transactionHash will be set by caller
                        contractEvidence.timestamp != null ? contractEvidence.timestamp : BigInteger.ZERO,
                        contractEvidence.memo != null ? contractEvidence.memo : "");
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve evidence {} from smart contract: {}", evidenceId, e.getMessage());
        }
        return null;
    }
}