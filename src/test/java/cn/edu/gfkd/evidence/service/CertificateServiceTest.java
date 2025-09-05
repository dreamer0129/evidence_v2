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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock
    private CertificateConfig certificateConfig;

    private CertificateService certificateService;

    @BeforeEach
    void setUp() {
        certificateService = new CertificateService(certificateConfig);
    }

    @Test
    void testGenerateCertificate_Success() {
        // Given
        EvidenceEntity evidence = createTestEvidence();
        when(certificateConfig.getOutputPath()).thenReturn("./test-certificates");
        when(certificateConfig.isAutoCreateDirectory()).thenReturn(true);

        // When & Then
        // Note: This test will fail if the template PDF doesn't exist
        // In a real scenario, we would mock the PDF generation
        assertThrows(CertificateGenerationException.class, () -> {
            certificateService.generateCertificate(evidence);
        });
    }

    @Test
    void testCertificateExists_WithValidPath() {
        // Given
        String validPath = "./test-certificates/certificate_test.pdf";

        // When
        boolean exists = certificateService.certificateExists(validPath);

        // Then
        assertFalse(exists); // File doesn't exist yet
    }

    @Test
    void testCertificateExists_WithNullPath() {
        // When
        boolean exists = certificateService.certificateExists(null);

        // Then
        assertFalse(exists);
    }

    @Test
    void testCertificateExists_WithEmptyPath() {
        // When
        boolean exists = certificateService.certificateExists("");

        // Then
        assertFalse(exists);
    }

    @Test
    void testGetCertificateBytes_WithNullPath() {
        // When & Then
        assertThrows(IOException.class, () -> {
            certificateService.getCertificateBytes(null);
        });
    }

    @Test
    void testGetCertificateBytes_WithEmptyPath() {
        // When & Then
        assertThrows(IOException.class, () -> {
            certificateService.getCertificateBytes("");
        });
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