package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.entity.Evidence;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvidenceServiceTest {
    
    @Mock
    private EvidenceRepository evidenceRepository;
    
    @InjectMocks
    private EvidenceService evidenceService;
    
    private Evidence evidence1;
    private Evidence evidence2;
    
    @BeforeEach
    void setUp() {
        evidence1 = new Evidence(
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
        evidence1.setId(1L);
        
        evidence2 = new Evidence(
            "EVID:1234567891:CN-002",
            "0x0987654321098765432109876543210987654321",
            "test_image.jpg",
            "image/jpeg",
            2048L,
            BigInteger.valueOf(1234567801),
            "SHA256",
            "0x0987654321fedcba0987654321fedcba0987654321fedcba0987654321fedcba",
            BigInteger.valueOf(101),
            "0x0987654321fedcba0987654321fedcba0987654321fedcba0987654321fedcba",
            BigInteger.valueOf(1234567891),
            "Test image evidence"
        );
        evidence2.setId(2L);
    }
    
    @Test
    void createEvidence_shouldReturnSavedEvidence() {
        // Given
        when(evidenceRepository.save(any(Evidence.class))).thenReturn(evidence1);
        
        // When
        Evidence result = evidenceService.createEvidence(evidence1);
        
        // Then
        assertNotNull(result);
        assertEquals(evidence1.getId(), result.getId());
        assertEquals(evidence1.getEvidenceId(), result.getEvidenceId());
        verify(evidenceRepository, times(1)).save(evidence1);
    }
    
    @Test
    void getEvidenceById_shouldReturnEvidence() {
        // Given
        when(evidenceRepository.findById(1L)).thenReturn(Optional.of(evidence1));
        
        // When
        Optional<Evidence> result = evidenceService.getEvidenceById(1L);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(evidence1.getId(), result.get().getId());
        verify(evidenceRepository, times(1)).findById(1L);
    }
    
    @Test
    void getEvidenceByEvidenceId_shouldReturnEvidence() {
        // Given
        when(evidenceRepository.findByEvidenceId("EVID:1234567890:CN-001")).thenReturn(Optional.of(evidence1));
        
        // When
        Optional<Evidence> result = evidenceService.getEvidenceByEvidenceId("EVID:1234567890:CN-001");
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(evidence1.getEvidenceId(), result.get().getEvidenceId());
        verify(evidenceRepository, times(1)).findByEvidenceId("EVID:1234567890:CN-001");
    }
    
    @Test
    void getEvidenceByUserAddress_shouldReturnEvidenceList() {
        // Given
        String userAddress = "0x1234567890123456789012345678901234567890";
        List<Evidence> evidenceList = Arrays.asList(evidence1);
        when(evidenceRepository.findByUserAddress(userAddress)).thenReturn(evidenceList);
        
        // When
        List<Evidence> result = evidenceService.getEvidenceByUserAddress(userAddress);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userAddress, result.get(0).getUserAddress());
        verify(evidenceRepository, times(1)).findByUserAddress(userAddress);
    }
    
    @Test
    void getEvidenceByUserAddressWithPagination_shouldReturnPage() {
        // Given
        String userAddress = "0x1234567890123456789012345678901234567890";
        Pageable pageable = PageRequest.of(0, 10);
        Page<Evidence> evidencePage = new PageImpl<>(Arrays.asList(evidence1));
        when(evidenceRepository.findByUserAddress(userAddress, pageable)).thenReturn(evidencePage);
        
        // When
        Page<Evidence> result = evidenceService.getEvidenceByUserAddress(userAddress, pageable);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(userAddress, result.getContent().get(0).getUserAddress());
        verify(evidenceRepository, times(1)).findByUserAddress(userAddress, pageable);
    }
    
    @Test
    void existsByEvidenceId_shouldReturnTrue() {
        // Given
        String evidenceId = "EVID:1234567890:CN-001";
        when(evidenceRepository.existsByEvidenceId(evidenceId)).thenReturn(true);
        
        // When
        boolean result = evidenceService.existsByEvidenceId(evidenceId);
        
        // Then
        assertTrue(result);
        verify(evidenceRepository, times(1)).existsByEvidenceId(evidenceId);
    }
    
    @Test
    void existsByEvidenceId_shouldReturnFalse() {
        // Given
        String evidenceId = "NONEXISTENT";
        when(evidenceRepository.existsByEvidenceId(evidenceId)).thenReturn(false);
        
        // When
        boolean result = evidenceService.existsByEvidenceId(evidenceId);
        
        // Then
        assertFalse(result);
        verify(evidenceRepository, times(1)).existsByEvidenceId(evidenceId);
    }
    
    @Test
    void countByUserAddress_shouldReturnCount() {
        // Given
        String userAddress = "0x1234567890123456789012345678901234567890";
        when(evidenceRepository.countByUserAddress(userAddress)).thenReturn(5L);
        
        // When
        long result = evidenceService.countByUserAddress(userAddress);
        
        // Then
        assertEquals(5L, result);
        verify(evidenceRepository, times(1)).countByUserAddress(userAddress);
    }
    
    @Test
    void updateEvidence_shouldReturnUpdatedEvidence() {
        // Given
        evidence1.setStatus("verified");
        when(evidenceRepository.save(any(Evidence.class))).thenReturn(evidence1);
        
        // When
        Evidence result = evidenceService.updateEvidence(evidence1);
        
        // Then
        assertNotNull(result);
        assertEquals("verified", result.getStatus());
        verify(evidenceRepository, times(1)).save(evidence1);
    }
    
    @Test
    void deleteEvidence_shouldDeleteEvidence() {
        // Given
        doNothing().when(evidenceRepository).deleteById(1L);
        
        // When
        evidenceService.deleteEvidence(1L);
        
        // Then
        verify(evidenceRepository, times(1)).deleteById(1L);
    }
}