package cn.edu.gfkd.evidence.service.processor;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;
import cn.edu.gfkd.evidence.entity.EvidenceEntity;
import cn.edu.gfkd.evidence.enums.EvidenceEventType;
import cn.edu.gfkd.evidence.exception.EventProcessingException;
import cn.edu.gfkd.evidence.exception.EvidenceNotFoundException;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import cn.edu.gfkd.evidence.service.storage.EventStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 证据状态变更事件处理器
 * 
 * 主要职责： 1. 处理EvidenceStatusChanged事件 2. 更新证据记录的状态 3. 处理撤销相关的逻辑 4. 确保状态变更的准确性
 */
@Service @RequiredArgsConstructor @Slf4j
public class EvidenceStatusChangedProcessor implements BlockchainEventProcessor {

    private final EvidenceRepository evidenceRepository;
    private final EventStorageService eventStorageService;
    private final ObjectMapper objectMapper;

    @Override @Transactional
    public void processEvent(BlockchainEvent event) throws EventProcessingException {
        log.debug("Processing EvidenceStatusChanged event: id={}, txHash={}", event.getId(),
                event.getTransactionHash());

        try {
            // 解析事件数据
            EvidenceStatusChangedEventData eventData = parseEventData(event);
            log.debug(
                    "Parsed EvidenceStatusChanged event data: evidenceId={}, oldStatus={}, newStatus={}",
                    eventData.evidenceId, eventData.oldStatus, eventData.newStatus);

            // 查找证据记录
            EvidenceEntity evidence = evidenceRepository.findByEvidenceId(eventData.evidenceId)
                    .orElseThrow(() -> new EvidenceNotFoundException(
                            "Evidence not found: " + eventData.evidenceId));

            // 记录状态变更前的信息
            String previousStatus = evidence.getStatus();

            // 更新证据状态
            evidence.setStatus(eventData.newStatus);

            // 如果状态变更为revoked，设置撤销相关信息
            if ("revoked".equals(eventData.newStatus) && eventData.user != null) {
                evidence.setRevokedAt(LocalDateTime.now());
                evidence.setRevokerAddress(eventData.user);
                log.debug("Evidence revoked by user: {}", eventData.user);
            }

            // 保存更新
            EvidenceEntity updatedEvidence = evidenceRepository.save(evidence);

            log.info("Successfully updated evidence status from {} to {} for evidenceId: {}",
                    previousStatus, eventData.newStatus, eventData.evidenceId);

        } catch (EvidenceNotFoundException e) {
            // 证据不存在，这种情况不应该重试
            String errorMsg = "Evidence not found for status change: " + e.getMessage();
            log.warn(errorMsg);
            throw new EventProcessingException(errorMsg, event.getEventName(),
                    event.getId().toString(), false);
        } catch (EventProcessingException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = "Failed to process EvidenceStatusChanged event: " + e.getMessage();
            log.error(errorMsg, e);
            throw new EventProcessingException(errorMsg, event.getEventName(),
                    event.getId().toString(), e, true);
        }
    }

    @Override
    public String getSupportedEventType() {
        return EvidenceEventType.EVIDENCE_STATUS_CHANGED.name();
    }

    @Override
    public int getPriority() {
        return 2; // 中等优先级
    }

    @Override
    public boolean canProcess(BlockchainEvent event) {
        return getSupportedEventType().equals(event.getEventName());
    }

    @Override
    public String getProcessorDescription() {
        return "EvidenceStatusChanged事件处理器 - 负责处理证据状态变更事件，更新证据记录的状态信息";
    }

    /**
     * 解析事件数据
     */
    private EvidenceStatusChangedEventData parseEventData(BlockchainEvent event)
            throws IOException {
        try {
            JsonNode jsonNode = objectMapper.readTree(event.getRawData());

            // 提取事件数据
            String evidenceId = extractFieldFromJson(jsonNode, "evidenceId");
            String oldStatus = extractFieldFromJson(jsonNode, "oldStatus");
            String newStatus = extractFieldFromJson(jsonNode, "newStatus");
            String user = extractFieldFromJson(jsonNode, "user"); // 可选字段

            if (evidenceId == null || evidenceId.isEmpty()) {
                throw new IOException("Cannot extract evidenceId from event data");
            }

            if (oldStatus == null || oldStatus.isEmpty()) {
                throw new IOException("Cannot extract oldStatus from event data");
            }

            if (newStatus == null || newStatus.isEmpty()) {
                throw new IOException("Cannot extract newStatus from event data");
            }

            // 验证状态值的合法性
            validateStatusValues(oldStatus, newStatus);

            return new EvidenceStatusChangedEventData(evidenceId, oldStatus, newStatus, user,
                    event.getTransactionHash());

        } catch (IOException e) {
            log.error("Failed to parse EvidenceStatusChanged event data: {}", e.getMessage(), e);
            throw new IOException("Failed to parse event data: " + e.getMessage(), e);
        }
    }

    /**
     * 从JSON数据中提取字段
     */
    private String extractFieldFromJson(JsonNode jsonNode, String fieldName) {
        // 尝试多种可能的字段路径
        String[] possiblePaths = { fieldName, fieldName.toLowerCase(), fieldName.toUpperCase(),
                "params." + fieldName, "returnValues." + fieldName, "args." + fieldName };

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
     * 验证状态值的合法性
     */
    private void validateStatusValues(String oldStatus, String newStatus) throws IOException {
        // 定义合法的状态值
        String[] validStatuses = { "effective", "revoked", "pending", "rejected", "expired" };

        boolean oldStatusValid = false;
        boolean newStatusValid = false;

        for (String status : validStatuses) {
            if (status.equals(oldStatus)) {
                oldStatusValid = true;
            }
            if (status.equals(newStatus)) {
                newStatusValid = true;
            }
        }

        if (!oldStatusValid) {
            throw new IOException("Invalid oldStatus value: " + oldStatus);
        }

        if (!newStatusValid) {
            throw new IOException("Invalid newStatus value: " + newStatus);
        }

        // 检查状态变更是否合理
        if (oldStatus.equals(newStatus)) {
            throw new IOException("Status change with same old and new value: " + oldStatus);
        }

        // 特殊状态变更逻辑检查
        if ("revoked".equals(newStatus) && "revoked".equals(oldStatus)) {
            throw new IOException("Cannot revoke already revoked evidence");
        }
    }

    /**
     * 证据状态变更事件数据内部类
     */
    private static class EvidenceStatusChangedEventData {
        final String evidenceId;
        final String oldStatus;
        final String newStatus;
        final String user; // 可选字段，执行状态变更的用户
        final String transactionHash;

        EvidenceStatusChangedEventData(String evidenceId, String oldStatus, String newStatus,
                String user, String transactionHash) {
            this.evidenceId = evidenceId;
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
            this.user = user;
            this.transactionHash = transactionHash;
        }
    }
}