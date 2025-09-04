package cn.edu.gfkd.evidence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;
import cn.edu.gfkd.evidence.entity.EvidenceEntity;
import cn.edu.gfkd.evidence.exception.SyncException;
import cn.edu.gfkd.evidence.repository.BlockchainEventRepository;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import cn.edu.gfkd.evidence.repository.SyncStatusRepository;

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

        private BlockchainEvent testBlockchainEvent;

        @BeforeEach
        void setUp() {
                testBlockchainEvent = new BlockchainEvent(
                                "0xContractAddress",
                                "EvidenceSubmitted",
                                BigInteger.valueOf(100),
                                "0xTransactionHash",
                                BigInteger.valueOf(0),
                                BigInteger.valueOf(1234567890),
                                "{\"evidenceId\":\"EVID:1234567890:CN-001\",\"user\":\"0x1234567890123456789012345678901234567890\",\"hashValue\":\"0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef\",\"timestamp\":\"1234567890\"}");
        }

        @Test
        void reprocessUnprocessedEvents_HasUnprocessedEvents_CallsEventListenerSync() throws JsonProcessingException {
                // Given
                when(blockchainEventRepository.findUnprocessedEvents(any()))
                                .thenReturn(Collections.singletonList(testBlockchainEvent));
                when(objectMapper.readTree(testBlockchainEvent.getRawData()))
                                .thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"evidenceId\":\"EVID:1234567890:CN-001\"}"));

                // When
                evidenceSyncService.reprocessUnprocessedEvents();

                // Then
                verify(blockchainEventRepository).findUnprocessedEvents(any());
                verify(blockchainEventListener).syncPastEvents(BigInteger.valueOf(100), BigInteger.valueOf(100));
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
                verify(blockchainEventListener, never()).syncPastEvents(any(), any());
        }

        @Test
        void reprocessUnprocessedEvents_ExceptionDuringProcessing_LogsError() throws JsonProcessingException {
                // Given
                when(blockchainEventRepository.findUnprocessedEvents(any()))
                                .thenReturn(Collections.singletonList(testBlockchainEvent));
                when(objectMapper.readTree(testBlockchainEvent.getRawData()))
                                .thenThrow(new RuntimeException("JSON parsing error"));

                // When
                evidenceSyncService.reprocessUnprocessedEvents();

                // Then - the implementation still calls syncPastEvents even when JSON parsing fails
                verify(blockchainEventRepository).findUnprocessedEvents(any());
                verify(blockchainEventListener).syncPastEvents(BigInteger.valueOf(100), BigInteger.valueOf(100));
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

        @Test
        void cleanupOldEvents_NullMaxBlockNumber_DoesNothing() {
                // Given
                when(evidenceRepository.findMaxBlockNumber()).thenReturn(null);

                // When
                evidenceSyncService.cleanupOldEvents();

                // Then
                verify(blockchainEventRepository, never()).deleteByBlockNumberLessThan(any());
        }

        @Test
        void cleanupOldEvents_ExceptionDuringProcessing_ThrowsSyncException() {
                // Given
                when(evidenceRepository.findMaxBlockNumber())
                                .thenThrow(new RuntimeException("Database error"));

                // When & Then
                assertThatThrownBy(() -> evidenceSyncService.cleanupOldEvents())
                                .isInstanceOf(SyncException.class)
                                .hasMessageContaining("Failed to cleanup old events");
        }
}