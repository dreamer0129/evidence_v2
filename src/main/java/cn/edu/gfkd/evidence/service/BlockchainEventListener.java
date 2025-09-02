package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;
import cn.edu.gfkd.evidence.entity.SyncStatus;
import cn.edu.gfkd.evidence.event.BlockchainEventReceived;
import cn.edu.gfkd.evidence.generated.EvidenceStorage;
import cn.edu.gfkd.evidence.repository.BlockchainEventRepository;
import cn.edu.gfkd.evidence.repository.SyncStatusRepository;
import cn.edu.gfkd.evidence.utils.ContractUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.crypto.Credentials;
import org.web3j.tx.Contract;

import java.io.IOException;
import org.web3j.utils.Numeric;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class BlockchainEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(BlockchainEventListener.class);
    
    @Autowired
    private Web3j web3j;
    
    @Autowired
    private BlockchainEventRepository blockchainEventRepository;
    
    @Autowired
    private SyncStatusRepository syncStatusRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private EvidenceStorage evidenceStorage;
    
    @PostConstruct
    public void init() {
        try {
            String contractAddress = ContractUtils.getDeployedContractAddress("EvidenceStorage", "localhost");
            evidenceStorage = EvidenceStorage.load(
                contractAddress, 
                web3j, 
                Credentials.create("0x0000000000000000000000000000000000000000000000000000000000000000"), 
                Contract.GAS_PRICE, 
                Contract.GAS_LIMIT
            );
            logger.info("EvidenceStorage contract loaded at: {}", contractAddress);
        } catch (Exception e) {
            logger.error("Failed to load EvidenceStorage contract", e);
            throw new RuntimeException("Failed to load EvidenceStorage contract", e);
        }
    }
    
    @Async
    public void startEventListening() {
        try {
            SyncStatus syncStatus = getOrCreateSyncStatus();
            BigInteger startBlock = syncStatus.getLastBlockNumber().add(BigInteger.ONE);
            
            logger.info("Starting event listening from block: {}", startBlock);
            
            EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(startBlock),
                DefaultBlockParameter.valueOf("latest"),
                evidenceStorage.getContractAddress()
            );
            
            // EvidenceSubmitted event
            evidenceStorage.evidenceSubmittedEventFlowable(filter)
                .subscribe(event -> {
                    try {
                        processEvidenceSubmitted(event);
                    } catch (Exception e) {
                        logger.error("Error processing EvidenceSubmitted event", e);
                    }
                }, error -> {
                    logger.error("Error in EvidenceSubmitted event subscription", error);
                });
            
            // EvidenceStatusChanged event
            evidenceStorage.evidenceStatusChangedEventFlowable(filter)
                .subscribe(event -> {
                    try {
                        processEvidenceStatusChanged(event);
                    } catch (Exception e) {
                        logger.error("Error processing EvidenceStatusChanged event", e);
                    }
                }, error -> {
                    logger.error("Error in EvidenceStatusChanged event subscription", error);
                });
            
            // EvidenceVerified event
            evidenceStorage.evidenceVerifiedEventFlowable(filter)
                .subscribe(event -> {
                    try {
                        processEvidenceVerified(event);
                    } catch (Exception e) {
                        logger.error("Error processing EvidenceVerified event", e);
                    }
                }, error -> {
                    logger.error("Error in EvidenceVerified event subscription", error);
                });
            
            // EvidenceRevoked event
            evidenceStorage.evidenceRevokedEventFlowable(filter)
                .subscribe(event -> {
                    try {
                        processEvidenceRevoked(event);
                    } catch (Exception e) {
                        logger.error("Error processing EvidenceRevoked event", e);
                    }
                }, error -> {
                    logger.error("Error in EvidenceRevoked event subscription", error);
                });
            
        } catch (Exception e) {
            logger.error("Failed to start event listening", e);
        }
    }
    
    private void processEvidenceSubmitted(EvidenceStorage.EvidenceSubmittedEventResponse event) throws IOException {
        BigInteger blockTimestamp = getBlockTimestamp(event.log.getBlockNumber());
        
        BlockchainEvent blockchainEvent = new BlockchainEvent(
            evidenceStorage.getContractAddress(),
            "EvidenceSubmitted",
            event.log.getBlockNumber(),
            event.log.getTransactionHash(),
            BigInteger.valueOf(event.log.getLogIndex().longValue()),
            blockTimestamp,
            objectMapper.writeValueAsString(event)
        );
        
        blockchainEventRepository.save(blockchainEvent);
        
        BlockchainEventReceived eventReceived = new BlockchainEventReceived(
            evidenceStorage.getContractAddress(),
            "EvidenceSubmitted",
            Numeric.toHexString(event.evidenceId),
            event.user,
            Numeric.toHexString(event.hashValue),
            null,
            null,
            event.log.getBlockNumber(),
            event.log.getTransactionHash(),
            BigInteger.valueOf(event.log.getLogIndex().longValue()),
            blockTimestamp,
            true
        );
        
        eventPublisher.publishEvent(eventReceived);
        logger.info("EvidenceSubmitted event processed for evidenceId: {}", event.evidenceId);
    }
    
    private void processEvidenceStatusChanged(EvidenceStorage.EvidenceStatusChangedEventResponse event) throws IOException {
        BigInteger blockTimestamp = getBlockTimestamp(event.log.getBlockNumber());
        
        BlockchainEvent blockchainEvent = new BlockchainEvent(
            evidenceStorage.getContractAddress(),
            "EvidenceStatusChanged",
            event.log.getBlockNumber(),
            event.log.getTransactionHash(),
            BigInteger.valueOf(event.log.getLogIndex().longValue()),
            blockTimestamp,
            objectMapper.writeValueAsString(event)
        );
        
        blockchainEventRepository.save(blockchainEvent);
        
        BlockchainEventReceived eventReceived = new BlockchainEventReceived(
            evidenceStorage.getContractAddress(),
            "EvidenceStatusChanged",
            Numeric.toHexString(event.evidenceId),
            null,
            null,
            event.oldStatus,
            event.newStatus,
            event.log.getBlockNumber(),
            event.log.getTransactionHash(),
            BigInteger.valueOf(event.log.getLogIndex().longValue()),
            blockTimestamp,
            true
        );
        
        eventPublisher.publishEvent(eventReceived);
        logger.info("EvidenceStatusChanged event processed for evidenceId: {}", event.evidenceId);
    }
    
    private void processEvidenceVerified(EvidenceStorage.EvidenceVerifiedEventResponse event) throws IOException {
        BigInteger blockTimestamp = getBlockTimestamp(event.log.getBlockNumber());
        
        BlockchainEvent blockchainEvent = new BlockchainEvent(
            evidenceStorage.getContractAddress(),
            "EvidenceVerified",
            event.log.getBlockNumber(),
            event.log.getTransactionHash(),
            BigInteger.valueOf(event.log.getLogIndex().longValue()),
            blockTimestamp,
            objectMapper.writeValueAsString(event)
        );
        
        blockchainEventRepository.save(blockchainEvent);
        
        BlockchainEventReceived eventReceived = new BlockchainEventReceived(
            evidenceStorage.getContractAddress(),
            "EvidenceVerified",
            Numeric.toHexString(event.evidenceId),
            null,
            null,
            null,
            event.isValid ? "verified" : "invalid",
            event.log.getBlockNumber(),
            event.log.getTransactionHash(),
            BigInteger.valueOf(event.log.getLogIndex().longValue()),
            blockTimestamp,
            event.isValid
        );
        
        eventPublisher.publishEvent(eventReceived);
        logger.info("EvidenceVerified event processed for evidenceId: {}", event.evidenceId);
    }
    
    private void processEvidenceRevoked(EvidenceStorage.EvidenceRevokedEventResponse event) throws IOException {
        BigInteger blockTimestamp = getBlockTimestamp(event.log.getBlockNumber());
        
        BlockchainEvent blockchainEvent = new BlockchainEvent(
            evidenceStorage.getContractAddress(),
            "EvidenceRevoked",
            event.log.getBlockNumber(),
            event.log.getTransactionHash(),
            BigInteger.valueOf(event.log.getLogIndex().longValue()),
            blockTimestamp,
            objectMapper.writeValueAsString(event)
        );
        
        blockchainEventRepository.save(blockchainEvent);
        
        BlockchainEventReceived eventReceived = new BlockchainEventReceived(
            evidenceStorage.getContractAddress(),
            "EvidenceRevoked",
            Numeric.toHexString(event.evidenceId),
            null,
            null,
            null,
            "revoked",
            event.log.getBlockNumber(),
            event.log.getTransactionHash(),
            BigInteger.valueOf(event.log.getLogIndex().longValue()),
            blockTimestamp,
            true
        );
        
        eventPublisher.publishEvent(eventReceived);
        logger.info("EvidenceRevoked event processed for evidenceId: {}", event.evidenceId);
    }
    
    private BigInteger getBlockTimestamp(BigInteger blockNumber) throws IOException {
        EthBlock block = web3j.ethGetBlockByNumber(
            DefaultBlockParameter.valueOf(blockNumber), 
            false
        ).send();
        
        return block.getBlock().getTimestamp();
    }
    
    public String getContractAddress() {
        return evidenceStorage.getContractAddress();
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
            logger.error("Failed to get or create sync status", e);
            throw new RuntimeException("Failed to get or create sync status", e);
        }
    }
    
    public void syncPastEvents(BigInteger startBlock, BigInteger endBlock) {
        try {
            logger.info("Syncing past events from block {} to {}", startBlock, endBlock);
            
            EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(startBlock),
                DefaultBlockParameter.valueOf(endBlock),
                evidenceStorage.getContractAddress()
            );
            
            // Get all logs from the filter
            EthLog ethLogs = web3j.ethGetLogs(filter).send();
            List<EthLog.LogResult> logResults = ethLogs.getLogs();
            
            for (EthLog.LogResult logResult : logResults) {
                try {
                    // Use reflection to avoid direct class reference issues
                    Object log = logResult.get();
                    if (log != null) {
                        String txHash = (String) log.getClass().getMethod("getTransactionHash").invoke(log);
                        TransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt().get();
                        
                            // Try to parse as different event types
                        try {
                        List<EvidenceStorage.EvidenceSubmittedEventResponse> submittedEvents = evidenceStorage.getEvidenceSubmittedEvents(receipt);
                        if (!submittedEvents.isEmpty()) {
                            for (EvidenceStorage.EvidenceSubmittedEventResponse event : submittedEvents) {
                                processEvidenceSubmitted(event);
                            }
                        }
                    } catch (Exception e) {
                        // Not an EvidenceSubmitted event
                    }
                    
                    try {
                        List<EvidenceStorage.EvidenceStatusChangedEventResponse> statusChangedEvents = evidenceStorage.getEvidenceStatusChangedEvents(receipt);
                        if (!statusChangedEvents.isEmpty()) {
                            for (EvidenceStorage.EvidenceStatusChangedEventResponse event : statusChangedEvents) {
                                processEvidenceStatusChanged(event);
                            }
                        }
                    } catch (Exception e) {
                        // Not an EvidenceStatusChanged event
                    }
                    
                    try {
                        List<EvidenceStorage.EvidenceVerifiedEventResponse> verifiedEvents = evidenceStorage.getEvidenceVerifiedEvents(receipt);
                        if (!verifiedEvents.isEmpty()) {
                            for (EvidenceStorage.EvidenceVerifiedEventResponse event : verifiedEvents) {
                                processEvidenceVerified(event);
                            }
                        }
                    } catch (Exception e) {
                        // Not an EvidenceVerified event
                    }
                    
                    try {
                        List<EvidenceStorage.EvidenceRevokedEventResponse> revokedEvents = evidenceStorage.getEvidenceRevokedEvents(receipt);
                        if (!revokedEvents.isEmpty()) {
                            for (EvidenceStorage.EvidenceRevokedEventResponse event : revokedEvents) {
                                processEvidenceRevoked(event);
                            }
                        }
                    } catch (Exception e) {
                        // Not an EvidenceRevoked event
                    }
                } catch (Exception e) {
                    logger.error("Error processing log result", e);
                }
            }
            
            // Update sync status
            SyncStatus syncStatus = getOrCreateSyncStatus();
            syncStatus.setLastBlockNumber(endBlock);
            syncStatus.setLastSyncTimestamp(java.time.LocalDateTime.now());
            syncStatus.setSyncStatus("SYNCED");
            syncStatusRepository.save(syncStatus);
            
            logger.info("Completed syncing past events from block {} to {}", startBlock, endBlock);
            
        } catch (Exception e) {
            logger.error("Failed to sync past events", e);
            throw new RuntimeException("Failed to sync past events", e);
        }
    }
}