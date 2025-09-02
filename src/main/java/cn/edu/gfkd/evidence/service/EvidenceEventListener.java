package cn.edu.gfkd.evidence.service;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.util.encoders.Hex;

import org.bouncycastle.jcajce.provider.asymmetric.ec.SignatureSpi.ecNR;
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

            // Schedule periodic sync checks
            scheduler.scheduleAtFixedRate(this::checkAndSyncMissingEvents, 3, 30, TimeUnit.SECONDS);

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
        EthBlock block = web3j.ethGetBlockByNumber(
                DefaultBlockParameter.valueOf(blockNumber), false).send();
        return block.getBlock().getTimestamp();
    }

    public String getContractAddress() {
        return evidenceStorage.getContractAddress();
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
            List<EthLog.LogResult> logResults = ethLogs.getLogs();

            for (EthLog.LogResult logResult : logResults) {
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

    private void processLogResult(EthLog.LogResult logResult) throws Exception {
        Object logObj = logResult.get();
        if (logObj != null) {
            // The actual object returned is of type Log which extends EthLog.Log
            org.web3j.protocol.core.methods.response.Log log = (org.web3j.protocol.core.methods.response.Log) logObj;
            String txHash = log.getTransactionHash();
            TransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt()
                    .orElseThrow(() -> new BlockchainException("Transaction receipt not found: " + txHash));

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

            BlockchainEventReceived eventReceived = createEvidenceSubmittedEvent(event, blockTimestamp);
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
            EvidenceStorage.EvidenceSubmittedEventResponse event,
            BigInteger blockTimestamp) throws Exception {

        return BlockchainEventReceived.builder()
                .contractAddress(evidenceStorage.getContractAddress())
                .eventName("EvidenceSubmitted")
                .blockNumber(event.log.getBlockNumber())
                .transactionHash(event.log.getTransactionHash())
                .logIndex(BigInteger.valueOf(event.log.getLogIndex().longValue()))
                .blockTimestamp(blockTimestamp)
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

        evidenceStorage.evidenceSubmittedEventFlowable(
                DefaultBlockParameterName.EARLIEST,
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

        evidenceStorage.evidenceStatusChangedEventFlowable(
                DefaultBlockParameterName.EARLIEST,
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

        evidenceStorage.evidenceRevokedEventFlowable(
                DefaultBlockParameterName.EARLIEST,
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

    public void checkAndSyncMissingEvents() {
        try {
            if (!isBlockchainConnected()) {
                log.warn("Blockchain node not connected. Skipping sync check.");
                return;
            }

            BigInteger currentBlock = getCurrentBlockNumber();
            SyncStatus syncStatus = syncStatusRepository.findById(evidenceStorage.getContractAddress()).orElse(null);

            if (syncStatus == null) {
                log.warn("No sync status found. Skipping sync check.");
                return;
            }

            BigInteger lastSyncedBlock = syncStatus.getLastBlockNumber();
            BigInteger blocksBehind = currentBlock.subtract(lastSyncedBlock);

            if (blocksBehind.compareTo(BigInteger.valueOf(1)) > 0) {
                log.info("Detected {} blocks behind. Triggering sync...", blocksBehind);

                BigInteger startBlock = lastSyncedBlock.add(BigInteger.ONE);
                BigInteger endBlock = startBlock.add(BigInteger.valueOf(99));

                if (endBlock.compareTo(currentBlock) > 0) {
                    endBlock = currentBlock;
                }

                syncPastEvents(startBlock, endBlock);
            }

        } catch (Exception e) {
            log.error("Failed to check and sync missing events", e);
        }
    }

    private void syncInBatches(BigInteger startBlock, BigInteger endBlock, BigInteger batchSize) {
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
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("Failed to sync blocks {} to {}", current, batchEnd, e);
                current = batchEnd.add(BigInteger.ONE);
            }
        }
    }

    private BigInteger getCurrentBlockNumber() {
        try {
            return web3j.ethBlockNumber().send().getBlockNumber();
        } catch (IOException e) {
            throw new BlockchainException("Failed to get current block number", e);
        }
    }

    private boolean isBlockchainConnected() {
        try {
            web3j.ethBlockNumber().send();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void shutdown() {
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
    }
}