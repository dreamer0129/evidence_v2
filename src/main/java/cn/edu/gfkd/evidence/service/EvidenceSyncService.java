package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;
import cn.edu.gfkd.evidence.entity.Evidence;
import cn.edu.gfkd.evidence.entity.SyncStatus;
import cn.edu.gfkd.evidence.event.BlockchainEventReceived;
import cn.edu.gfkd.evidence.repository.BlockchainEventRepository;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import cn.edu.gfkd.evidence.repository.SyncStatusRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class EvidenceSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(EvidenceSyncService.class);
    
    @Autowired
    private EvidenceRepository evidenceRepository;
    
    @Autowired
    private BlockchainEventRepository blockchainEventRepository;
    
    @Autowired
    private SyncStatusRepository syncStatusRepository;
    
    @Autowired
    private BlockchainEventListener blockchainEventListener;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @EventListener
    @Async
    @Transactional
    public void handleBlockchainEvent(BlockchainEventReceived event) {
        try {
            logger.info("Processing blockchain event: {} for evidenceId: {}", 
                event.getEventName(), event.getEvidenceId());
            
            switch (event.getEventName()) {
                case "EvidenceSubmitted":
                    processEvidenceSubmitted(event);
                    break;
                case "EvidenceStatusChanged":
                    processEvidenceStatusChanged(event);
                    break;
                case "EvidenceVerified":
                    processEvidenceVerified(event);
                    break;
                case "EvidenceRevoked":
                    processEvidenceRevoked(event);
                    break;
                default:
                    logger.warn("Unknown event type: {}", event.getEventName());
            }
            
            // Mark blockchain event as processed
            markEventAsProcessed(event.getTransactionHash(), event.getLogIndex());
            
            // Update sync status
            updateSyncStatus(event.getBlockNumber());
            
        } catch (Exception e) {
            logger.error("Failed to process blockchain event for evidenceId: {}", 
                event.getEvidenceId(), e);
            throw new RuntimeException("Failed to process blockchain event", e);
        }
    }
    
    private void processEvidenceSubmitted(BlockchainEventReceived event) {
        // Check if evidence already exists
        if (evidenceRepository.existsByEvidenceId(event.getEvidenceId())) {
            logger.info("Evidence {} already exists, skipping", event.getEvidenceId());
            return;
        }
        
        // For now, we only have basic info from the event
        // In a real implementation, we might need to fetch additional details from the contract
        Evidence evidence = new Evidence(
            event.getEvidenceId(),
            event.getUserAddress(),
            "", // fileName - would need to be fetched from contract
            "", // mimeType - would need to be fetched from contract
            0L, // fileSize - would need to be fetched from contract
            BigInteger.ZERO, // fileCreationTime - would need to be fetched from contract
            "SHA256", // hashAlgorithm - default assumption
            event.getHashValue(),
            event.getBlockNumber(),
            event.getTransactionHash(),
            event.getBlockTimestamp(),
            "" // memo - would need to be fetched from contract
        );
        
        evidence.setStatus("effective");
        
        evidenceRepository.save(evidence);
        
        logger.info("Created new evidence record for evidenceId: {}", event.getEvidenceId());
    }
    
    private void processEvidenceStatusChanged(BlockchainEventReceived event) {
        Evidence evidence = evidenceRepository.findByEvidenceId(event.getEvidenceId())
            .orElseThrow(() -> {
                logger.warn("Evidence not found for status change: {}", event.getEvidenceId());
                return new RuntimeException("Evidence not found: " + event.getEvidenceId());
            });
        
        // Update status
        evidence.setStatus(event.getNewStatus());
        
        // If status is revoked, record additional info
        if ("revoked".equals(event.getNewStatus())) {
            evidence.setRevokedAt(LocalDateTime.now());
            evidence.setRevokerAddress(event.getUserAddress());
        }
        
        evidenceRepository.save(evidence);
        
        logger.info("Updated evidence status from {} to {} for evidenceId: {}", 
            event.getOldStatus(), event.getNewStatus(), event.getEvidenceId());
    }
    
    // EvidenceVerified event is handled but not stored as it's just a verification operation
    private void processEvidenceVerified(BlockchainEventReceived event) {
        logger.info("Evidence verified for evidenceId: {}, isValid: {}", 
            event.getEvidenceId(), event.isValid());
        // No need to store verification status as it's just a verification operation
    }
    
    private void processEvidenceRevoked(BlockchainEventReceived event) {
        Evidence evidence = evidenceRepository.findByEvidenceId(event.getEvidenceId())
            .orElseThrow(() -> {
                logger.warn("Evidence not found for revocation: {}", event.getEvidenceId());
                return new RuntimeException("Evidence not found: " + event.getEvidenceId());
            });
        
        // Update revocation info
        evidence.setStatus("revoked");
        evidence.setRevokedAt(LocalDateTime.now());
        evidence.setRevokerAddress(event.getUserAddress());
        
        evidenceRepository.save(evidence);
        
        logger.info("Revoked evidence for evidenceId: {}", event.getEvidenceId());
    }
    
    @Transactional
    protected void markEventAsProcessed(String transactionHash, BigInteger logIndex) {
        List<BlockchainEvent> events = blockchainEventRepository.findByTransactionHash(transactionHash);
        
        for (BlockchainEvent event : events) {
            if (event.getLogIndex().equals(logIndex)) {
                event.setIsProcessed(true);
                event.setProcessedAt(LocalDateTime.now());
                blockchainEventRepository.save(event);
                break;
            }
        }
    }
    
    @Transactional
    protected void updateSyncStatus(BigInteger blockNumber) {
        try {
            String contractAddress = blockchainEventListener.getContractAddress();
            SyncStatus syncStatus = syncStatusRepository.findById(contractAddress)
                .orElseGet(() -> new SyncStatus(contractAddress, BigInteger.ZERO));
            
            // Only update if this is a newer block
            if (blockNumber.compareTo(syncStatus.getLastBlockNumber()) > 0) {
                syncStatus.setLastBlockNumber(blockNumber);
                syncStatus.setLastSyncTimestamp(LocalDateTime.now());
                syncStatus.setSyncStatus("SYNCED");
                syncStatus.setErrorMessage(null);
                syncStatus.setRetryCount(0);
                syncStatusRepository.save(syncStatus);
            }
            
        } catch (Exception e) {
            logger.error("Failed to update sync status for block: {}", blockNumber, e);
        }
    }
    
    @Transactional
    public void reprocessUnprocessedEvents() {
        logger.info("Reprocessing unprocessed blockchain events...");
        
        List<BlockchainEvent> unprocessedEvents = blockchainEventRepository.findUnprocessedEvents(
            org.springframework.data.domain.PageRequest.of(0, 100)
        );
        
        for (BlockchainEvent event : unprocessedEvents) {
            try {
                // Parse event data and recreate BlockchainEventReceived
                BlockchainEventReceived receivedEvent = parseEventData(event);
                if (receivedEvent != null) {
                    handleBlockchainEvent(receivedEvent);
                }
            } catch (Exception e) {
                logger.error("Failed to reprocess event with transaction hash: {}", 
                    event.getTransactionHash(), e);
            }
        }
        
        logger.info("Completed reprocessing {} unprocessed events", unprocessedEvents.size());
    }
    
    private BlockchainEventReceived parseEventData(BlockchainEvent event) {
        try {
            // This is a simplified version - in production, you'd want proper JSON parsing
            // based on the event type
            switch (event.getEventName()) {
                case "EvidenceSubmitted":
                    return new BlockchainEventReceived(
                        event.getEventName(),
                        event.getEvidenceId(),
                        "", // userAddress - would be parsed from eventData
                        "", // hashValue - would be parsed from eventData
                        null,
                        null,
                        event.getBlockNumber(),
                        event.getTransactionHash(),
                        event.getLogIndex(),
                        event.getBlockTimestamp(),
                        true
                    );
                // Add other event types as needed
                default:
                    return null;
            }
        } catch (Exception e) {
            logger.error("Failed to parse event data for transaction: {}", 
                event.getTransactionHash(), e);
            return null;
        }
    }
    
    @Transactional
    public void cleanupOldEvents() {
        try {
            // Get the maximum processed block number from evidence table
            BigInteger maxBlockNumber = evidenceRepository.findMaxBlockNumber();
            
            if (maxBlockNumber != null && maxBlockNumber.compareTo(BigInteger.valueOf(1000)) > 0) {
                // Delete blockchain events older than 1000 blocks before the max evidence block
                BigInteger cutoffBlock = maxBlockNumber.subtract(BigInteger.valueOf(1000));
                blockchainEventRepository.deleteByBlockNumberLessThan(cutoffBlock);
                
                logger.info("Cleaned up old blockchain events before block {}", cutoffBlock);
            }
        } catch (Exception e) {
            logger.error("Failed to cleanup old events", e);
        }
    }
}