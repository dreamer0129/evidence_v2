package cn.edu.gfkd.evidence.service;

import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.edu.gfkd.evidence.entity.EvidenceEntity;
import cn.edu.gfkd.evidence.exception.CertificateGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service @RequiredArgsConstructor @Slf4j
public class CertificateStorageService {

    private final CertificateService certificateService;

    @Transactional
    public EvidenceEntity generateAndStoreCertificate(EvidenceEntity evidence)
            throws CertificateGenerationException {
        log.debug("Generating and storing certificate for evidenceId: {}",
                evidence.getEvidenceId());

        try {
            // Generate certificate (CertificateService now handles storage internally)
            String certificateId = certificateService.generateCertificate(evidence);

            // Update evidence entity with the certificate ID
            evidence.setCertificateId(certificateId);

            log.info("Certificate generated and stored for evidenceId: {}, certificateId: {}",
                    evidence.getEvidenceId(), certificateId);

            return evidence;

        } catch (CertificateGenerationException e) {
            log.error("Failed to generate certificate for evidence {}: {}",
                    evidence.getEvidenceId(), e.getMessage(), e);
            throw e;
        }
    }

    public boolean deleteCertificate(String certificateId) {
        if (certificateId == null || certificateId.isEmpty()) {
            return false;
        }

        try {
            // Delegate to CertificateService
            return certificateService.deleteCertificate(certificateId);
        } catch (Exception e) {
            log.error("Failed to delete certificate {}: {}", certificateId, e.getMessage(), e);
            return false;
        }
    }

    public boolean certificateExists(String certificateId) {
        return certificateService.certificateExists(certificateId);
    }

    public byte[] getCertificateBytes(String certificateId) throws IOException {
        return certificateService.getCertificateBytes(certificateId);
    }

    public long getCertificateFileSize(String certificateId) throws IOException {
        return certificateService.getCertificateFileSize(certificateId);
    }
}