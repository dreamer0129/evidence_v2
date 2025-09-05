package cn.edu.gfkd.evidence.service.processor;

import java.io.IOException;
import java.time.LocalDateTime;

import cn.edu.gfkd.evidence.exception.EventProcessingException;
import cn.edu.gfkd.evidence.service.storage.EventStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;
import cn.edu.gfkd.evidence.entity.EvidenceEntity;
import cn.edu.gfkd.evidence.exception.EvidenceNotFoundException;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 证据撤销事件处理器
 * 
 * 主要职责：
 * 1. 处理EvidenceRevoked事件
 * 2. 将证据状态设置为已撤销
 * 3. 记录撤销时间和撤销者信息
 * 4. 确保撤销操作的准确性
 */
@Service @RequiredArgsConstructor @Slf4j
public class EvidenceRevokedProcessor implements BlockchainEventProcessor {

    private final EvidenceRepository evidenceRepository;
    private final EventStorageService eventStorageService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void processEvent(BlockchainEvent event) throws EventProcessingException {
        log.debug("Processing EvidenceRevoked event: id={}, txHash={}", 
                event.getId(), event.getTransactionHash());

        try {
            // 解析事件数据
            EvidenceRevokedEventData eventData = parseEventData(event);
            log.debug("Parsed EvidenceRevoked event data: evidenceId={}, revoker={}", 
                    eventData.evidenceId, eventData.revoker);

            // 查找证据记录
            EvidenceEntity evidence = evidenceRepository.findByEvidenceId(eventData.evidenceId)
                    .orElseThrow(() -> new EvidenceNotFoundException("Evidence not found: " + eventData.evidenceId));

            // 检查证据当前状态
            String previousStatus = evidence.getStatus();
            if ("revoked".equals(previousStatus)) {
                log.info("Evidence {} is already revoked, skipping", eventData.evidenceId);
                return;
            }

            // 更新证据状态为已撤销
            evidence.setStatus("revoked");
            evidence.setRevokedAt(LocalDateTime.now());
            evidence.setRevokerAddress(eventData.revoker);

            // 保存更新
            EvidenceEntity updatedEvidence = evidenceRepository.save(evidence);

            log.info("Successfully revoked evidence for evidenceId: {} by revoker: {}, previous status: {}", 
                    eventData.evidenceId, eventData.revoker, previousStatus);

        } catch (EvidenceNotFoundException e) {
            // 证据不存在，这种情况不应该重试
            String errorMsg = "Evidence not found for revocation: " + e.getMessage();
            log.warn(errorMsg);
            throw new EventProcessingException(errorMsg, 
                    event.getEventName(), event.getId().toString(), false);
        } catch (EventProcessingException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = "Failed to process EvidenceRevoked event: " + e.getMessage();
            log.error(errorMsg, e);
            throw new EventProcessingException(errorMsg, 
                    event.getEventName(), event.getId().toString(), e, true);
        }
    }

    @Override
    public String getSupportedEventType() {
        return "EvidenceRevoked";
    }

    @Override
    public int getPriority() {
        return 3; // 较低优先级，因为撤销操作通常不紧急
    }

    @Override
    public boolean canProcess(BlockchainEvent event) {
        return "EvidenceRevoked".equals(event.getEventName());
    }

    @Override
    public String getProcessorDescription() {
        return "EvidenceRevoked事件处理器 - 负责处理证据撤销事件，将证据状态设置为已撤销并记录撤销信息";
    }


    /**
     * 解析事件数据
     */
    private EvidenceRevokedEventData parseEventData(BlockchainEvent event) throws IOException {
        try {
            JsonNode jsonNode = objectMapper.readTree(event.getRawData());
            
            // 提取事件数据
            String evidenceId = extractFieldFromJson(jsonNode, "evidenceId");
            String revoker = extractFieldFromJson(jsonNode, "revoker");

            if (evidenceId == null || evidenceId.isEmpty()) {
                throw new IOException("Cannot extract evidenceId from event data");
            }
            
            if (revoker == null || revoker.isEmpty()) {
                throw new IOException("Cannot extract revoker from event data");
            }

            // 验证撤销者地址格式
            validateRevokerAddress(revoker);

            return new EvidenceRevokedEventData(evidenceId, revoker, event.getTransactionHash());
            
        } catch (IOException e) {
            log.error("Failed to parse EvidenceRevoked event data: {}", e.getMessage(), e);
            throw new IOException("Failed to parse event data: " + e.getMessage(), e);
        }
    }

    /**
     * 从JSON数据中提取字段
     */
    private String extractFieldFromJson(JsonNode jsonNode, String fieldName) {
        // 尝试多种可能的字段路径
        String[] possiblePaths = {
            fieldName,
            fieldName.toLowerCase(),
            fieldName.toUpperCase(),
            "params." + fieldName,
            "returnValues." + fieldName,
            "args." + fieldName
        };

        for (String path : possiblePaths) {
            try {
                JsonNode node = jsonNode;
                String[] parts = path.split("\\.");
                
                for (String part : parts) {
                    if (node.has(part)) {
                        node = node.get(part);
                    } else {
                        node = null;
                        break;
                    }
                }
                
                if (node != null && node.isTextual()) {
                    return node.asText();
                }
            } catch (Exception e) {
                // 继续尝试下一个路径
            }
        }

        return null;
    }

    /**
     * 验证撤销者地址格式
     */
    private void validateRevokerAddress(String revoker) throws IOException {
        if (revoker == null || revoker.isEmpty()) {
            throw new IOException("Revoker address cannot be null or empty");
        }
        
        // 基本的以太坊地址格式验证
        if (!revoker.matches("^0x[a-fA-F0-9]{40}$")) {
            throw new IOException("Invalid revoker address format: " + revoker);
        }
        
        // 检查是否是零地址（通常不允许零地址撤销）
        if ("0x0000000000000000000000000000000000000000".equals(revoker.toLowerCase())) {
            throw new IOException("Revoker address cannot be zero address");
        }
    }

    /**
     * 证据撤销事件数据内部类
     */
    private static class EvidenceRevokedEventData {
        final String evidenceId;
        final String revoker;
        final String transactionHash;

        EvidenceRevokedEventData(String evidenceId, String revoker, String transactionHash) {
            this.evidenceId = evidenceId;
            this.revoker = revoker;
            this.transactionHash = transactionHash;
        }
    }
}