package cn.edu.gfkd.evidence.integration;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;
import cn.edu.gfkd.evidence.entity.EvidenceEntity;
import cn.edu.gfkd.evidence.entity.SyncStatus;
import cn.edu.gfkd.evidence.event.BlockchainEventReceived;
import cn.edu.gfkd.evidence.repository.BlockchainEventRepository;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import cn.edu.gfkd.evidence.repository.SyncStatusRepository;
import cn.edu.gfkd.evidence.service.EvidenceEventListener;
import cn.edu.gfkd.evidence.service.EvidenceSyncService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EvidenceSyncServiceIntegrationTest {

        @Autowired
        private EvidenceSyncService evidenceSyncService;

        @Autowired
        private EvidenceRepository evidenceRepository;

        @Autowired
        private BlockchainEventRepository blockchainEventRepository;

        @Autowired
        private SyncStatusRepository syncStatusRepository;

        @MockBean
        private EvidenceEventListener evidenceEventListener;

        @Test
        void reprocessUnprocessedEvents_ProcessesEvidenceSubmittedEvent() throws Exception {
                // Given
                when(evidenceEventListener.getContractAddress()).thenReturn("0xContractAddress");

                // Create blockchain event record with raw data that can be parsed
                BlockchainEvent blockchainEvent = new BlockchainEvent(
                                "0xContractAddress",
                                "EvidenceSubmitted",
                                BigInteger.valueOf(100),
                                "0xTransactionHash",
                                BigInteger.valueOf(0),
                                BigInteger.valueOf(1234567890),
                                "{\"evidenceId\":\"EVID:1234567890:CN-001\",\"user\":\"0x1234567890123456789012345678901234567890\",\"hashValue\":\"0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef\",\"timestamp\":\"1234567890\"}");
                blockchainEvent.setIsProcessed(false);
                blockchainEventRepository.save(blockchainEvent);

                // Create sync status
                SyncStatus syncStatus = new SyncStatus("0xContractAddress", BigInteger.valueOf(50));
                syncStatusRepository.save(syncStatus);

                // When
                evidenceSyncService.reprocessUnprocessedEvents();

                // Then
                List<EvidenceEntity> evidenceList = evidenceRepository.findAll();
                assertThat(evidenceList).hasSize(1);
                EvidenceEntity savedEvidence = evidenceList.get(0);
                assertThat(savedEvidence.getEvidenceId()).isEqualTo("EVID:1234567890:CN-001");
                assertThat(savedEvidence.getUserAddress()).isEqualTo("0x1234567890123456789012345678901234567890");
                assertThat(savedEvidence.getHashValue())
                                .isEqualTo("0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
                assertThat(savedEvidence.getStatus()).isEqualTo("effective");

                // Verify blockchain event is marked as processed
                List<BlockchainEvent> blockchainEvents = blockchainEventRepository
                                .findByTransactionHash("0xTransactionHash");
                assertThat(blockchainEvents).hasSize(1);
                assertThat(blockchainEvents.get(0).getIsProcessed()).isTrue();
        }

        @Test
        void handleBlockchainEvent_EvidenceAlreadyExists_DoesNotCreateDuplicate() {
                // Given
                EvidenceEntity existingEvidence = new EvidenceEntity(
                                "EVID:1234567890:CN-001",
                                "0x1234567890123456789012345678901234567890",
                                "test_file.pdf",
                                "application/pdf",
                                1024L,
                                BigInteger.valueOf(1234567890),
                                "SHA256",
                                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                                BigInteger.valueOf(90),
                                "0xOldTransactionHash",
                                BigInteger.valueOf(1234567890),
                                "Test evidence memo");
                evidenceRepository.save(existingEvidence);

                BlockchainEventReceived event = BlockchainEventReceived.builder()
                                .contractAddress("0xContractAddress")
                                .eventName("EvidenceSubmitted")
                                .blockNumber(BigInteger.valueOf(100))
                                .transactionHash("0xTransactionHash")
                                .logIndex(BigInteger.valueOf(0))
                                .blockTimestamp(BigInteger.valueOf(1234567890))
                                .rawData("{\"test\":\"data\"}")
                                .parameter("evidenceId", "EVID:1234567890:CN-001")
                                .parameter("user", "0x1234567890123456789012345678901234567890")
                                .parameter("hashValue",
                                                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef")
                                .parameter("timestamp", BigInteger.valueOf(1234567890))
                                .build();

                // Create sync status
                SyncStatus syncStatus = new SyncStatus("0xContractAddress", BigInteger.valueOf(50));
                syncStatusRepository.save(syncStatus);

                // When
                evidenceSyncService.handleBlockchainEvent(event);

                // Then
                List<EvidenceEntity> evidenceList = evidenceRepository.findAll();
                assertThat(evidenceList).hasSize(1);
                assertThat(evidenceList.get(0).getEvidenceId()).isEqualTo("EVID:1234567890:CN-001");
        }

        @Test
        void reprocessUnprocessedEvents_ProcessesEvidenceStatusChangedEvent() throws Exception {
                // Given
                when(evidenceEventListener.getContractAddress()).thenReturn("0xContractAddress");

                EvidenceEntity existingEvidence = new EvidenceEntity(
                                "EVID:1234567890:CN-001",
                                "0x1234567890123456789012345678901234567890",
                                "test_file.pdf",
                                "application/pdf",
                                1024L,
                                BigInteger.valueOf(1234567890),
                                "SHA256",
                                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                                BigInteger.valueOf(90),
                                "0xOldTransactionHash",
                                BigInteger.valueOf(1234567890),
                                "Test evidence memo");
                existingEvidence.setStatus("pending");
                evidenceRepository.save(existingEvidence);

                // Create blockchain event record with raw data that can be parsed
                BlockchainEvent blockchainEvent = new BlockchainEvent(
                                "0xContractAddress",
                                "EvidenceStatusChanged",
                                BigInteger.valueOf(100),
                                "0xTransactionHash",
                                BigInteger.valueOf(0),
                                BigInteger.valueOf(1234567890),
                                "{\"evidenceId\":\"EVID:1234567890:CN-001\",\"oldStatus\":\"pending\",\"newStatus\":\"verified\"}");
                blockchainEvent.setIsProcessed(false);
                blockchainEventRepository.save(blockchainEvent);

                // Create sync status
                SyncStatus syncStatus = new SyncStatus("0xContractAddress", BigInteger.valueOf(50));
                syncStatusRepository.save(syncStatus);

                // When
                evidenceSyncService.reprocessUnprocessedEvents();

                // Then
                EvidenceEntity updatedEvidence = evidenceRepository.findByEvidenceId("EVID:1234567890:CN-001")
                                .orElse(null);
                assertThat(updatedEvidence).isNotNull();
                assertThat(updatedEvidence.getStatus()).isEqualTo("verified");

                // Verify blockchain event is marked as processed
                List<BlockchainEvent> blockchainEvents = blockchainEventRepository
                                .findByTransactionHash("0xTransactionHash");
                assertThat(blockchainEvents).hasSize(1);
                assertThat(blockchainEvents.get(0).getIsProcessed()).isTrue();
        }

        @Test
        void reprocessUnprocessedEvents_ProcessesEvidenceRevokedEvent() throws Exception {
                // Given
                when(evidenceEventListener.getContractAddress()).thenReturn("0xContractAddress");

                EvidenceEntity existingEvidence = new EvidenceEntity(
                                "EVID:1234567890:CN-001",
                                "0x1234567890123456789012345678901234567890",
                                "test_file.pdf",
                                "application/pdf",
                                1024L,
                                BigInteger.valueOf(1234567890),
                                "SHA256",
                                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                                BigInteger.valueOf(90),
                                "0xOldTransactionHash",
                                BigInteger.valueOf(1234567890),
                                "Test evidence memo");
                existingEvidence.setStatus("effective");
                evidenceRepository.save(existingEvidence);

                // Create blockchain event record with raw data that can be parsed
                BlockchainEvent blockchainEvent = new BlockchainEvent(
                                "0xContractAddress",
                                "EvidenceRevoked",
                                BigInteger.valueOf(100),
                                "0xTransactionHash",
                                BigInteger.valueOf(0),
                                BigInteger.valueOf(1234567890),
                                "{\"evidenceId\":[69,86,73,68,58,49,50,51,52,53,54,55,56,57,48,58,67,78,45,48,48,49],\"revoker\":[48,120,49,50,51,52,53,54,55,56,57,48,49,50,51,52,53,54,55,56,57,48,49,50,51,52,53,54,55,56,57,48,49,50,51,52,53,54,55,56,57,48]}");
                blockchainEvent.setIsProcessed(false);
                blockchainEventRepository.save(blockchainEvent);

                // Create sync status
                SyncStatus syncStatus = new SyncStatus("0xContractAddress", BigInteger.valueOf(50));
                syncStatusRepository.save(syncStatus);

                // When
                evidenceSyncService.reprocessUnprocessedEvents();

                // Then
                EvidenceEntity updatedEvidence = evidenceRepository.findByEvidenceId("EVID:1234567890:CN-001")
                                .orElse(null);
                assertThat(updatedEvidence).isNotNull();
                assertThat(updatedEvidence.getStatus()).isEqualTo("revoked");
                assertThat(updatedEvidence.getRevokerAddress()).isNotNull();
                assertThat(updatedEvidence.getRevokedAt()).isNotNull();

                // Verify blockchain event is marked as processed
                List<BlockchainEvent> blockchainEvents = blockchainEventRepository
                                .findByTransactionHash("0xTransactionHash");
                assertThat(blockchainEvents).hasSize(1);
                assertThat(blockchainEvents.get(0).getIsProcessed()).isTrue();
        }

        @Test
        void cleanupOldEvents_DeletesEventsBeforeCutoffBlock() {
                // Given
                // Create old blockchain event
                BlockchainEvent oldEvent = new BlockchainEvent(
                                "0xContractAddress",
                                "EvidenceSubmitted",
                                BigInteger.valueOf(500),
                                "0xOldTransactionHash",
                                BigInteger.valueOf(0),
                                BigInteger.valueOf(1234567890),
                                "{\"test\":\"data\"}");
                oldEvent.setIsProcessed(true);
                oldEvent.setProcessedAt(LocalDateTime.now());
                blockchainEventRepository.save(oldEvent);

                // Create recent blockchain event
                BlockchainEvent recentEvent = new BlockchainEvent(
                                "0xContractAddress",
                                "EvidenceSubmitted",
                                BigInteger.valueOf(1500),
                                "0xRecentTransactionHash",
                                BigInteger.valueOf(0),
                                BigInteger.valueOf(1234567890),
                                "{\"test\":\"data\"}");
                recentEvent.setIsProcessed(true);
                recentEvent.setProcessedAt(LocalDateTime.now());
                blockchainEventRepository.save(recentEvent);

                // Create evidence with high block number
                EvidenceEntity evidence = new EvidenceEntity(
                                "EVID:1234567890:CN-001",
                                "0x1234567890123456789012345678901234567890",
                                "test_file.pdf",
                                "application/pdf",
                                1024L,
                                BigInteger.valueOf(1234567890),
                                "SHA256",
                                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                                BigInteger.valueOf(2500),
                                "0xTransactionHash",
                                BigInteger.valueOf(1234567890),
                                "Test evidence memo");
                evidenceRepository.save(evidence);

                // When
                evidenceSyncService.cleanupOldEvents();

                // Then
                List<BlockchainEvent> remainingEvents = blockchainEventRepository.findAll();
                assertThat(remainingEvents).hasSize(1);
                assertThat(remainingEvents.get(0).getBlockNumber()).isEqualTo(BigInteger.valueOf(1500));
        }

        @Test
        void reprocessUnprocessedEvents_ProcessesUnprocessedEvents() {
                // Given
                // Create unprocessed blockchain event
                BlockchainEvent unprocessedEvent = new BlockchainEvent(
                                "0xContractAddress",
                                "EvidenceSubmitted",
                                BigInteger.valueOf(100),
                                "0xTransactionHash",
                                BigInteger.valueOf(0),
                                BigInteger.valueOf(1234567890),
                                "{\"evidenceId\":\"EVID:1234567890:CN-001\",\"user\":\"0x1234567890123456789012345678901234567890\",\"hashValue\":\"0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef\",\"timestamp\":\"1234567890\"}");
                unprocessedEvent.setIsProcessed(false);
                blockchainEventRepository.save(unprocessedEvent);

                // Create sync status
                SyncStatus syncStatus = new SyncStatus("0xContractAddress", BigInteger.valueOf(50));
                syncStatusRepository.save(syncStatus);

                // When
                evidenceSyncService.reprocessUnprocessedEvents();

                // Then
                List<BlockchainEvent> processedEvents = blockchainEventRepository
                                .findByTransactionHash("0xTransactionHash");
                assertThat(processedEvents).hasSize(1);
                assertThat(processedEvents.get(0).getIsProcessed()).isTrue();
                assertThat(processedEvents.get(0).getProcessedAt()).isNotNull();
        }
}