package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.entity.Evidence;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EvidenceService {
    
    @Autowired
    private EvidenceRepository evidenceRepository;
    
    public Evidence createEvidence(Evidence evidence) {
        return evidenceRepository.save(evidence);
    }
    
    public Optional<Evidence> getEvidenceById(Long id) {
        return evidenceRepository.findById(id);
    }
    
    public Optional<Evidence> getEvidenceByEvidenceId(String evidenceId) {
        return evidenceRepository.findByEvidenceId(evidenceId);
    }
    
    public Optional<Evidence> getEvidenceByTransactionHash(String transactionHash) {
        return evidenceRepository.findByTransactionHash(transactionHash);
    }
    
    public List<Evidence> getEvidenceByUserAddress(String userAddress) {
        return evidenceRepository.findByUserAddress(userAddress);
    }
    
    public List<Evidence> getEvidenceByHashValue(String hashValue) {
        return evidenceRepository.findByHashValue(hashValue);
    }
    
    public List<Evidence> getEvidenceByStatus(String status) {
        return evidenceRepository.findByStatus(status);
    }
    
    public Page<Evidence> getEvidenceByUserAddress(String userAddress, Pageable pageable) {
        return evidenceRepository.findByUserAddress(userAddress, pageable);
    }
    
    public Page<Evidence> getEvidenceByStatus(String status, Pageable pageable) {
        return evidenceRepository.findByStatus(status, pageable);
    }
    
    public Page<Evidence> searchEvidence(String evidenceId, String userAddress, 
                                        String status, Pageable pageable) {
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
    
    public Evidence updateEvidence(Evidence evidence) {
        return evidenceRepository.save(evidence);
    }
    
    public void deleteEvidence(Long id) {
        evidenceRepository.deleteById(id);
    }
    
    public void deleteEvidenceByEvidenceId(String evidenceId) {
        Evidence evidence = evidenceRepository.findByEvidenceId(evidenceId)
            .orElseThrow(() -> new RuntimeException("Evidence not found: " + evidenceId));
        evidenceRepository.delete(evidence);
    }
    
    // Additional methods for statistics
    public long count() {
        return evidenceRepository.count();
    }
    
    // Removed countByVerified and findByIsVerified as isVerified field is no longer stored
    
    public long countByUserAddressAndStatus(String userAddress, String status) {
        return evidenceRepository.findByUserAddress(userAddress).stream()
            .filter(e -> status.equals(e.getStatus()))
            .count();
    }
    
    // Removed countByUserAddressAndVerified as isVerified field is no longer stored
}