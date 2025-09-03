package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.entity.Evidence;
import cn.edu.gfkd.evidence.exception.EvidenceNotFoundException;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EvidenceService Unit Tests")
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
            "0x1234567890123456789012345678901234567890",
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
    @DisplayName("Should create evidence successfully when data is valid")
    void createEvidence_WhenValidData_ShouldCreateEvidence() {
        // Arrange
        when(evidenceRepository.save(any(Evidence.class))).thenReturn(evidence1);
        
        // Act
        Evidence result = evidenceService.createEvidence(evidence1);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("EVID:1234567890:CN-001", result.getEvidenceId());
        assertEquals("0x1234567890123456789012345678901234567890", result.getUserAddress());
        assertEquals("0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef", result.getHashValue());
        
        // Verify interactions
        verify(evidenceRepository).save(evidence1);
    }

    @Test
    @DisplayName("Should throw exception when creating evidence with null data")
    void createEvidence_WhenEvidenceIsNull_ShouldThrowException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> evidenceService.createEvidence(null)
        );

        assertEquals("Evidence cannot be null", exception.getMessage());
        
        // Verify no interactions
        verifyNoInteractions(evidenceRepository);
    }

    @Test
    @DisplayName("Should throw exception when creating evidence with empty evidence ID")
    void createEvidence_WhenEvidenceIdIsEmpty_ShouldThrowException() {
        // Arrange
        evidence1.setEvidenceId("");
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> evidenceService.createEvidence(evidence1)
        );

        assertEquals("Evidence ID cannot be empty", exception.getMessage());
        
        // Verify no interactions
        verifyNoInteractions(evidenceRepository);
    }

    @Test
    @DisplayName("Should throw exception when creating evidence with empty user address")
    void createEvidence_WhenUserAddressIsEmpty_ShouldThrowException() {
        // Arrange
        evidence1.setUserAddress("");
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> evidenceService.createEvidence(evidence1)
        );

        assertEquals("User address cannot be empty", exception.getMessage());
        
        // Verify no interactions
        verifyNoInteractions(evidenceRepository);
    }

    @Test
    @DisplayName("Should throw exception when creating evidence with empty hash value")
    void createEvidence_WhenHashValueIsEmpty_ShouldThrowException() {
        // Arrange
        evidence1.setHashValue("");
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> evidenceService.createEvidence(evidence1)
        );

        assertEquals("Hash value cannot be empty", exception.getMessage());
        
        // Verify no interactions
        verifyNoInteractions(evidenceRepository);
    }
    
    @Test
    @DisplayName("Should return evidence when found by ID")
    void getEvidenceById_WhenEvidenceExists_ShouldReturnEvidence() {
        // Arrange
        when(evidenceRepository.findById(1L)).thenReturn(Optional.of(evidence1));
        
        // Act
        Optional<Evidence> result = evidenceService.getEvidenceById(1L);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals("EVID:1234567890:CN-001", result.get().getEvidenceId());
        
        // Verify interactions
        verify(evidenceRepository).findById(1L);
    }
    
    @Test
    @DisplayName("Should return empty when evidence not found by ID")
    void getEvidenceById_WhenEvidenceDoesNotExist_ShouldReturnEmpty() {
        // Arrange
        when(evidenceRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act
        Optional<Evidence> result = evidenceService.getEvidenceById(999L);
        
        // Assert
        assertFalse(result.isPresent());
        
        // Verify interactions
        verify(evidenceRepository).findById(999L);
    }
    
    @Test
    @DisplayName("Should return evidence when found by evidence ID")
    void getEvidenceByEvidenceId_WhenEvidenceExists_ShouldReturnEvidence() {
        // Arrange
        when(evidenceRepository.findByEvidenceId("EVID:1234567890:CN-001")).thenReturn(Optional.of(evidence1));
        
        // Act
        Optional<Evidence> result = evidenceService.getEvidenceByEvidenceId("EVID:1234567890:CN-001");
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals("EVID:1234567890:CN-001", result.get().getEvidenceId());
        
        // Verify interactions
        verify(evidenceRepository).findByEvidenceId("EVID:1234567890:CN-001");
    }
    
    @Test
    @DisplayName("Should return evidence when found by transaction hash")
    void getEvidenceByTransactionHash_WhenEvidenceExists_ShouldReturnEvidence() {
        // Arrange
        String txHash = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
        when(evidenceRepository.findByTransactionHash(txHash)).thenReturn(Optional.of(evidence1));
        
        // Act
        Optional<Evidence> result = evidenceService.getEvidenceByTransactionHash(txHash);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(txHash, result.get().getTransactionHash());
        
        // Verify interactions
        verify(evidenceRepository).findByTransactionHash(txHash);
    }
    
    @Test
    @DisplayName("Should return evidence list when found by user address")
    void getEvidenceByUserAddress_WhenEvidenceExists_ShouldReturnEvidenceList() {
        // Arrange
        String userAddress = "0x1234567890123456789012345678901234567890";
        List<Evidence> evidenceList = Arrays.asList(evidence1, evidence2);
        when(evidenceRepository.findByUserAddress(userAddress)).thenReturn(evidenceList);
        
        // Act
        List<Evidence> result = evidenceService.getEvidenceByUserAddress(userAddress);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(userAddress, result.get(0).getUserAddress());
        assertEquals(userAddress, result.get(1).getUserAddress());
        
        // Verify interactions
        verify(evidenceRepository).findByUserAddress(userAddress);
    }
    
    @Test
    @DisplayName("Should return empty list when no evidence found by user address")
    void getEvidenceByUserAddress_WhenNoEvidenceExists_ShouldReturnEmptyList() {
        // Arrange
        String userAddress = "0x0000000000000000000000000000000000000000";
        when(evidenceRepository.findByUserAddress(userAddress)).thenReturn(Collections.emptyList());
        
        // Act
        List<Evidence> result = evidenceService.getEvidenceByUserAddress(userAddress);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        // Verify interactions
        verify(evidenceRepository).findByUserAddress(userAddress);
    }
    
    @Test
    @DisplayName("Should return evidence list when found by hash value")
    void getEvidenceByHashValue_WhenEvidenceExists_ShouldReturnEvidenceList() {
        // Arrange
        String hashValue = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
        List<Evidence> evidenceList = Arrays.asList(evidence1);
        when(evidenceRepository.findByHashValue(hashValue)).thenReturn(evidenceList);
        
        // Act
        List<Evidence> result = evidenceService.getEvidenceByHashValue(hashValue);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(hashValue, result.get(0).getHashValue());
        
        // Verify interactions
        verify(evidenceRepository).findByHashValue(hashValue);
    }
    
    @Test
    @DisplayName("Should return evidence list when found by status")
    void getEvidenceByStatus_WhenEvidenceExists_ShouldReturnEvidenceList() {
        // Arrange
        String status = "effective";
        List<Evidence> evidenceList = Arrays.asList(evidence1, evidence2);
        when(evidenceRepository.findByStatus(status)).thenReturn(evidenceList);
        
        // Act
        List<Evidence> result = evidenceService.getEvidenceByStatus(status);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(status, result.get(0).getStatus());
        assertEquals(status, result.get(1).getStatus());
        
        // Verify interactions
        verify(evidenceRepository).findByStatus(status);
    }
    
    @Test
    @DisplayName("Should return paginated evidence when found by user address")
    void getEvidenceByUserAddressWithPagination_WhenEvidenceExists_ShouldReturnPage() {
        // Arrange
        String userAddress = "0x1234567890123456789012345678901234567890";
        Pageable pageable = PageRequest.of(0, 10);
        Page<Evidence> evidencePage = new PageImpl<>(Arrays.asList(evidence1));
        when(evidenceRepository.findByUserAddress(userAddress, pageable)).thenReturn(evidencePage);
        
        // Act
        Page<Evidence> result = evidenceService.getEvidenceByUserAddress(userAddress, pageable);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(userAddress, result.getContent().get(0).getUserAddress());
        
        // Verify interactions
        verify(evidenceRepository).findByUserAddress(userAddress, pageable);
    }
    
    @Test
    @DisplayName("Should return paginated evidence when found by status")
    void getEvidenceByStatusWithPagination_WhenEvidenceExists_ShouldReturnPage() {
        // Arrange
        String status = "effective";
        Pageable pageable = PageRequest.of(0, 10);
        Page<Evidence> evidencePage = new PageImpl<>(Arrays.asList(evidence1, evidence2));
        when(evidenceRepository.findByStatus(status, pageable)).thenReturn(evidencePage);
        
        // Act
        Page<Evidence> result = evidenceService.getEvidenceByStatus(status, pageable);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(status, result.getContent().get(0).getStatus());
        
        // Verify interactions
        verify(evidenceRepository).findByStatus(status, pageable);
    }
    
    @Test
    @DisplayName("Should return paginated evidence when searching with filters")
    void searchEvidence_WhenEvidenceExists_ShouldReturnPage() {
        // Arrange
        String evidenceId = "EVID:1234567890:CN-001";
        String userAddress = "0x1234567890123456789012345678901234567890";
        String status = "effective";
        Pageable pageable = PageRequest.of(0, 10);
        Page<Evidence> evidencePage = new PageImpl<>(Arrays.asList(evidence1));
        when(evidenceRepository.findByFilters(evidenceId, userAddress, status, pageable)).thenReturn(evidencePage);
        
        // Act
        Page<Evidence> result = evidenceService.searchEvidence(evidenceId, userAddress, status, pageable);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(evidenceId, result.getContent().get(0).getEvidenceId());
        
        // Verify interactions
        verify(evidenceRepository).findByFilters(evidenceId, userAddress, status, pageable);
    }
    
    @Test
    @DisplayName("Should return true when evidence exists by evidence ID")
    void existsByEvidenceId_WhenEvidenceExists_ShouldReturnTrue() {
        // Arrange
        String evidenceId = "EVID:1234567890:CN-001";
        when(evidenceRepository.existsByEvidenceId(evidenceId)).thenReturn(true);
        
        // Act
        boolean result = evidenceService.existsByEvidenceId(evidenceId);
        
        // Assert
        assertTrue(result);
        
        // Verify interactions
        verify(evidenceRepository).existsByEvidenceId(evidenceId);
    }
    
    @Test
    @DisplayName("Should return false when evidence does not exist by evidence ID")
    void existsByEvidenceId_WhenEvidenceDoesNotExist_ShouldReturnFalse() {
        // Arrange
        String evidenceId = "NONEXISTENT";
        when(evidenceRepository.existsByEvidenceId(evidenceId)).thenReturn(false);
        
        // Act
        boolean result = evidenceService.existsByEvidenceId(evidenceId);
        
        // Assert
        assertFalse(result);
        
        // Verify interactions
        verify(evidenceRepository).existsByEvidenceId(evidenceId);
    }
    
    @Test
    @DisplayName("Should return true when evidence exists by transaction hash")
    void existsByTransactionHash_WhenEvidenceExists_ShouldReturnTrue() {
        // Arrange
        String txHash = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
        when(evidenceRepository.existsByTransactionHash(txHash)).thenReturn(true);
        
        // Act
        boolean result = evidenceService.existsByTransactionHash(txHash);
        
        // Assert
        assertTrue(result);
        
        // Verify interactions
        verify(evidenceRepository).existsByTransactionHash(txHash);
    }
    
    @Test
    @DisplayName("Should return count when counting by user address")
    void countByUserAddress_WhenEvidenceExists_ShouldReturnCount() {
        // Arrange
        String userAddress = "0x1234567890123456789012345678901234567890";
        when(evidenceRepository.countByUserAddress(userAddress)).thenReturn(5L);
        
        // Act
        long result = evidenceService.countByUserAddress(userAddress);
        
        // Assert
        assertEquals(5L, result);
        
        // Verify interactions
        verify(evidenceRepository).countByUserAddress(userAddress);
    }
    
    @Test
    @DisplayName("Should return count when counting by status")
    void countByStatus_WhenEvidenceExists_ShouldReturnCount() {
        // Arrange
        String status = "effective";
        when(evidenceRepository.countByStatus(status)).thenReturn(10L);
        
        // Act
        long result = evidenceService.countByStatus(status);
        
        // Assert
        assertEquals(10L, result);
        
        // Verify interactions
        verify(evidenceRepository).countByStatus(status);
    }
    
    @Test
    @DisplayName("Should return count when counting by user address and status")
    void countByUserAddressAndStatus_WhenEvidenceExists_ShouldReturnCount() {
        // Arrange
        String userAddress = "0x1234567890123456789012345678901234567890";
        String status = "effective";
        when(evidenceRepository.countByUserAddressAndStatus(userAddress, status)).thenReturn(3L);
        
        // Act
        long result = evidenceService.countByUserAddressAndStatus(userAddress, status);
        
        // Assert
        assertEquals(3L, result);
        
        // Verify interactions
        verify(evidenceRepository).countByUserAddressAndStatus(userAddress, status);
    }
    
    @Test
    @DisplayName("Should update evidence successfully when data is valid")
    void updateEvidence_WhenValidData_ShouldUpdateEvidence() {
        // Arrange
        evidence1.setStatus("verified");
        when(evidenceRepository.save(any(Evidence.class))).thenReturn(evidence1);
        
        // Act
        Evidence result = evidenceService.updateEvidence(evidence1);
        
        // Assert
        assertNotNull(result);
        assertEquals("verified", result.getStatus());
        
        // Verify interactions
        verify(evidenceRepository).save(evidence1);
    }
    
    @Test
    @DisplayName("Should throw exception when updating evidence with null data")
    void updateEvidence_WhenEvidenceIsNull_ShouldThrowException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> evidenceService.updateEvidence(null)
        );

        assertEquals("Evidence cannot be null", exception.getMessage());
        
        // Verify no interactions
        verifyNoInteractions(evidenceRepository);
    }
    
    @Test
    @DisplayName("Should delete evidence successfully when evidence exists")
    void deleteEvidence_WhenEvidenceExists_ShouldDeleteEvidence() {
        // Arrange
        when(evidenceRepository.existsById(1L)).thenReturn(true);
        doNothing().when(evidenceRepository).deleteById(1L);
        
        // Act
        evidenceService.deleteEvidence(1L);
        
        // Assert
        verify(evidenceRepository).existsById(1L);
        verify(evidenceRepository).deleteById(1L);
    }
    
    @Test
    @DisplayName("Should throw exception when deleting evidence that does not exist")
    void deleteEvidence_WhenEvidenceDoesNotExist_ShouldThrowException() {
        // Arrange
        when(evidenceRepository.existsById(999L)).thenReturn(false);
        
        // Act & Assert
        EvidenceNotFoundException exception = assertThrows(
            EvidenceNotFoundException.class,
            () -> evidenceService.deleteEvidence(999L)
        );

        assertEquals("Evidence not found with ID: 999", exception.getMessage());
        
        // Verify interactions
        verify(evidenceRepository).existsById(999L);
        verify(evidenceRepository, never()).deleteById(any());
    }
    
    @Test
    @DisplayName("Should delete evidence by evidence ID successfully when evidence exists")
    void deleteEvidenceByEvidenceId_WhenEvidenceExists_ShouldDeleteEvidence() {
        // Arrange
        String evidenceId = "EVID:1234567890:CN-001";
        when(evidenceRepository.findByEvidenceId(evidenceId)).thenReturn(Optional.of(evidence1));
        doNothing().when(evidenceRepository).delete(any(Evidence.class));
        
        // Act
        evidenceService.deleteEvidenceByEvidenceId(evidenceId);
        
        // Assert
        verify(evidenceRepository).findByEvidenceId(evidenceId);
        verify(evidenceRepository).delete(evidence1);
    }
    
    @Test
    @DisplayName("Should throw exception when deleting evidence by evidence ID that does not exist")
    void deleteEvidenceByEvidenceId_WhenEvidenceDoesNotExist_ShouldThrowException() {
        // Arrange
        String evidenceId = "NONEXISTENT";
        when(evidenceRepository.findByEvidenceId(evidenceId)).thenReturn(Optional.empty());
        
        // Act & Assert
        EvidenceNotFoundException exception = assertThrows(
            EvidenceNotFoundException.class,
            () -> evidenceService.deleteEvidenceByEvidenceId(evidenceId)
        );

        assertEquals("Evidence not found: NONEXISTENT", exception.getMessage());
        
        // Verify interactions
        verify(evidenceRepository).findByEvidenceId(evidenceId);
        verify(evidenceRepository, never()).delete(any());
    }
    
    @Test
    @DisplayName("Should return total count of evidence")
    void count_WhenEvidenceExists_ShouldReturnTotalCount() {
        // Arrange
        when(evidenceRepository.count()).thenReturn(25L);
        
        // Act
        long result = evidenceService.count();
        
        // Assert
        assertEquals(25L, result);
        
        // Verify interactions
        verify(evidenceRepository).count();
    }
    
    @Test
    @DisplayName("Should handle repository exception during evidence creation")
    void createEvidence_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Arrange
        when(evidenceRepository.save(any(Evidence.class))).thenThrow(new RuntimeException("Database error"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> evidenceService.createEvidence(evidence1)
        );

        assertEquals("Database error", exception.getMessage());
        
        // Verify interactions
        verify(evidenceRepository).save(evidence1);
    }
    
    @Test
    @DisplayName("Should handle repository exception during evidence lookup by ID")
    void getEvidenceById_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Arrange
        when(evidenceRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> evidenceService.getEvidenceById(1L)
        );

        assertEquals("Database error", exception.getMessage());
        
        // Verify interactions
        verify(evidenceRepository).findById(1L);
    }
}