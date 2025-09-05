package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.entity.EvidenceEntity;
import cn.edu.gfkd.evidence.exception.CertificateGenerationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateStorageServiceTest {

    @Mock
    private CertificateService certificateService;

    private CertificateStorageService certificateStorageService;

    @BeforeEach
    void setUp() {
        certificateStorageService = new CertificateStorageService(certificateService);
    }

    @Test
    void testGenerateAndStoreCertificate_Success() throws CertificateGenerationException, IOException {
        // Given
        EvidenceEntity evidence = createTestEvidence();
        String certificateId = "cert_test-evidence-123";
        
        when(certificateService.generateCertificate(any(EvidenceEntity.class)))
                .thenReturn(certificateId);

        // When
        EvidenceEntity result = certificateStorageService.generateAndStoreCertificate(evidence);

        // Then
        assertNotNull(result);
        assertEquals(certificateId, result.getCertificateId());
        verify(certificateService, times(1)).generateCertificate(evidence);
    }

    @Test
    void testGenerateAndStoreCertificate_Failure() throws CertificateGenerationException {
        // Given
        EvidenceEntity evidence = createTestEvidence();
        
        when(certificateService.generateCertificate(any(EvidenceEntity.class)))
                .thenThrow(new CertificateGenerationException("Template not found"));

        // When & Then
        assertThrows(CertificateGenerationException.class, () -> {
            certificateStorageService.generateAndStoreCertificate(evidence);
        });
        
        verify(certificateService, times(1)).generateCertificate(evidence);
    }

    @Test
    void testCertificateExists_True() {
        // Given
        String certificateId = "cert_test-evidence-123";
        
        when(certificateService.certificateExists(certificateId))
                .thenReturn(true);

        // When
        boolean result = certificateStorageService.certificateExists(certificateId);

        // Then
        assertTrue(result);
        verify(certificateService, times(1)).certificateExists(certificateId);
    }

    @Test
    void testCertificateExists_False() {
        // Given
        String certificateId = "cert_nonexistent";
        
        when(certificateService.certificateExists(certificateId))
                .thenReturn(false);

        // When
        boolean result = certificateStorageService.certificateExists(certificateId);

        // Then
        assertFalse(result);
        verify(certificateService, times(1)).certificateExists(certificateId);
    }

    @Test
    void testCertificateExists_Null() {
        // Given
        String certificateId = null;

        // When
        boolean result = certificateStorageService.certificateExists(certificateId);

        // Then
        assertFalse(result);
        verify(certificateService, times(1)).certificateExists(certificateId);
    }

    @Test
    void testCertificateExists_Empty() {
        // Given
        String certificateId = "";

        // When
        boolean result = certificateStorageService.certificateExists(certificateId);

        // Then
        assertFalse(result);
        verify(certificateService, times(1)).certificateExists(certificateId);
    }

    @Test
    void testGetCertificateBytes_Success() throws IOException {
        // Given
        String certificateId = "cert_test-evidence-123";
        byte[] expectedBytes = new byte[]{1, 2, 3, 4, 5};
        
        when(certificateService.getCertificateBytes(certificateId))
                .thenReturn(expectedBytes);

        // When
        byte[] result = certificateStorageService.getCertificateBytes(certificateId);

        // Then
        assertArrayEquals(expectedBytes, result);
        verify(certificateService, times(1)).getCertificateBytes(certificateId);
    }

    @Test
    void testGetCertificateBytes_NotFound() throws IOException {
        // Given
        String certificateId = "cert_nonexistent";
        
        when(certificateService.getCertificateBytes(certificateId))
                .thenThrow(new IOException("Certificate not found"));

        // When & Then
        assertThrows(IOException.class, () -> {
            certificateStorageService.getCertificateBytes(certificateId);
        });
        
        verify(certificateService, times(1)).getCertificateBytes(certificateId);
    }

    @Test
    void testGetCertificateFileSize_Success() throws IOException {
        // Given
        String certificateId = "cert_test-evidence-123";
        long expectedSize = 1024L;
        
        when(certificateService.getCertificateFileSize(certificateId))
                .thenReturn(expectedSize);

        // When
        long result = certificateStorageService.getCertificateFileSize(certificateId);

        // Then
        assertEquals(expectedSize, result);
        verify(certificateService, times(1)).getCertificateFileSize(certificateId);
    }

    @Test
    void testDeleteCertificate_Success() {
        // Given
        String certificateId = "cert_test-evidence-123";
        
        when(certificateService.deleteCertificate(certificateId))
                .thenReturn(true);

        // When
        boolean result = certificateStorageService.deleteCertificate(certificateId);

        // Then
        assertTrue(result);
        verify(certificateService, times(1)).deleteCertificate(certificateId);
    }

    @Test
    void testDeleteCertificate_Failure() {
        // Given
        String certificateId = "cert_test-evidence-123";
        
        when(certificateService.deleteCertificate(certificateId))
                .thenReturn(false);

        // When
        boolean result = certificateStorageService.deleteCertificate(certificateId);

        // Then
        assertFalse(result);
        verify(certificateService, times(1)).deleteCertificate(certificateId);
    }

    @Test
    void testDeleteCertificate_Null() {
        // Given
        String certificateId = null;

        // When
        boolean result = certificateStorageService.deleteCertificate(certificateId);

        // Then
        assertFalse(result);
        verify(certificateService, never()).deleteCertificate(any());
    }

    @Test
    void testDeleteCertificate_Empty() {
        // Given
        String certificateId = "";

        // When
        boolean result = certificateStorageService.deleteCertificate(certificateId);

        // Then
        assertFalse(result);
        verify(certificateService, never()).deleteCertificate(any());
    }

    private EvidenceEntity createTestEvidence() {
        EvidenceEntity evidence = new EvidenceEntity();
        evidence.setId(1L);
        evidence.setEvidenceId("test-evidence-123");
        evidence.setUserAddress("0x1234567890123456789012345678901234567890");
        evidence.setFileName("test.pdf");
        evidence.setMimeType("application/pdf");
        evidence.setFileSize(1024L);
        evidence.setFileCreationTime(BigInteger.valueOf(1234567890L));
        evidence.setHashAlgorithm("SHA-256");
        evidence.setHashValue("abc123def456");
        evidence.setStatus("CONFIRMED");
        evidence.setBlockNumber(BigInteger.valueOf(1000000L));
        evidence.setTransactionHash("0xabcdef1234567890");
        evidence.setBlockTimestamp(BigInteger.valueOf(1234567890L));
        evidence.setMemo("Test evidence");
        evidence.setCreatedAt(LocalDateTime.now());
        evidence.setUpdatedAt(LocalDateTime.now());
        return evidence;
    }
}