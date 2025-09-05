package cn.edu.gfkd.evidence.service.processor;

import java.io.IOException;
import java.math.BigInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;
import cn.edu.gfkd.evidence.entity.EvidenceEntity;
import cn.edu.gfkd.evidence.enums.EvidenceEventType;
import cn.edu.gfkd.evidence.exception.EventProcessingException;
import cn.edu.gfkd.evidence.generated.EvidenceStorageContract;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import cn.edu.gfkd.evidence.service.storage.EventStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 证据提交事件处理器
 * 
 * 主要职责： 1. 处理EvidenceSubmitted事件 2. 从智能合约获取完整的证据数据 3. 创建或更新证据记录 4. 确保证据数据的一致性
 */
@Service @RequiredArgsConstructor @Slf4j
public class EvidenceSubmittedProcessor implements BlockchainEventProcessor {

    private final EvidenceRepository evidenceRepository;
    private final EventStorageService eventStorageService;
    private final EvidenceStorageContract evidenceStorageContract;
    private final ObjectMapper objectMapper;

    @Override @Transactional
    public void processEvent(BlockchainEvent event) throws EventProcessingException {
        log.debug("Processing EvidenceSubmitted event: id={}, txHash={}", event.getId(),
                event.getTransactionHash());

        try {
            // 解析事件数据
            EvidenceSubmittedEventData eventData = parseEventData(event);
            log.debug("Parsed EvidenceSubmitted event data: evidenceId={}", eventData.evidenceId);

            // 检查证据是否已存在
            if (evidenceRepository.existsByEvidenceId(eventData.evidenceId)) {
                log.info("Evidence {} already exists, skipping", eventData.evidenceId);
                return;
            }

            // 从智能合约获取完整的证据数据
            EvidenceEntity evidence = getCompleteEvidenceFromContract(eventData.evidenceId);
            if (evidence == null) {
                // 如果合约调用失败，不存储该证据，留着后期再处理
                String errorMsg = "Failed to retrieve evidence from contract: "
                        + eventData.evidenceId;
                log.warn(errorMsg);
                throw new EventProcessingException(errorMsg, event.getEventName(),
                        event.getId().toString(), true);
            }

            // 设置区块链特定字段
            evidence.setTransactionHash(event.getTransactionHash());
            evidence.setStatus("effective");

            // 保存证据记录
            EvidenceEntity savedEvidence = evidenceRepository.save(evidence);

            log.info(
                    "Successfully created new evidence record for evidenceId: {} with id: {} from contract",
                    eventData.evidenceId, savedEvidence.getId());

        } catch (EventProcessingException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = "Failed to process EvidenceSubmitted event: " + e.getMessage();
            log.error(errorMsg, e);
            throw new EventProcessingException(errorMsg, event.getEventName(),
                    event.getId().toString(), e, true);
        }
    }

    @Override
    public String getSupportedEventType() {
        return EvidenceEventType.EVIDENCE_SUBMITTED.name();
    }

    @Override
    public int getPriority() {
        return 1; // 高优先级
    }

    @Override
    public boolean canProcess(BlockchainEvent event) {
        return getSupportedEventType().equals(event.getEventName());
    }

    @Override
    public String getProcessorDescription() {
        return "EvidenceSubmitted事件处理器 - 负责处理新证据提交事件，从智能合约获取完整证据数据并创建证据记录";
    }

    /**
     * 解析事件数据
     */
    private EvidenceSubmittedEventData parseEventData(BlockchainEvent event) throws IOException {
        try {
            JsonNode jsonNode = objectMapper.readTree(event.getRawData());

            // 尝试从原始数据中解析evidenceId
            String evidenceId = extractEvidenceIdFromJson(jsonNode);

            if (evidenceId == null || evidenceId.isEmpty()) {
                throw new IOException("Cannot extract evidenceId from event data");
            }

            return new EvidenceSubmittedEventData(evidenceId, event.getTransactionHash());

        } catch (IOException e) {
            log.error("Failed to parse EvidenceSubmitted event data: {}", e.getMessage(), e);
            throw new IOException("Failed to parse event data: " + e.getMessage(), e);
        }
    }

    /**
     * 从JSON数据中提取evidenceId
     */
    private String extractEvidenceIdFromJson(JsonNode jsonNode) {
        // 尝试多种可能的字段路径
        String[] possiblePaths = { "evidenceId", "evidence_id", "evidenceID", "params.evidenceId",
                "params.evidence_id", "returnValues.evidenceId", "returnValues.evidence_id",
                "args.evidenceId", "args.evidence_id" };

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
     * 从智能合约获取完整的证据数据
     */
    private EvidenceEntity getCompleteEvidenceFromContract(String evidenceId) {
        log.debug("Retrieving complete evidence from contract for evidenceId: {}", evidenceId);

        try {
            EvidenceStorageContract.Evidence contractEvidence = evidenceStorageContract
                    .getEvidence(evidenceId).send();

            if (contractEvidence != null && contractEvidence.exists) {
                EvidenceEntity evidence = new EvidenceEntity(
                        contractEvidence.evidenceId != null ? contractEvidence.evidenceId : "",
                        contractEvidence.userId != null ? contractEvidence.userId : "",
                        contractEvidence.metadata != null ? contractEvidence.metadata.fileName : "",
                        contractEvidence.metadata != null ? contractEvidence.metadata.mimeType : "",
                        contractEvidence.metadata != null
                                ? contractEvidence.metadata.size.longValue()
                                : 0L,
                        contractEvidence.metadata != null ? contractEvidence.metadata.size
                                : BigInteger.ZERO,
                        contractEvidence.hash != null ? contractEvidence.hash.algorithm : "SHA256",
                        contractEvidence.hash != null
                                ? org.web3j.utils.Numeric.toHexString(contractEvidence.hash.value)
                                : "",
                        contractEvidence.blockHeight != null ? contractEvidence.blockHeight
                                : BigInteger.ZERO,
                        "", // transactionHash will be set by caller
                        contractEvidence.timestamp != null ? contractEvidence.timestamp
                                : BigInteger.ZERO,
                        contractEvidence.memo != null ? contractEvidence.memo : "");

                log.debug("Successfully retrieved evidence from contract: evidenceId={}",
                        evidenceId);
                return evidence;
            } else {
                log.warn("Evidence not found in contract or does not exist: evidenceId={}",
                        evidenceId);
                return null;
            }

        } catch (Exception e) {
            log.error("Failed to retrieve evidence {} from smart contract: {}", evidenceId,
                    e.getMessage(), e);
            return null;
        }
    }

    /**
     * 证据提交事件数据内部类
     */
    private static class EvidenceSubmittedEventData {
        final String evidenceId;
        final String transactionHash;

        EvidenceSubmittedEventData(String evidenceId, String transactionHash) {
            this.evidenceId = evidenceId;
            this.transactionHash = transactionHash;
        }
    }
}