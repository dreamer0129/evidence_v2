package cn.edu.gfkd.evidence.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cn.edu.gfkd.evidence.entity.EvidenceEntity;
import cn.edu.gfkd.evidence.exception.EvidenceNotFoundException;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service @RequiredArgsConstructor @Slf4j
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;

    public EvidenceEntity createEvidence(EvidenceEntity evidence) {
        validateEvidence(evidence);
        log.info("Creating new evidence for user: {}", evidence.getUserAddress());
        EvidenceEntity savedEvidence = evidenceRepository.save(evidence);
        log.info("Successfully created evidence with ID: {}", savedEvidence.getId());
        return savedEvidence;
    }

    private void validateEvidence(EvidenceEntity evidence) {
        if (evidence == null) {
            throw new IllegalArgumentException("Evidence cannot be null");
        }
        if (!StringUtils.hasText(evidence.getEvidenceId())) {
            throw new IllegalArgumentException("Evidence ID cannot be empty");
        }
        if (!StringUtils.hasText(evidence.getUserAddress())) {
            throw new IllegalArgumentException("User address cannot be empty");
        }
        if (!StringUtils.hasText(evidence.getHashValue())) {
            throw new IllegalArgumentException("Hash value cannot be empty");
        }
    }

    public Optional<EvidenceEntity> getEvidenceById(Long id) {
        return evidenceRepository.findById(id);
    }

    public Optional<EvidenceEntity> getEvidenceByEvidenceId(String evidenceId) {
        return evidenceRepository.findByEvidenceId(evidenceId);
    }

    public Optional<EvidenceEntity> getEvidenceByTransactionHash(String transactionHash) {
        return evidenceRepository.findByTransactionHash(transactionHash);
    }

    public List<EvidenceEntity> getEvidenceByUserAddress(String userAddress) {
        return evidenceRepository.findByUserAddress(userAddress);
    }

    public List<EvidenceEntity> getEvidenceByHashValue(String hashValue) {
        return evidenceRepository.findByHashValue(hashValue);
    }

    public List<EvidenceEntity> getEvidenceByStatus(String status) {
        return evidenceRepository.findByStatus(status);
    }

    public Page<EvidenceEntity> getEvidenceByUserAddress(String userAddress, Pageable pageable) {
        return evidenceRepository.findByUserAddress(userAddress, pageable);
    }

    public Page<EvidenceEntity> getEvidenceByStatus(String status, Pageable pageable) {
        return evidenceRepository.findByStatus(status, pageable);
    }

    public Page<EvidenceEntity> searchEvidence(String evidenceId, String userAddress, String status,
            Pageable pageable) {
        return evidenceRepository.findByFilters(evidenceId, userAddress, status, pageable);
    }

    public long countByUserAddress(String userAddress) {
        return evidenceRepository.countByUserAddress(userAddress);
    }

    public long countByStatus(String status) {
        return evidenceRepository.countByStatus(status);
    }

    public boolean existsByEvidenceId(String evidenceId) {
        return evidenceRepository.existsByEvidenceId(evidenceId);
    }

    public boolean existsByTransactionHash(String transactionHash) {
        return evidenceRepository.existsByTransactionHash(transactionHash);
    }

    public EvidenceEntity updateEvidence(EvidenceEntity evidence) {
        validateEvidence(evidence);
        log.info("Updating evidence with ID: {}", evidence.getId());
        return evidenceRepository.save(evidence);
    }

    public void deleteEvidence(Long id) {
        if (!evidenceRepository.existsById(id)) {
            throw new EvidenceNotFoundException("Evidence not found with ID: " + id);
        }
        evidenceRepository.deleteById(id);
        log.info("Deleted evidence with ID: {}", id);
    }

    public void deleteEvidenceByEvidenceId(String evidenceId) {
        EvidenceEntity evidence = evidenceRepository.findByEvidenceId(evidenceId).orElseThrow(
                () -> new EvidenceNotFoundException("Evidence not found: " + evidenceId));
        evidenceRepository.delete(evidence);
        log.info("Deleted evidence with evidence ID: {}", evidenceId);
    }

    // Additional methods for statistics
    public long count() {
        return evidenceRepository.count();
    }

    // Fixed N+1 query problem by using repository method instead of stream filter

    public long countByUserAddressAndStatus(String userAddress, String status) {
        return evidenceRepository.countByUserAddressAndStatus(userAddress, status);
    }

    // Removed countByUserAddressAndVerified as isVerified field is no longer stored
}