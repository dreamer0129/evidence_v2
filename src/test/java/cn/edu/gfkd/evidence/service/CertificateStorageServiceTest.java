package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.entity.EvidenceEntity;
import cn.edu.gfkd.evidence.exception.CertificateGenerationException;
import cn.edu.gfkd.evidence.service.storage.CertificateStorage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateStorageServiceTest {

    @Mock
    private CertificateService certificateService;
    
    @Mock
    private CertificateStorage certificateStorage;

    private CertificateStorageService certificateStorageService;

    @BeforeEach
    void setUp() {
        certificateStorageService = new CertificateStorageService(certificateService, certificateStorage);
    }

    @Test
    void testGenerateAndStoreCertificate_Success() throws CertificateGenerationException, IOException {
        // Given
        EvidenceEntity evidence = createTestEvidence();
        String tempPath = "./temp/certificate_test-evidence-123.pdf";
        String certificateId = "cert_test-evidence-123";
        byte[] certificateBytes = new byte[]{1, 2, 3, 4, 5};
        
        when(certificateService.generateCertificate(any(EvidenceEntity.class)))
                .thenReturn(tempPath);
        when(certificateService.getCertificateBytes(tempPath))
                .thenReturn(certificateBytes);
        when(certificateStorage.storeCertificate(any(EvidenceEntity.class), any(InputStream.class)))
                .thenReturn(certificateId);

        // When
        EvidenceEntity result = certificateStorageService.generateAndStoreCertificate(evidence);

        // Then
        assertNotNull(result);
        assertEquals(certificateId, result.getCertificateId());
        verify(certificateService, times(1)).generateCertificate(evidence);
        verify(certificateService, times(1)).getCertificateBytes(tempPath);
        verify(certificateStorage, times(1)).storeCertificate(any(EvidenceEntity.class), any(InputStream.class));
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
    void testDeleteCertificate_Success() throws IOException {
        // Given
        String certificateId = "cert_test-evidence-123";
        
        when(certificateStorage.deleteCertificate(certificateId)).thenReturn(true);

        // When
        boolean result = certificateStorageService.deleteCertificate(certificateId);

        // Then
        assertTrue(result);
        verify(certificateStorage, times(1)).deleteCertificate(certificateId);
    }

    @Test
    void testDeleteCertificate_FileDoesNotExist() throws IOException {
        // Given
        String certificateId = "cert_test-evidence-123";
        
        when(certificateStorage.deleteCertificate(certificateId)).thenReturn(false);

        // When
        boolean result = certificateStorageService.deleteCertificate(certificateId);

        // Then
        assertFalse(result);
        verify(certificateStorage, times(1)).deleteCertificate(certificateId);
    }

    @Test
    void testDeleteCertificate_NullPath() throws IOException {
        // When
        boolean result = certificateStorageService.deleteCertificate(null);

        // Then
        assertFalse(result);
        verify(certificateStorage, never()).deleteCertificate(any());
    }

    @Test
    void testCertificateExists() {
        // Given
        String certificateId = "cert_test-evidence-123";
        
        when(certificateStorage.certificateExists(certificateId)).thenReturn(true);

        // When
        boolean result = certificateStorageService.certificateExists(certificateId);

        // Then
        assertTrue(result);
        verify(certificateStorage, times(1)).certificateExists(certificateId);
    }

    @Test
    void testGetCertificateBytes() throws Exception {
        // Given
        String certificateId = "cert_test-evidence-123";
        byte[] expectedBytes = new byte[]{1, 2, 3, 4, 5};
        InputStream expectedStream = new ByteArrayInputStream(expectedBytes);
        
        when(certificateStorage.getCertificate(certificateId))
                .thenReturn(Optional.of(expectedStream));

        // When
        byte[] result = certificateStorageService.getCertificateBytes(certificateId);

        // Then
        assertArrayEquals(expectedBytes, result);
        verify(certificateStorage, times(1)).getCertificate(certificateId);
    }

    @Test
    void testGetCertificateBytes_NotFound() throws Exception {
        // Given
        String certificateId = "cert_test-evidence-123";
        
        when(certificateStorage.getCertificate(certificateId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(IOException.class, () -> {
            certificateStorageService.getCertificateBytes(certificateId);
        });
    }

    @Test
    void testGetCertificateFileSize() throws Exception {
        // Given
        String certificateId = "cert_test-evidence-123";
        long expectedSize = 1024L;
        
        when(certificateStorage.getCertificateSize(certificateId)).thenReturn(expectedSize);

        // When
        long result = certificateStorageService.getCertificateFileSize(certificateId);

        // Then
        assertEquals(expectedSize, result);
        verify(certificateStorage, times(1)).getCertificateSize(certificateId);
    }

    @Test
    void testGetCertificatePublicUrl() {
        // Given
        String certificateId = "cert_test-evidence-123";
        String expectedUrl = "/certificates/cert_test-evidence-123.pdf";
        
        when(certificateStorage.getPublicUrl(certificateId))
                .thenReturn(Optional.of(expectedUrl));

        // When
        Optional<String> result = certificateStorageService.getCertificatePublicUrl(certificateId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(expectedUrl, result.get());
        verify(certificateStorage, times(1)).getPublicUrl(certificateId);
    }

    @Test
    void testGetStorageType() {
        // Given
        String expectedType = "filesystem";
        
        when(certificateStorage.getStorageType()).thenReturn(expectedType);

        // When
        String result = certificateStorageService.getStorageType();

        // Then
        assertEquals(expectedType, result);
        verify(certificateStorage, times(1)).getStorageType();
    }

    private EvidenceEntity createTestEvidence() {
        EvidenceEntity evidence = new EvidenceEntity();
        evidence.setEvidenceId("test-evidence-123");
        evidence.setUserAddress("0x1234567890123456789012345678901234567890");
        evidence.setFileName("test-file.pdf");
        evidence.setMimeType("application/pdf");
        evidence.setFileSize(1024L);
        evidence.setFileCreationTime(BigInteger.valueOf(1640995200L));
        evidence.setHashAlgorithm("SHA-256");
        evidence.setHashValue("0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
        evidence.setBlockNumber(BigInteger.valueOf(12345L));
        evidence.setTransactionHash("0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890");
        evidence.setBlockTimestamp(BigInteger.valueOf(1640995200L));
        evidence.setMemo("Test evidence");
        evidence.setStatus("effective");
        evidence.setCreatedAt(LocalDateTime.now());
        evidence.setUpdatedAt(LocalDateTime.now());
        return evidence;
    }
}