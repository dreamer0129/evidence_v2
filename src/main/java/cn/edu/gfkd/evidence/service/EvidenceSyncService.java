package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;
import cn.edu.gfkd.evidence.entity.Evidence;
import cn.edu.gfkd.evidence.entity.SyncStatus;
import cn.edu.gfkd.evidence.event.BlockchainEventReceived;
import cn.edu.gfkd.evidence.exception.EvidenceNotFoundException;
import cn.edu.gfkd.evidence.exception.SyncException;
import cn.edu.gfkd.evidence.repository.BlockchainEventRepository;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import cn.edu.gfkd.evidence.repository.SyncStatusRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EvidenceSyncService {

    private final EvidenceRepository evidenceRepository;
    private final BlockchainEventRepository blockchainEventRepository;
    private final SyncStatusRepository syncStatusRepository;
    private final EvidenceEventListener blockchainEventListener;
    private final ObjectMapper objectMapper;

    private void logEventProcessing(BlockchainEventReceived event, String action) {
        log.info("{} blockchain event: {} for evidenceId: {}", 
            action, event.getEventName(), event.getParameters().get("evidenceId"));
    }

    @EventListener
    @Async
    public void handleBlockchainEvent(BlockchainEventReceived event) {
        try {
            logEventProcessing(event, "Processing");
            
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
                    log.warn("Unknown event type: {}", event.getEventName());
            }
            
            markEventAsProcessed(event.getTransactionHash(), event.getLogIndex());
            updateSyncStatus(event.getBlockNumber());
            
        } catch (Exception e) {
            Object evidenceId = event.getParameters().get("evidenceId");
            log.error("Failed to process blockchain event for evidenceId: {}", 
                evidenceId != null ? evidenceId : "unknown", e);
            throw new SyncException("Failed to process blockchain event", e);
        }
    }

    private void processEvidenceSubmitted(BlockchainEventReceived event) {
        validateEventInput(event);
        
        String evidenceId = (String) event.getParameters().get("evidenceId");
        
        if (evidenceRepository.existsByEvidenceId(evidenceId)) {
            log.info("Evidence {} already exists, skipping", evidenceId);
            return;
        }
        
        Evidence evidence = createEvidenceFromEvent(event);
        evidence.setStatus("effective");
        
        evidenceRepository.save(evidence);
        log.info("Created new evidence record for evidenceId: {}", evidenceId);
    }

    private Evidence createEvidenceFromEvent(BlockchainEventReceived event) {
        return new Evidence(
            (String) event.getParameters().get("evidenceId"),
            (String) event.getParameters().get("user"),
            "", // fileName - would need to be fetched from contract
            "", // mimeType - would need to be fetched from contract
            0L, // fileSize - would need to be fetched from contract
            (BigInteger) event.getParameters().getOrDefault("timestamp", BigInteger.ZERO),
            "SHA256", // hashAlgorithm - default assumption
            (String) event.getParameters().get("hashValue"),
            event.getBlockNumber(),
            event.getTransactionHash(),
            event.getBlockTimestamp(),
            "" // memo - would need to be fetched from contract
        );
    }

    private void validateEventInput(BlockchainEventReceived event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (!StringUtils.hasText((String) event.getParameters().get("evidenceId"))) {
            throw new IllegalArgumentException("Evidence ID cannot be empty");
        }
        if (!StringUtils.hasText((String) event.getParameters().get("user"))) {
            throw new IllegalArgumentException("User address cannot be empty");
        }
        if (!StringUtils.hasText((String) event.getParameters().get("hashValue"))) {
            throw new IllegalArgumentException("Hash value cannot be empty");
        }
    }

    private void processEvidenceStatusChanged(BlockchainEventReceived event) {
        String evidenceId = (String) event.getParameters().get("evidenceId");
        String newStatus = (String) event.getParameters().get("newStatus");
        String oldStatus = (String) event.getParameters().get("oldStatus");
        
        Evidence evidence = evidenceRepository.findByEvidenceId(evidenceId)
            .orElseThrow(() -> new EvidenceNotFoundException("Evidence not found: " + evidenceId));
        
        evidence.setStatus(newStatus);
        
        if ("revoked".equals(newStatus)) {
            evidence.setRevokedAt(LocalDateTime.now());
            evidence.setRevokerAddress((String) event.getParameters().get("user"));
        }
        
        evidenceRepository.save(evidence);
        log.info("Updated evidence status from {} to {} for evidenceId: {}", 
            oldStatus, newStatus, evidenceId);
    }

    private void processEvidenceVerified(BlockchainEventReceived event) {
        String evidenceId = (String) event.getParameters().get("evidenceId");
        Boolean isValid = (Boolean) event.getParameters().get("isValid");
        
        log.info("Evidence verified for evidenceId: {}, isValid: {}", 
            evidenceId, isValid != null ? isValid : "unknown");
        // No need to store verification status as it's just a verification operation
    }

    private void processEvidenceRevoked(BlockchainEventReceived event) {
        String evidenceId = (String) event.getParameters().get("evidenceId");
        String userAddress = (String) event.getParameters().get("user");
        
        Evidence evidence = evidenceRepository.findByEvidenceId(evidenceId)
            .orElseThrow(() -> new EvidenceNotFoundException("Evidence not found: " + evidenceId));
        
        evidence.setStatus("revoked");
        evidence.setRevokedAt(LocalDateTime.now());
        evidence.setRevokerAddress(userAddress);
        
        evidenceRepository.save(evidence);
        log.info("Revoked evidence for evidenceId: {}", evidenceId);
    }

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

    protected void updateSyncStatus(BigInteger blockNumber) {
        try {
            String contractAddress = blockchainEventListener.getContractAddress();
            SyncStatus syncStatus = syncStatusRepository.findById(contractAddress)
                .orElseGet(() -> new SyncStatus(contractAddress, BigInteger.ZERO));
            
            if (blockNumber.compareTo(syncStatus.getLastBlockNumber()) > 0) {
                syncStatus.setLastBlockNumber(blockNumber);
                syncStatus.setLastSyncTimestamp(LocalDateTime.now());
                syncStatus.setSyncStatus("SYNCED");
                syncStatus.setErrorMessage(null);
                syncStatus.setRetryCount(0);
                syncStatusRepository.save(syncStatus);
            }
            
        } catch (Exception e) {
            log.error("Failed to update sync status for block: {}", blockNumber, e);
            throw new SyncException("Failed to update sync status", e);
        }
    }

    public void reprocessUnprocessedEvents() {
        log.info("Reprocessing unprocessed blockchain events...");
        
        List<BlockchainEvent> unprocessedEvents = blockchainEventRepository.findUnprocessedEvents(
            PageRequest.of(0, 100)
        );
        
        for (BlockchainEvent event : unprocessedEvents) {
            try {
                BlockchainEventReceived receivedEvent = parseEventData(event);
                if (receivedEvent != null) {
                    handleBlockchainEvent(receivedEvent);
                }
            } catch (Exception e) {
                log.error("Failed to reprocess event with transaction hash: {}", 
                    event.getTransactionHash(), e);
            }
        }
        
        log.info("Completed reprocessing {} unprocessed events", unprocessedEvents.size());
    }

    public void cleanupOldEvents() {
        try {
            BigInteger maxBlockNumber = evidenceRepository.findMaxBlockNumber();
            
            if (maxBlockNumber != null && maxBlockNumber.compareTo(BigInteger.valueOf(1000)) > 0) {
                BigInteger cutoffBlock = maxBlockNumber.subtract(BigInteger.valueOf(1000));
                blockchainEventRepository.deleteByBlockNumberLessThan(cutoffBlock);
                log.info("Cleaned up old blockchain events before block {}", cutoffBlock);
            }
        } catch (Exception e) {
            log.error("Failed to cleanup old events", e);
            throw new SyncException("Failed to cleanup old events", e);
        }
    }

    private BlockchainEventReceived parseEventData(BlockchainEvent event) {
        try {
            Map<String, Object> parameters = parseEventParameters(event);
            
            BlockchainEventReceived.Builder builder = BlockchainEventReceived.builder()
                .contractAddress(event.getContractAddress())
                .eventName(event.getEventName())
                .blockNumber(event.getBlockNumber())
                .transactionHash(event.getTransactionHash())
                .logIndex(event.getLogIndex())
                .blockTimestamp(event.getBlockTimestamp())
                .rawData(event.getRawData());
                
            parameters.forEach(builder::parameter);
            
            return builder.build();
                
        } catch (Exception e) {
            log.error("Failed to parse event data for transaction: {}", event.getTransactionHash(), e);
            return null;
        }
    }

    private Map<String, Object> parseEventParameters(BlockchainEvent event) {
        Map<String, Object> parameters = new HashMap<>();
        
        if (event.getRawData() != null && !event.getRawData().isEmpty()) {
            try {
                JsonNode rootNode = objectMapper.readTree(event.getRawData());
                
                switch (event.getEventName()) {
                    case "EvidenceSubmitted":
                        parseEvidenceSubmittedParameters(rootNode, parameters);
                        break;
                    case "EvidenceStatusChanged":
                        parseEvidenceStatusChangedParameters(rootNode, parameters);
                        break;
                    case "EvidenceVerified":
                        parseEvidenceVerifiedParameters(rootNode, parameters);
                        break;
                    case "EvidenceRevoked":
                        parseEvidenceRevokedParameters(rootNode, parameters);
                        break;
                }
            } catch (Exception e) {
                log.error("Failed to parse event parameters for event: {}", event.getEventName(), e);
            }
        }
        
        return parameters;
    }

    private void parseEvidenceSubmittedParameters(JsonNode rootNode, Map<String, Object> parameters) {
        extractParameter(rootNode, "evidenceId", parameters);
        extractParameter(rootNode, "user", parameters);
        extractParameter(rootNode, "hashValue", parameters);
        
        if (rootNode.has("timestamp")) {
            parameters.put("timestamp", new BigInteger(rootNode.get("timestamp").asText()));
        }
    }

    private void parseEvidenceStatusChangedParameters(JsonNode rootNode, Map<String, Object> parameters) {
        extractParameter(rootNode, "evidenceId", parameters);
        extractParameter(rootNode, "oldStatus", parameters);
        extractParameter(rootNode, "newStatus", parameters);
    }

    private void parseEvidenceVerifiedParameters(JsonNode rootNode, Map<String, Object> parameters) {
        extractParameter(rootNode, "evidenceId", parameters);
        
        if (rootNode.has("isValid")) {
            parameters.put("isValid", rootNode.get("isValid").asBoolean());
        }
    }

    private void parseEvidenceRevokedParameters(JsonNode rootNode, Map<String, Object> parameters) {
        extractParameter(rootNode, "evidenceId", parameters);
    }

    private void extractParameter(JsonNode rootNode, String paramName, Map<String, Object> parameters) {
        if (rootNode.has(paramName)) {
            JsonNode paramNode = rootNode.get(paramName);
            if (paramNode.isArray()) {
                byte[] bytes = new byte[paramNode.size()];
                for (int i = 0; i < paramNode.size(); i++) {
                    bytes[i] = (byte) paramNode.get(i).asInt();
                }
                parameters.put(paramName, bytes);
            } else {
                parameters.put(paramName, paramNode.asText());
            }
        }
    }
}