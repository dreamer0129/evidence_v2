package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;
import cn.edu.gfkd.evidence.entity.Evidence;
import cn.edu.gfkd.evidence.entity.SyncStatus;
import cn.edu.gfkd.evidence.event.BlockchainEventReceived;
import cn.edu.gfkd.evidence.repository.BlockchainEventRepository;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import cn.edu.gfkd.evidence.repository.SyncStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private BlockchainEventListener blockchainEventListener;
    
    @InjectMocks
    private EvidenceSyncService evidenceSyncService;
    
    private BlockchainEventReceived evidenceSubmittedEvent;
    private BlockchainEventReceived statusChangedEvent;
    private Evidence evidence;
    private BlockchainEvent blockchainEvent;
    private SyncStatus syncStatus;
    
    @BeforeEach
    void setUp() {
        // EvidenceSubmitted event
        evidenceSubmittedEvent = new BlockchainEventReceived(
            "EvidenceSubmitted",
            "EVID:1234567890:CN-001",
            "0x1234567890123456789012345678901234567890",
            "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
            null,
            null,
            BigInteger.valueOf(100),
            "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
            BigInteger.ZERO,
            BigInteger.valueOf(1234567890),
            true
        );
        
        // EvidenceStatusChanged event
        statusChangedEvent = new BlockchainEventReceived(
            "EvidenceStatusChanged",
            "EVID:1234567890:CN-001",
            null,
            null,
            "effective",
            "verified",
            BigInteger.valueOf(101),
            "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
            BigInteger.ONE,
            BigInteger.valueOf(1234567891),
            true
        );
        
        // Evidence entity
        evidence = new Evidence(
            "EVID:1234567890:CN-001",
            "0x1234567890123456789012345678901234567890",
            "test_file.pdf",
            "application/pdf",
            1024L,
            BigInteger.valueOf(1234567800),
            "SHA256",
            "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
            BigInteger.valueOf(100),
            "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
            BigInteger.valueOf(1234567890),
            "Test evidence memo"
        );
        evidence.setId(1L);
        
        // BlockchainEvent entity
        blockchainEvent = new BlockchainEvent(
            "EvidenceSubmitted",
            "EVID:1234567890:CN-001",
            BigInteger.valueOf(100),
            "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
            BigInteger.ZERO,
            BigInteger.valueOf(1234567890),
            "{}"
        );
        blockchainEvent.setId(1L);
        
        // SyncStatus entity
        syncStatus = new SyncStatus(
            "0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512",
            BigInteger.valueOf(99)
        );
        syncStatus.setId(1L);
    }
    
    @Test
    void handleBlockchainEvent_EvidenceSubmitted_shouldCreateEvidence() {
        // Given
        when(evidenceRepository.existsByEvidenceId(evidenceSubmittedEvent.getEvidenceId())).thenReturn(false);
        when(evidenceRepository.save(any(Evidence.class))).thenReturn(evidence);
        when(blockchainEventRepository.findByTransactionHash(any())).thenReturn(List.of(blockchainEvent));
        when(blockchainEventListener.getContractAddress()).thenReturn("0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512");
        when(syncStatusRepository.findById(any())).thenReturn(Optional.of(syncStatus));
        when(syncStatusRepository.save(any(SyncStatus.class))).thenReturn(syncStatus);
        
        // When
        evidenceSyncService.handleBlockchainEvent(evidenceSubmittedEvent);
        
        // Then
        verify(evidenceRepository, times(1)).existsByEvidenceId(evidenceSubmittedEvent.getEvidenceId());
        verify(evidenceRepository, times(1)).save(any(Evidence.class));
        verify(blockchainEventRepository, times(1)).save(any(BlockchainEvent.class));
    }
    
    @Test
    void handleBlockchainEvent_EvidenceSubmitted_alreadyExists_shouldSkip() {
        // Given
        when(evidenceRepository.existsByEvidenceId(evidenceSubmittedEvent.getEvidenceId())).thenReturn(true);
        
        // When
        evidenceSyncService.handleBlockchainEvent(evidenceSubmittedEvent);
        
        // Then
        verify(evidenceRepository, times(1)).existsByEvidenceId(evidenceSubmittedEvent.getEvidenceId());
        verify(evidenceRepository, never()).save(any(Evidence.class));
    }
    
    @Test
    void handleBlockchainEvent_EvidenceStatusChanged_shouldUpdateEvidence() {
        // Given
        when(evidenceRepository.findByEvidenceId(statusChangedEvent.getEvidenceId())).thenReturn(Optional.of(evidence));
        when(evidenceRepository.save(any(Evidence.class))).thenReturn(evidence);
        when(blockchainEventRepository.findByTransactionHash(any())).thenReturn(List.of(blockchainEvent));
        when(blockchainEventListener.getContractAddress()).thenReturn("0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512");
        when(syncStatusRepository.findById(any())).thenReturn(Optional.of(syncStatus));
        when(syncStatusRepository.save(any(SyncStatus.class))).thenReturn(syncStatus);
        
        // When
        evidenceSyncService.handleBlockchainEvent(statusChangedEvent);
        
        // Then
        verify(evidenceRepository, times(1)).findByEvidenceId(statusChangedEvent.getEvidenceId());
        verify(evidenceRepository, times(1)).save(any(Evidence.class));
        assertEquals("verified", evidence.getStatus());
    }
    
    @Test
    void handleBlockchainEvent_EvidenceStatusChanged_evidenceNotFound_shouldThrowException() {
        // Given
        when(evidenceRepository.findByEvidenceId(statusChangedEvent.getEvidenceId())).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            evidenceSyncService.handleBlockchainEvent(statusChangedEvent);
        });
        
        verify(evidenceRepository, times(1)).findByEvidenceId(statusChangedEvent.getEvidenceId());
        verify(evidenceRepository, never()).save(any(Evidence.class));
    }
    
    @Test
    void updateSyncStatus_shouldUpdateLastBlockNumber() {
        // Given
        BigInteger newBlockNumber = BigInteger.valueOf(150);
        when(blockchainEventListener.getContractAddress()).thenReturn("0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512");
        when(syncStatusRepository.findById(any())).thenReturn(Optional.of(syncStatus));
        when(syncStatusRepository.save(any(SyncStatus.class))).thenReturn(syncStatus);
        
        // When
        evidenceSyncService.updateSyncStatus(newBlockNumber);
        
        // Then
        verify(syncStatusRepository, times(1)).save(any(SyncStatus.class));
        assertEquals(newBlockNumber, syncStatus.getLastBlockNumber());
        assertEquals("SYNCED", syncStatus.getSyncStatus());
    }
    
    @Test
    void markEventAsProcessed_shouldMarkEventAsProcessed() {
        // Given
        String transactionHash = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
        BigInteger logIndex = BigInteger.ZERO;
        when(blockchainEventRepository.findByTransactionHash(transactionHash)).thenReturn(List.of(blockchainEvent));
        when(blockchainEventRepository.save(any(BlockchainEvent.class))).thenReturn(blockchainEvent);
        
        // When
        evidenceSyncService.markEventAsProcessed(transactionHash, logIndex);
        
        // Then
        verify(blockchainEventRepository, times(1)).save(any(BlockchainEvent.class));
        assertTrue(blockchainEvent.getIsProcessed());
        assertNotNull(blockchainEvent.getProcessedAt());
    }
}