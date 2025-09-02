package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.entity.Evidence;
import cn.edu.gfkd.evidence.exception.EvidenceNotFoundException;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    
    public Evidence createEvidence(Evidence evidence) {
        validateEvidence(evidence);
        log.info("Creating new evidence for user: {}", evidence.getUserAddress());
        Evidence savedEvidence = evidenceRepository.save(evidence);
        log.info("Successfully created evidence with ID: {}", savedEvidence.getId());
        return savedEvidence;
    }
    
    private void validateEvidence(Evidence evidence) {
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
    
    @Transactional(readOnly = true)
    public Optional<Evidence> getEvidenceById(Long id) {
        return evidenceRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public Optional<Evidence> getEvidenceByEvidenceId(String evidenceId) {
        return evidenceRepository.findByEvidenceId(evidenceId);
    }
    
    @Transactional(readOnly = true)
    public Optional<Evidence> getEvidenceByTransactionHash(String transactionHash) {
        return evidenceRepository.findByTransactionHash(transactionHash);
    }
    
    @Transactional(readOnly = true)
    public List<Evidence> getEvidenceByUserAddress(String userAddress) {
        return evidenceRepository.findByUserAddress(userAddress);
    }
    
    @Transactional(readOnly = true)
    public List<Evidence> getEvidenceByHashValue(String hashValue) {
        return evidenceRepository.findByHashValue(hashValue);
    }
    
    @Transactional(readOnly = true)
    public List<Evidence> getEvidenceByStatus(String status) {
        return evidenceRepository.findByStatus(status);
    }
    
    @Transactional(readOnly = true)
    public Page<Evidence> getEvidenceByUserAddress(String userAddress, Pageable pageable) {
        return evidenceRepository.findByUserAddress(userAddress, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<Evidence> getEvidenceByStatus(String status, Pageable pageable) {
        return evidenceRepository.findByStatus(status, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<Evidence> searchEvidence(String evidenceId, String userAddress, 
                                        String status, Pageable pageable) {
        return evidenceRepository.findByFilters(evidenceId, userAddress, status, pageable);
    }
    
    @Transactional(readOnly = true)
    public long countByUserAddress(String userAddress) {
        return evidenceRepository.countByUserAddress(userAddress);
    }
    
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return evidenceRepository.countByStatus(status);
    }
    
    @Transactional(readOnly = true)
    public boolean existsByEvidenceId(String evidenceId) {
        return evidenceRepository.existsByEvidenceId(evidenceId);
    }
    
    @Transactional(readOnly = true)
    public boolean existsByTransactionHash(String transactionHash) {
        return evidenceRepository.existsByTransactionHash(transactionHash);
    }
    
    public Evidence updateEvidence(Evidence evidence) {
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
        Evidence evidence = evidenceRepository.findByEvidenceId(evidenceId)
            .orElseThrow(() -> new EvidenceNotFoundException("Evidence not found: " + evidenceId));
        evidenceRepository.delete(evidence);
        log.info("Deleted evidence with evidence ID: {}", evidenceId);
    }
    
    // Additional methods for statistics
    @Transactional(readOnly = true)
    public long count() {
        return evidenceRepository.count();
    }
    
    // Fixed N+1 query problem by using repository method instead of stream filter
    @Transactional(readOnly = true)
    public long countByUserAddressAndStatus(String userAddress, String status) {
        return evidenceRepository.countByUserAddressAndStatus(userAddress, status);
    }
    
    // Removed countByUserAddressAndVerified as isVerified field is no longer stored
}