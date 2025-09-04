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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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

        private String generateUniqueTransactionHash() {
            return "0x" + UUID.randomUUID().toString().replace("-", "") + "1234567890";
        }

        private String generateUniqueEvidenceId() {
            return "EVID:" + System.currentTimeMillis() + ":CN-" + UUID.randomUUID().toString().substring(0, 8);
        }

        private String generateUniqueContractAddress() {
            return "0x" + String.format("%040d", Math.abs(UUID.randomUUID().hashCode()));
        }

        @Test
        void reprocessUnprocessedEvents_ProcessesEvidenceSubmittedEvent() throws Exception {
                // Given
                String contractAddress = generateUniqueContractAddress();
                String transactionHash = generateUniqueTransactionHash();
                String evidenceId = generateUniqueEvidenceId();
                String userAddress = "0x1234567890123456789012345678901234567890";
                String hashValue = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
                
                when(evidenceEventListener.getContractAddress()).thenReturn(contractAddress);

                // Create blockchain event record with raw data that can be parsed
                BlockchainEvent blockchainEvent = new BlockchainEvent(
                                contractAddress,
                                "EvidenceSubmitted",
                                BigInteger.valueOf(100),
                                transactionHash,
                                BigInteger.valueOf(0),
                                BigInteger.valueOf(1234567890),
                                "{\"evidenceId\":\"" + evidenceId + "\",\"user\":\"" + userAddress + "\",\"hashValue\":\"" + hashValue + "\",\"timestamp\":\"1234567890\"}");
                blockchainEvent.setIsProcessed(false);
                blockchainEventRepository.save(blockchainEvent);

                // Create sync status
                SyncStatus syncStatus = new SyncStatus(contractAddress, BigInteger.valueOf(50));
                syncStatusRepository.save(syncStatus);

                // When
                evidenceSyncService.reprocessUnprocessedEvents();

                // Then
                List<EvidenceEntity> evidenceList = evidenceRepository.findAll();
                assertThat(evidenceList).hasSize(1);
                EvidenceEntity savedEvidence = evidenceList.get(0);
                assertThat(savedEvidence.getEvidenceId()).isEqualTo(evidenceId);
                assertThat(savedEvidence.getUserAddress()).isEqualTo(userAddress);
                assertThat(savedEvidence.getHashValue()).isEqualTo(hashValue);
                assertThat(savedEvidence.getStatus()).isEqualTo("effective");

                // Verify blockchain event is marked as processed
                List<BlockchainEvent> blockchainEvents = blockchainEventRepository
                                .findByTransactionHash(transactionHash);
                assertThat(blockchainEvents).hasSize(1);
                assertThat(blockchainEvents.get(0).getIsProcessed()).isTrue();
        }

        @Test
        void handleBlockchainEvent_EvidenceAlreadyExists_DoesNotCreateDuplicate() {
                // Given
                String evidenceId = generateUniqueEvidenceId();
                String userAddress = "0x1234567890123456789012345678901234567890";
                String hashValue = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
                String contractAddress = generateUniqueContractAddress();
                String transactionHash = generateUniqueTransactionHash();
                
                EvidenceEntity existingEvidence = new EvidenceEntity(
                                evidenceId,
                                userAddress,
                                "test_file.pdf",
                                "application/pdf",
                                1024L,
                                BigInteger.valueOf(1234567890),
                                "SHA256",
                                hashValue,
                                BigInteger.valueOf(90),
                                "0xOldTransactionHash",
                                BigInteger.valueOf(1234567890),
                                "Test evidence memo");
                evidenceRepository.save(existingEvidence);

                BlockchainEventReceived event = BlockchainEventReceived.builder()
                                .contractAddress(contractAddress)
                                .eventName("EvidenceSubmitted")
                                .blockNumber(BigInteger.valueOf(100))
                                .transactionHash(transactionHash)
                                .logIndex(BigInteger.valueOf(0))
                                .blockTimestamp(BigInteger.valueOf(1234567890))
                                .rawData("{\"test\":\"data\"}")
                                .parameter("evidenceId", evidenceId)
                                .parameter("user", userAddress)
                                .parameter("hashValue", hashValue)
                                .parameter("timestamp", BigInteger.valueOf(1234567890))
                                .build();

                // Create sync status
                SyncStatus syncStatus = new SyncStatus(contractAddress, BigInteger.valueOf(50));
                syncStatusRepository.save(syncStatus);

                // When
                evidenceSyncService.handleBlockchainEvent(event);

                // Then
                List<EvidenceEntity> evidenceList = evidenceRepository.findAll();
                assertThat(evidenceList).hasSize(1);
                assertThat(evidenceList.get(0).getEvidenceId()).isEqualTo(evidenceId);
        }

        @Test
        void reprocessUnprocessedEvents_ProcessesEvidenceStatusChangedEvent() throws Exception {
                // Given
                String contractAddress = generateUniqueContractAddress();
                String transactionHash = generateUniqueTransactionHash();
                String evidenceId = generateUniqueEvidenceId();
                String userAddress = "0x1234567890123456789012345678901234567890";
                String hashValue = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
                
                when(evidenceEventListener.getContractAddress()).thenReturn(contractAddress);

                EvidenceEntity existingEvidence = new EvidenceEntity(
                                evidenceId,
                                userAddress,
                                "test_file.pdf",
                                "application/pdf",
                                1024L,
                                BigInteger.valueOf(1234567890),
                                "SHA256",
                                hashValue,
                                BigInteger.valueOf(90),
                                "0xOldTransactionHash",
                                BigInteger.valueOf(1234567890),
                                "Test evidence memo");
                existingEvidence.setStatus("pending");
                evidenceRepository.save(existingEvidence);

                // Create blockchain event record with raw data that can be parsed
                BlockchainEvent blockchainEvent = new BlockchainEvent(
                                contractAddress,
                                "EvidenceStatusChanged",
                                BigInteger.valueOf(100),
                                transactionHash,
                                BigInteger.valueOf(0),
                                BigInteger.valueOf(1234567890),
                                "{\"evidenceId\":\"" + evidenceId + "\",\"oldStatus\":\"pending\",\"newStatus\":\"verified\"}");
                blockchainEvent.setIsProcessed(false);
                blockchainEventRepository.save(blockchainEvent);

                // Create sync status
                SyncStatus syncStatus = new SyncStatus(contractAddress, BigInteger.valueOf(50));
                syncStatusRepository.save(syncStatus);

                // When
                evidenceSyncService.reprocessUnprocessedEvents();

                // Then
                EvidenceEntity updatedEvidence = evidenceRepository.findByEvidenceId(evidenceId)
                                .orElse(null);
                assertThat(updatedEvidence).isNotNull();
                assertThat(updatedEvidence.getStatus()).isEqualTo("verified");

                // Verify blockchain event is marked as processed
                List<BlockchainEvent> blockchainEvents = blockchainEventRepository
                                .findByTransactionHash(transactionHash);
                assertThat(blockchainEvents).hasSize(1);
                assertThat(blockchainEvents.get(0).getIsProcessed()).isTrue();
        }

        @Test
        void reprocessUnprocessedEvents_ProcessesEvidenceRevokedEvent() throws Exception {
                // Given
                String contractAddress = generateUniqueContractAddress();
                String transactionHash = generateUniqueTransactionHash();
                String evidenceId = generateUniqueEvidenceId();
                String userAddress = "0x1234567890123456789012345678901234567890";
                String hashValue = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
                
                when(evidenceEventListener.getContractAddress()).thenReturn(contractAddress);

                EvidenceEntity existingEvidence = new EvidenceEntity(
                                evidenceId,
                                userAddress,
                                "test_file.pdf",
                                "application/pdf",
                                1024L,
                                BigInteger.valueOf(1234567890),
                                "SHA256",
                                hashValue,
                                BigInteger.valueOf(90),
                                "0xOldTransactionHash",
                                BigInteger.valueOf(1234567890),
                                "Test evidence memo");
                existingEvidence.setStatus("effective");
                evidenceRepository.save(existingEvidence);

                // Create blockchain event record with raw data that can be parsed
                BlockchainEvent blockchainEvent = new BlockchainEvent(
                                contractAddress,
                                "EvidenceRevoked",
                                BigInteger.valueOf(100),
                                transactionHash,
                                BigInteger.valueOf(0),
                                BigInteger.valueOf(1234567890),
                                "{\"evidenceId\":\"" + evidenceId + "\",\"revoker\":\"0x1234567890123456789012345678901234567890\"}");
                blockchainEvent.setIsProcessed(false);
                blockchainEventRepository.save(blockchainEvent);

                // Create sync status
                SyncStatus syncStatus = new SyncStatus(contractAddress, BigInteger.valueOf(50));
                syncStatusRepository.save(syncStatus);

                // When
                evidenceSyncService.reprocessUnprocessedEvents();

                // Then
                EvidenceEntity updatedEvidence = evidenceRepository.findByEvidenceId(evidenceId)
                                .orElse(null);
                assertThat(updatedEvidence).isNotNull();
                assertThat(updatedEvidence.getStatus()).isEqualTo("revoked");
                assertThat(updatedEvidence.getRevokerAddress()).isNotNull();
                assertThat(updatedEvidence.getRevokedAt()).isNotNull();

                // Verify blockchain event is marked as processed
                List<BlockchainEvent> blockchainEvents = blockchainEventRepository
                                .findByTransactionHash(transactionHash);
                assertThat(blockchainEvents).hasSize(1);
                assertThat(blockchainEvents.get(0).getIsProcessed()).isTrue();
        }

        @Test
        void cleanupOldEvents_DeletesEventsBeforeCutoffBlock() {
                // Given
                String contractAddress = generateUniqueContractAddress();
                String oldTransactionHash = generateUniqueTransactionHash();
                String recentTransactionHash = generateUniqueTransactionHash();
                String transactionHash = generateUniqueTransactionHash();
                String evidenceId = generateUniqueEvidenceId();
                String userAddress = "0x1234567890123456789012345678901234567890";
                String hashValue = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
                
                // Create old blockchain event
                BlockchainEvent oldEvent = new BlockchainEvent(
                                contractAddress,
                                "EvidenceSubmitted",
                                BigInteger.valueOf(500),
                                oldTransactionHash,
                                BigInteger.valueOf(0),
                                BigInteger.valueOf(1234567890),
                                "{\"test\":\"data\"}");
                oldEvent.setIsProcessed(true);
                oldEvent.setProcessedAt(LocalDateTime.now());
                blockchainEventRepository.save(oldEvent);

                // Create recent blockchain event
                BlockchainEvent recentEvent = new BlockchainEvent(
                                contractAddress,
                                "EvidenceSubmitted",
                                BigInteger.valueOf(1500),
                                recentTransactionHash,
                                BigInteger.valueOf(0),
                                BigInteger.valueOf(1234567890),
                                "{\"test\":\"data\"}");
                recentEvent.setIsProcessed(true);
                recentEvent.setProcessedAt(LocalDateTime.now());
                blockchainEventRepository.save(recentEvent);

                // Create evidence with high block number
                EvidenceEntity evidence = new EvidenceEntity(
                                evidenceId,
                                userAddress,
                                "test_file.pdf",
                                "application/pdf",
                                1024L,
                                BigInteger.valueOf(1234567890),
                                "SHA256",
                                hashValue,
                                BigInteger.valueOf(2500),
                                transactionHash,
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
                String contractAddress = generateUniqueContractAddress();
                String transactionHash = generateUniqueTransactionHash();
                String evidenceId = generateUniqueEvidenceId();
                String userAddress = "0x1234567890123456789012345678901234567890";
                String hashValue = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
                
                // Create unprocessed blockchain event
                BlockchainEvent unprocessedEvent = new BlockchainEvent(
                                contractAddress,
                                "EvidenceSubmitted",
                                BigInteger.valueOf(100),
                                transactionHash,
                                BigInteger.valueOf(0),
                                BigInteger.valueOf(1234567890),
                                "{\"evidenceId\":\"" + evidenceId + "\",\"user\":\"" + userAddress + "\",\"hashValue\":\"" + hashValue + "\",\"timestamp\":\"1234567890\"}");
                unprocessedEvent.setIsProcessed(false);
                blockchainEventRepository.save(unprocessedEvent);

                // Create sync status
                SyncStatus syncStatus = new SyncStatus(contractAddress, BigInteger.valueOf(50));
                syncStatusRepository.save(syncStatus);

                // When
                evidenceSyncService.reprocessUnprocessedEvents();

                // Then
                List<BlockchainEvent> processedEvents = blockchainEventRepository
                                .findByTransactionHash(transactionHash);
                assertThat(processedEvents).hasSize(1);
                assertThat(processedEvents.get(0).getIsProcessed()).isTrue();
                assertThat(processedEvents.get(0).getProcessedAt()).isNotNull();
        }
}