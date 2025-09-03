package cn.edu.gfkd.evidence.service;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.edu.gfkd.evidence.entity.BlockchainEvent;
import cn.edu.gfkd.evidence.entity.SyncStatus;
import cn.edu.gfkd.evidence.event.BlockchainEventReceived;
import cn.edu.gfkd.evidence.exception.BlockchainException;
import cn.edu.gfkd.evidence.generated.EvidenceStorage;
import cn.edu.gfkd.evidence.repository.BlockchainEventRepository;
import cn.edu.gfkd.evidence.repository.SyncStatusRepository;
import cn.edu.gfkd.evidence.utils.ContractUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvidenceEventListener {

    private final Web3j web3j;
    private final BlockchainEventRepository blockchainEventRepository;
    private final SyncStatusRepository syncStatusRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final Credentials credentials;

    @Value("${blockchain.contract.evidencestorage:EvidenceStorage}")
    private String contractName;

    @Value("${blockchain.node.timeout:5000}")
    private long nodeTimeout;

    @Value("${blockchain.node.network:localhost}")
    private String network;

    private EvidenceStorage evidenceStorage;
    private ScheduledExecutorService scheduler;

    public void init() {
        scheduler = Executors.newScheduledThreadPool(2);

        try {
            loadContract();

            // Start event listening after a delay
            scheduler.schedule(this::startEventListening, 3, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("Failed to initialize blockchain event listener", e);
            throw new BlockchainException("Failed to initialize blockchain event listener", e);
        }
    }

    private void loadContract() {
        try {
            String deployedAddress = ContractUtils.getDeployedContractAddress(contractName, network);
            evidenceStorage = EvidenceStorage.load(
                    deployedAddress,
                    web3j,
                    credentials, new DefaultGasProvider());
            log.info("EvidenceStorage contract loaded at: {}", deployedAddress);

        } catch (Exception e) {
            log.error("Failed to load EvidenceStorage contract", e);
            throw new BlockchainException("Failed to load EvidenceStorage contract", e);
        }
    }

    private BigInteger getBlockTimestamp(BigInteger blockNumber) throws IOException {
        EthBlock ethBlock = web3j.ethGetBlockByNumber(
                DefaultBlockParameter.valueOf(blockNumber), false).send();
        if (ethBlock.getBlock() == null) {
            throw new BlockchainException("Block not found: " + blockNumber);
        }
        return ethBlock.getBlock().getTimestamp();
    }

    public String getContractAddress() {
        return evidenceStorage.getContractAddress();
    }

    public EvidenceStorage.Evidence getEvidence(String evidenceId) {
        try {
            // Call the smart contract and get the raw response
            EvidenceStorage.Evidence evidence = evidenceStorage.getEvidence(evidenceId).send();

            if (evidence == null || !evidence.exists) {
                log.warn("Evidence {} does not exist in smart contract", evidenceId);
                return null;
            }

            return evidence;
        } catch (Exception e) {
            log.error("Failed to get evidence {} from smart contract: {}", evidenceId, e.getMessage());
            throw new BlockchainException("Failed to get evidence from smart contract", e);
        }
    }

    @Transactional
    public void syncPastEvents(BigInteger startBlock, BigInteger endBlock) {
        try {
            log.info("Syncing past events from block {} to {}", startBlock, endBlock);

            EthFilter filter = new EthFilter(
                    DefaultBlockParameter.valueOf(startBlock),
                    DefaultBlockParameter.valueOf(endBlock),
                    evidenceStorage.getContractAddress());

            EthLog ethLogs = web3j.ethGetLogs(filter).send();
            @SuppressWarnings("unchecked")
            List<EthLog.LogResult<Log>> logResults = (List<EthLog.LogResult<Log>>) (List<?>) ethLogs.getLogs();

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
            TransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt()
                    .orElseThrow(() -> new BlockchainException("Transaction receipt not found: " + txHash));
            if (receipt == null) {
                throw new BlockchainException("Transaction receipt is null for: " + txHash);
            }

            processAllEventTypes(receipt);
        }
    }

    private void processAllEventTypes(TransactionReceipt receipt) throws IOException {
        // Process EvidenceSubmitted events
        try {
            List<EvidenceStorage.EvidenceSubmittedEventResponse> submittedEvents = cn.edu.gfkd.evidence.generated.EvidenceStorage
                    .getEvidenceSubmittedEvents(receipt);
            submittedEvents.forEach(this::processEvidenceSubmitted);
        } catch (Exception e) {
            log.debug("No EvidenceSubmitted events found in receipt");
        }

        // Process EvidenceStatusChanged events
        try {
            List<EvidenceStorage.EvidenceStatusChangedEventResponse> statusChangedEvents = cn.edu.gfkd.evidence.generated.EvidenceStorage
                    .getEvidenceStatusChangedEvents(receipt);
            statusChangedEvents.forEach(this::processEvidenceStatusChanged);
        } catch (Exception e) {
            log.debug("No EvidenceStatusChanged events found in receipt");
        }

        // Process EvidenceVerified events
        try {
            List<EvidenceStorage.EvidenceVerifiedEventResponse> verifiedEvents = cn.edu.gfkd.evidence.generated.EvidenceStorage
                    .getEvidenceVerifiedEvents(receipt);
            verifiedEvents.forEach(this::processEvidenceVerified);
        } catch (Exception e) {
            log.debug("No EvidenceVerified events found in receipt");
        }

        // Process EvidenceRevoked events
        try {
            List<EvidenceStorage.EvidenceRevokedEventResponse> revokedEvents = cn.edu.gfkd.evidence.generated.EvidenceStorage
                    .getEvidenceRevokedEvents(receipt);
            revokedEvents.forEach(this::processEvidenceRevoked);
        } catch (Exception e) {
            log.debug("No EvidenceRevoked events found in receipt");
        }
    }

    private void processEvidenceSubmitted(
            EvidenceStorage.EvidenceSubmittedEventResponse event) {
        try {
            BigInteger blockTimestamp = getBlockTimestamp(event.log.getBlockNumber());

            BlockchainEvent blockchainEvent = createBlockchainEvent(event, "EvidenceSubmitted", blockTimestamp);
            blockchainEventRepository.save(blockchainEvent);

            BlockchainEventReceived eventReceived = createEvidenceSubmittedEvent(event);
            eventPublisher.publishEvent(eventReceived);

            log.info("EvidenceSubmitted event processed for evidenceId: {}", event.evidenceId);
        } catch (Exception e) {
            log.error("Error processing EvidenceSubmitted event", e);
            throw new BlockchainException("Failed to process EvidenceSubmitted event", e);
        }
    }

    private void processEvidenceStatusChanged(
            EvidenceStorage.EvidenceStatusChangedEventResponse event) {
        try {
            BigInteger blockTimestamp = getBlockTimestamp(event.log.getBlockNumber());

            BlockchainEvent blockchainEvent = createBlockchainEvent(event, "EvidenceStatusChanged", blockTimestamp);
            blockchainEventRepository.save(blockchainEvent);

            BlockchainEventReceived eventReceived = createEvidenceStatusChangedEvent(event, blockTimestamp);
            eventPublisher.publishEvent(eventReceived);

            log.info("EvidenceStatusChanged event processed for evidenceId: {}", event.evidenceId);
        } catch (Exception e) {
            log.error("Error processing EvidenceStatusChanged event", e);
            throw new BlockchainException("Failed to process EvidenceStatusChanged event", e);
        }
    }

    private void processEvidenceVerified(
            EvidenceStorage.EvidenceVerifiedEventResponse event) {
        try {
            BigInteger blockTimestamp = getBlockTimestamp(event.log.getBlockNumber());

            BlockchainEvent blockchainEvent = createBlockchainEvent(event, "EvidenceVerified", blockTimestamp);
            blockchainEventRepository.save(blockchainEvent);

            BlockchainEventReceived eventReceived = createEvidenceVerifiedEvent(event, blockTimestamp);
            eventPublisher.publishEvent(eventReceived);

            log.info("EvidenceVerified event processed for evidenceId: {}", event.evidenceId);
        } catch (Exception e) {
            log.error("Error processing EvidenceVerified event", e);
            throw new BlockchainException("Failed to process EvidenceVerified event", e);
        }
    }

    private void processEvidenceRevoked(
            EvidenceStorage.EvidenceRevokedEventResponse event) {
        try {
            BigInteger blockTimestamp = getBlockTimestamp(event.log.getBlockNumber());

            BlockchainEvent blockchainEvent = createBlockchainEvent(event, "EvidenceRevoked", blockTimestamp);
            blockchainEventRepository.save(blockchainEvent);

            BlockchainEventReceived eventReceived = createEvidenceRevokedEvent(event, blockTimestamp);
            eventPublisher.publishEvent(eventReceived);

            log.info("EvidenceRevoked event processed for evidenceId: {}", event.evidenceId);
        } catch (Exception e) {
            log.error("Error processing EvidenceRevoked event", e);
            throw new BlockchainException("Failed to process EvidenceRevoked event", e);
        }
    }

    private BlockchainEvent createBlockchainEvent(Object event, String eventType, BigInteger blockTimestamp)
            throws Exception {
        String rawData = objectMapper.writeValueAsString(event);

        // Cast to BaseEventResponse to access public log field directly
        org.web3j.protocol.core.methods.response.BaseEventResponse baseEvent = (org.web3j.protocol.core.methods.response.BaseEventResponse) event;
        org.web3j.protocol.core.methods.response.Log log = baseEvent.log;

        return new BlockchainEvent(
                evidenceStorage.getContractAddress(),
                eventType,
                log.getBlockNumber(),
                log.getTransactionHash(),
                BigInteger.valueOf(log.getLogIndex().longValue()),
                blockTimestamp,
                rawData);
    }

    private BlockchainEventReceived createEvidenceSubmittedEvent(
            EvidenceStorage.EvidenceSubmittedEventResponse event) throws Exception {

        return BlockchainEventReceived.builder()
                .contractAddress(evidenceStorage.getContractAddress())
                .eventName("EvidenceSubmitted")
                .blockNumber(event.log.getBlockNumber())
                .transactionHash(event.log.getTransactionHash())
                .logIndex(BigInteger.valueOf(event.log.getLogIndex().longValue()))
                .blockTimestamp(event.timestamp)
                .rawData(objectMapper.writeValueAsString(event))
                .parameter("evidenceId", event.evidenceId)
                .parameter("user", event.user)
                .parameter("hashValue", Numeric.toHexString(event.hashValue))
                .parameter("timestamp", event.timestamp)
                .build();
    }

    private BlockchainEventReceived createEvidenceStatusChangedEvent(
            EvidenceStorage.EvidenceStatusChangedEventResponse event,
            BigInteger blockTimestamp) throws Exception {

        return BlockchainEventReceived.builder()
                .contractAddress(evidenceStorage.getContractAddress())
                .eventName("EvidenceStatusChanged")
                .blockNumber(event.log.getBlockNumber())
                .transactionHash(event.log.getTransactionHash())
                .logIndex(BigInteger.valueOf(event.log.getLogIndex().longValue()))
                .blockTimestamp(blockTimestamp)
                .rawData(objectMapper.writeValueAsString(event))
                .parameter("evidenceId", event.evidenceId)
                .parameter("oldStatus", event.oldStatus)
                .parameter("newStatus", event.newStatus)
                .parameter("timestamp", event.timestamp)
                .build();
    }

    private BlockchainEventReceived createEvidenceVerifiedEvent(
            EvidenceStorage.EvidenceVerifiedEventResponse event,
            BigInteger blockTimestamp) throws Exception {

        return BlockchainEventReceived.builder()
                .contractAddress(evidenceStorage.getContractAddress())
                .eventName("EvidenceVerified")
                .blockNumber(event.log.getBlockNumber())
                .transactionHash(event.log.getTransactionHash())
                .logIndex(BigInteger.valueOf(event.log.getLogIndex().longValue()))
                .blockTimestamp(blockTimestamp)
                .rawData(objectMapper.writeValueAsString(event))
                .parameter("evidenceId", event.evidenceId)
                .parameter("isValid", event.isValid)
                .parameter("timestamp", event.timestamp)
                .build();
    }

    private BlockchainEventReceived createEvidenceRevokedEvent(
            EvidenceStorage.EvidenceRevokedEventResponse event,
            BigInteger blockTimestamp) throws Exception {

        return BlockchainEventReceived.builder()
                .contractAddress(evidenceStorage.getContractAddress())
                .eventName("EvidenceRevoked")
                .blockNumber(event.log.getBlockNumber())
                .transactionHash(event.log.getTransactionHash())
                .logIndex(BigInteger.valueOf(event.log.getLogIndex().longValue()))
                .blockTimestamp(blockTimestamp)
                .rawData(objectMapper.writeValueAsString(event))
                .parameter("evidenceId", event.evidenceId)
                .parameter("revoker", event.revoker)
                .parameter("timestamp", event.timestamp)
                .build();
    }

    private void updateSyncStatus(BigInteger blockNumber) {
        SyncStatus syncStatus = getOrCreateSyncStatus();
        syncStatus.setLastBlockNumber(blockNumber);
        syncStatus.setLastSyncTimestamp(LocalDateTime.now());
        syncStatus.setSyncStatus("SYNCED");
        syncStatusRepository.save(syncStatus);
    }

    private SyncStatus getOrCreateSyncStatus() {
        try {
            String contractAddress = evidenceStorage.getContractAddress();
            return syncStatusRepository.findById(contractAddress)
                    .orElseGet(() -> {
                        SyncStatus newStatus = new SyncStatus(contractAddress, BigInteger.ZERO);
                        return syncStatusRepository.save(newStatus);
                    });
        } catch (Exception e) {
            log.error("Failed to get or create sync status", e);
            throw new BlockchainException("Failed to get or create sync status", e);
        }
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
                    fromBlock instanceof DefaultBlockParameterName ? "LATEST" : startBlock.add(BigInteger.ONE));

            // Start EvidenceSubmitted event subscription
            evidenceStorage.evidenceSubmittedEventFlowable(
                    fromBlock,
                    DefaultBlockParameterName.LATEST)
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
            evidenceStorage.evidenceStatusChangedEventFlowable(
                    fromBlock,
                    DefaultBlockParameterName.LATEST)
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
            evidenceStorage.evidenceRevokedEventFlowable(
                    fromBlock,
                    DefaultBlockParameterName.LATEST)
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
                log.info("Detected {} blocks behind current block. Syncing missing events...", blocksBehind);

                syncInBatches(lastSyncedBlock.add(BigInteger.ONE), currentBlock, BigInteger.valueOf(1000));
            }

        } catch (Exception e) {
            log.error("Failed to sync missing events on startup", e);
        }
    }

    private void syncInBatches(BigInteger startBlock, BigInteger endBlock, BigInteger batchSize) {
        log.info("Starting sync of blocks {} to {} with batch size {}", startBlock, endBlock, batchSize);
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
            org.web3j.protocol.core.methods.response.EthBlockNumber blockNumber = web3j.ethBlockNumber().send();
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
}