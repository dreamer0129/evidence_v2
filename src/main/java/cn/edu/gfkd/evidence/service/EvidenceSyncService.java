package cn.edu.gfkd.evidence.service;

import java.math.BigInteger;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;
import cn.edu.gfkd.evidence.exception.SyncException;
import cn.edu.gfkd.evidence.generated.EvidenceStorageContract;
import cn.edu.gfkd.evidence.repository.BlockchainEventRepository;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import cn.edu.gfkd.evidence.repository.SyncStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service @RequiredArgsConstructor @Slf4j
public class EvidenceSyncService {

    private final EvidenceRepository evidenceRepository;
    private final BlockchainEventRepository blockchainEventRepository;
    private final SyncStatusRepository syncStatusRepository;
    private final EvidenceEventListener blockchainEventListener;
    private final ObjectMapper objectMapper;
    private final EvidenceStorageContract evidenceStorageContract;

    /**
     * 重新处理未处理的事件 - 用于手动触发重试
     */
    public void reprocessUnprocessedEvents() {
        log.info("Reprocessing unprocessed blockchain events...");

        List<BlockchainEvent> unprocessedEvents = blockchainEventRepository
                .findUnprocessedEvents(PageRequest.of(0, 100));

        for (BlockchainEvent event : unprocessedEvents) {
            try {
                log.info("Reprocessing event: {} for evidenceId: {}", event.getEventName(),
                        parseEvidenceIdFromRawData(event.getRawData()));
                // 直接调用 EvidenceEventListener 的同步处理方法
                blockchainEventListener.syncPastEvents(event.getBlockNumber(),
                        event.getBlockNumber());
            } catch (Exception e) {
                log.error("Failed to reprocess event with transaction hash: {}",
                        event.getTransactionHash(), e);
            }
        }

        log.info("Completed reprocessing {} unprocessed events", unprocessedEvents.size());
    }

    /**
     * 清理旧的事件数据
     */
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

    /**
     * 从原始数据中解析证据ID
     */
    private String parseEvidenceIdFromRawData(String rawData) {
        try {
            JsonNode rootNode = objectMapper.readTree(rawData);
            if (rootNode.has("evidenceId")) {
                return rootNode.get("evidenceId").asText();
            }
        } catch (Exception e) {
            log.debug("Failed to parse evidenceId from raw data: {}", e.getMessage());
        }
        return "unknown";
    }
}