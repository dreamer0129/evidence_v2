package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;
import cn.edu.gfkd.evidence.entity.EvidenceEntity;
import cn.edu.gfkd.evidence.entity.SyncStatus;
import cn.edu.gfkd.evidence.event.BlockchainEventReceived;
import cn.edu.gfkd.evidence.exception.EvidenceNotFoundException;
import cn.edu.gfkd.evidence.exception.SyncException;
import cn.edu.gfkd.evidence.repository.BlockchainEventRepository;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import cn.edu.gfkd.evidence.repository.SyncStatusRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvidenceSyncServiceTest {

        @Mock
        private EvidenceRepository evidenceRepository;

        @Mock
        private BlockchainEventRepository blockchainEventRepository;

        @Mock
        private SyncStatusRepository syncStatusRepository;

        @Mock
        private EvidenceEventListener blockchainEventListener;

        @Mock
        private ObjectMapper objectMapper;

        @InjectMocks
        private EvidenceSyncService evidenceSyncService;

        private BlockchainEventReceived testEvent;
        private EvidenceEntity testEvidence;
        private BlockchainEvent testBlockchainEvent;
        private SyncStatus testSyncStatus;

        @BeforeEach
        void setUp() {
                Map<String, Object> parameters = new HashMap<>();
                testEvent = BlockchainEventReceived.builder()
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

                testEvidence = new EvidenceEntity(
                                "EVID:1234567890:CN-001",
                                "0x1234567890123456789012345678901234567890",
                                "test_file.pdf",
                                "application/pdf",
                                1024L,
                                BigInteger.valueOf(1234567890),
                                "SHA256",
                                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                                BigInteger.valueOf(100),
                                "0xTransactionHash",
                                BigInteger.valueOf(1234567890),
                                "Test evidence memo");
                testEvidence.setId(1L);

                testBlockchainEvent = new BlockchainEvent(
                                "0xContractAddress",
                                "EvidenceSubmitted",
                                BigInteger.valueOf(100),
                                "0xTransactionHash",
                                BigInteger.valueOf(0),
                                BigInteger.valueOf(1234567890),
                                "{\"test\":\"data\"}");

                testSyncStatus = new SyncStatus("0xContractAddress", BigInteger.valueOf(50));
        }

        @Test
        void handleBlockchainEvent_EvidenceSubmitted_CreatesEvidence() {
                // Given
                when(evidenceRepository.existsByEvidenceId("EVID:1234567890:CN-001")).thenReturn(false);
                when(evidenceRepository.save(any(EvidenceEntity.class))).thenReturn(testEvidence);
                when(blockchainEventListener.getContractAddress()).thenReturn("0xContractAddress");
                when(syncStatusRepository.findById("0xContractAddress")).thenReturn(Optional.of(testSyncStatus));
                when(syncStatusRepository.save(any(SyncStatus.class))).thenReturn(testSyncStatus);
                when(blockchainEventRepository.findByTransactionHash("0xTransactionHash"))
                                .thenReturn(Collections.singletonList(testBlockchainEvent));

                // When
                evidenceSyncService.handleBlockchainEvent(testEvent);

                // Then
                verify(evidenceRepository).save(any(EvidenceEntity.class));
                verify(blockchainEventRepository).save(any(BlockchainEvent.class));
                verify(syncStatusRepository).save(any(SyncStatus.class));
        }

        @Test
        void handleBlockchainEvent_EvidenceAlreadyExists_SkipsCreation() {
                // Given
                when(evidenceRepository.existsByEvidenceId("EVID:1234567890:CN-001")).thenReturn(true);
                when(blockchainEventListener.getContractAddress()).thenReturn("0xContractAddress");
                when(syncStatusRepository.findById("0xContractAddress")).thenReturn(Optional.of(testSyncStatus));
                when(syncStatusRepository.save(any(SyncStatus.class))).thenReturn(testSyncStatus);
                when(blockchainEventRepository.findByTransactionHash("0xTransactionHash"))
                                .thenReturn(Collections.singletonList(testBlockchainEvent));

                // When
                evidenceSyncService.handleBlockchainEvent(testEvent);

                // Then
                verify(evidenceRepository, never()).save(any(EvidenceEntity.class));
                verify(blockchainEventRepository).save(any(BlockchainEvent.class));
                verify(syncStatusRepository).save(any(SyncStatus.class));
        }

        @Test
        void handleBlockchainEvent_EvidenceStatusChanged_UpdatesEvidence() {
                // Given
                BlockchainEventReceived statusChangeEvent = BlockchainEventReceived.builder()
                                .contractAddress("0xContractAddress")
                                .eventName("EvidenceStatusChanged")
                                .blockNumber(BigInteger.valueOf(100))
                                .transactionHash("0xTransactionHash")
                                .logIndex(BigInteger.valueOf(0))
                                .blockTimestamp(BigInteger.valueOf(1234567890))
                                .rawData("{\"test\":\"data\"}")
                                .parameter("evidenceId", "EVID:1234567890:CN-001")
                                .parameter("newStatus", "verified")
                                .parameter("oldStatus", "pending")
                                .parameter("revoker", "0x1234567890123456789012345678901234567890")
                                .build();

                when(evidenceRepository.findByEvidenceId("EVID:1234567890:CN-001"))
                                .thenReturn(Optional.of(testEvidence));
                when(evidenceRepository.save(any(EvidenceEntity.class))).thenReturn(testEvidence);
                when(blockchainEventListener.getContractAddress()).thenReturn("0xContractAddress");
                when(syncStatusRepository.findById("0xContractAddress")).thenReturn(Optional.of(testSyncStatus));
                when(syncStatusRepository.save(any(SyncStatus.class))).thenReturn(testSyncStatus);
                when(blockchainEventRepository.findByTransactionHash("0xTransactionHash"))
                                .thenReturn(Collections.singletonList(testBlockchainEvent));

                // When
                evidenceSyncService.handleBlockchainEvent(statusChangeEvent);

                // Then
                verify(evidenceRepository).save(any(EvidenceEntity.class));
                assertThat(testEvidence.getStatus()).isEqualTo("verified");
        }

        @Test
        void handleBlockchainEvent_EvidenceRevoked_UpdatesEvidence() {
                // Given
                BlockchainEventReceived revokeEvent = BlockchainEventReceived.builder()
                                .contractAddress("0xContractAddress")
                                .eventName("EvidenceRevoked")
                                .blockNumber(BigInteger.valueOf(100))
                                .transactionHash("0xTransactionHash")
                                .logIndex(BigInteger.valueOf(0))
                                .blockTimestamp(BigInteger.valueOf(1234567890))
                                .rawData("{\"test\":\"data\"}")
                                .parameter("evidenceId", "EVID:1234567890:CN-001")
                                .parameter("revoker", "0x1234567890123456789012345678901234567890")
                                .build();

                when(evidenceRepository.findByEvidenceId("EVID:1234567890:CN-001"))
                                .thenReturn(Optional.of(testEvidence));
                when(evidenceRepository.save(any(EvidenceEntity.class))).thenReturn(testEvidence);
                when(blockchainEventListener.getContractAddress()).thenReturn("0xContractAddress");
                when(syncStatusRepository.findById("0xContractAddress")).thenReturn(Optional.of(testSyncStatus));
                when(syncStatusRepository.save(any(SyncStatus.class))).thenReturn(testSyncStatus);
                when(blockchainEventRepository.findByTransactionHash("0xTransactionHash"))
                                .thenReturn(Collections.singletonList(testBlockchainEvent));

                // When
                evidenceSyncService.handleBlockchainEvent(revokeEvent);

                // Then
                verify(evidenceRepository).save(any(EvidenceEntity.class));
                assertThat(testEvidence.getStatus()).isEqualTo("revoked");
                assertThat(testEvidence.getRevokerAddress()).isEqualTo("0x1234567890123456789012345678901234567890");
                assertThat(testEvidence.getRevokedAt()).isNotNull();
        }

        @Test
        void handleBlockchainEvent_EvidenceNotFound_ThrowsSyncException() {
                // Given
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("evidenceId", "NONEXISTENT");
                parameters.put("newStatus", "verified");
                parameters.put("oldStatus", "pending");

                BlockchainEventReceived statusChangeEvent = BlockchainEventReceived.builder()
                                .contractAddress("0xContractAddress")
                                .eventName("EvidenceStatusChanged")
                                .blockNumber(BigInteger.valueOf(100))
                                .transactionHash("0xTransactionHash")
                                .logIndex(BigInteger.valueOf(0))
                                .blockTimestamp(BigInteger.valueOf(1234567890))
                                .rawData("{\"test\":\"data\"}")
                                .parameter("evidenceId", "NONEXISTENT")
                                .parameter("newStatus", "verified")
                                .parameter("oldStatus", "pending")
                                .build();

                when(evidenceRepository.findByEvidenceId("NONEXISTENT")).thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> evidenceSyncService.handleBlockchainEvent(statusChangeEvent))
                                .isInstanceOf(SyncException.class)
                                .hasMessageContaining("Failed to process blockchain event");
        }

        @Test
        void handleBlockchainEvent_UnknownEventType_LogsWarning() {
                // Given
                BlockchainEventReceived unknownEvent = BlockchainEventReceived.builder()
                                .contractAddress("0xContractAddress")
                                .eventName("UnknownEvent")
                                .blockNumber(BigInteger.valueOf(100))
                                .transactionHash("0xTransactionHash")
                                .logIndex(BigInteger.valueOf(0))
                                .blockTimestamp(BigInteger.valueOf(1234567890))
                                .rawData("{\"test\":\"data\"}")
                                .build();

                when(blockchainEventListener.getContractAddress()).thenReturn("0xContractAddress");
                when(syncStatusRepository.findById("0xContractAddress")).thenReturn(Optional.of(testSyncStatus));
                when(syncStatusRepository.save(any(SyncStatus.class))).thenReturn(testSyncStatus);
                when(blockchainEventRepository.findByTransactionHash("0xTransactionHash"))
                                .thenReturn(Collections.singletonList(testBlockchainEvent));

                // When
                evidenceSyncService.handleBlockchainEvent(unknownEvent);

                // Then
                verify(evidenceRepository, never()).save(any(EvidenceEntity.class));
                verify(syncStatusRepository).save(any(SyncStatus.class));
        }

        @Test
        void handleBlockchainEvent_NullEvent_ThrowsNullPointerException() {
                // When & Then
                assertThatThrownBy(() -> evidenceSyncService.handleBlockchainEvent(null))
                                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void handleBlockchainEvent_InvalidEventData_ThrowsSyncException() {
                // Given
                BlockchainEventReceived invalidEvent = BlockchainEventReceived.builder()
                                .contractAddress("0xContractAddress")
                                .eventName("EvidenceSubmitted")
                                .blockNumber(BigInteger.valueOf(100))
                                .transactionHash("0xTransactionHash")
                                .logIndex(BigInteger.valueOf(0))
                                .blockTimestamp(BigInteger.valueOf(1234567890))
                                .rawData("{\"test\":\"data\"}")
                                .build();

                // When & Then
                assertThatThrownBy(() -> evidenceSyncService.handleBlockchainEvent(invalidEvent))
                                .isInstanceOf(SyncException.class)
                                .hasMessageContaining("Failed to process blockchain event");
        }

        @Test
        void handleBlockchainEvent_ExceptionDuringProcessing_ThrowsSyncException() {
                // Given
                lenient().when(evidenceRepository.existsByEvidenceId("EVID:1234567890:CN-001"))
                                .thenThrow(new RuntimeException("Database error"));
                lenient().when(blockchainEventListener.getContractAddress()).thenReturn("0xContractAddress");

                // When & Then
                assertThatThrownBy(() -> evidenceSyncService.handleBlockchainEvent(testEvent))
                                .isInstanceOf(SyncException.class)
                                .hasMessageContaining("Failed to process blockchain event");
        }

        @Test
        void reprocessUnprocessedEvents_NoUnprocessedEvents_DoesNothing() {
                // Given
                when(blockchainEventRepository.findUnprocessedEvents(any()))
                                .thenReturn(Collections.emptyList());

                // When
                evidenceSyncService.reprocessUnprocessedEvents();

                // Then
                verify(blockchainEventRepository).findUnprocessedEvents(any());
                verify(evidenceRepository, never()).save(any(EvidenceEntity.class));
        }

        @Test
        void cleanupOldEvents_DeletesOldEvents() {
                // Given
                when(evidenceRepository.findMaxBlockNumber()).thenReturn(BigInteger.valueOf(2000));

                // When
                evidenceSyncService.cleanupOldEvents();

                // Then
                verify(blockchainEventRepository).deleteByBlockNumberLessThan(BigInteger.valueOf(1000));
        }

        @Test
        void cleanupOldEvents_InsufficientBlocks_DoesNothing() {
                // Given
                when(evidenceRepository.findMaxBlockNumber()).thenReturn(BigInteger.valueOf(500));

                // When
                evidenceSyncService.cleanupOldEvents();

                // Then
                verify(blockchainEventRepository, never()).deleteByBlockNumberLessThan(any());
        }
}