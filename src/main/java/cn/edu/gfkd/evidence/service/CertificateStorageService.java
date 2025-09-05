package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.entity.EvidenceEntity;
import cn.edu.gfkd.evidence.exception.CertificateGenerationException;
import cn.edu.gfkd.evidence.service.storage.CertificateStorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateStorageService {
    
    private final CertificateService certificateService;
    private final CertificateStorage certificateStorage;
    
    @Transactional
    public EvidenceEntity generateAndStoreCertificate(EvidenceEntity evidence) throws CertificateGenerationException {
        log.debug("Generating and storing certificate for evidenceId: {}", evidence.getEvidenceId());
        
        try {
            // Generate certificate
            String certificatePath = certificateService.generateCertificate(evidence);
            
            // Read the generated certificate and store using the abstraction
            byte[] certificateBytes = certificateService.getCertificateBytes(certificatePath);
            InputStream certificateStream = new ByteArrayInputStream(certificateBytes);
            
            // Store using the abstract storage
            String certificateId = certificateStorage.storeCertificate(evidence, certificateStream);
            
            // Update evidence entity with the certificate ID
            evidence.setCertificateId(certificateId);
            
            // Clean up the temporary file if it exists
            try {
                Path tempPath = Paths.get(certificatePath);
                if (Files.exists(tempPath)) {
                    Files.delete(tempPath);
                }
            } catch (IOException e) {
                log.warn("Failed to clean up temporary certificate file {}: {}", certificatePath, e.getMessage());
            }
            
            log.info("Certificate generated and stored for evidenceId: {}, certificateId: {}", 
                    evidence.getEvidenceId(), certificateId);
            
            return evidence;
            
        } catch (CertificateGenerationException e) {
            log.error("Failed to generate certificate for evidence {}: {}", 
                    evidence.getEvidenceId(), e.getMessage(), e);
            throw e;
        } catch (IOException e) {
            log.error("Failed to store certificate for evidence {}: {}", 
                    evidence.getEvidenceId(), e.getMessage(), e);
            throw new CertificateGenerationException("Failed to store certificate: " + e.getMessage(), e);
        }
    }
    
    public boolean deleteCertificate(String certificateId) {
        if (certificateId == null || certificateId.isEmpty()) {
            return false;
        }
        
        try {
            return certificateStorage.deleteCertificate(certificateId);
        } catch (IOException e) {
            log.error("Failed to delete certificate file {}: {}", certificateId, e.getMessage(), e);
            return false;
        }
    }
    
    public boolean certificateExists(String certificateId) {
        return certificateStorage.certificateExists(certificateId);
    }
    
    public byte[] getCertificateBytes(String certificateId) throws IOException {
        Optional<InputStream> certificateStream = certificateStorage.getCertificate(certificateId);
        if (certificateStream.isPresent()) {
            return certificateStream.get().readAllBytes();
        }
        throw new IOException("Certificate not found: " + certificateId);
    }
    
    public long getCertificateFileSize(String certificateId) throws IOException {
        return certificateStorage.getCertificateSize(certificateId);
    }
    
    public Optional<String> getCertificatePublicUrl(String certificateId) {
        return certificateStorage.getPublicUrl(certificateId);
    }
    
    public String getStorageType() {
        return certificateStorage.getStorageType();
    }
}