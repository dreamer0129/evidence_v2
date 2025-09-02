package cn.edu.gfkd.evidence.controller;

import cn.edu.gfkd.evidence.dto.ApiResponse;
import cn.edu.gfkd.evidence.dto.EvidenceDTO;
import cn.edu.gfkd.evidence.entity.Evidence;
import cn.edu.gfkd.evidence.service.EvidenceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/evidence")
@CrossOrigin(origins = "*", maxAge = 3600)
public class EvidenceController {
    
    @Autowired
    private EvidenceService evidenceService;
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EvidenceDTO>> getEvidenceById(@PathVariable Long id) {
        Evidence evidence = evidenceService.getEvidenceById(id)
            .orElseThrow(() -> new RuntimeException("Evidence not found with id: " + id));
        
        EvidenceDTO evidenceDTO = convertToDTO(evidence);
        
        return ResponseEntity.ok(ApiResponse.success(evidenceDTO));
    }
    
    @GetMapping("/evidenceId/{evidenceId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EvidenceDTO>> getEvidenceByEvidenceId(@PathVariable String evidenceId) {
        Evidence evidence = evidenceService.getEvidenceByEvidenceId(evidenceId)
            .orElseThrow(() -> new RuntimeException("Evidence not found with evidenceId: " + evidenceId));
        
        EvidenceDTO evidenceDTO = convertToDTO(evidence);
        
        return ResponseEntity.ok(ApiResponse.success(evidenceDTO));
    }
    
    @GetMapping("/transaction/{transactionHash}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EvidenceDTO>> getEvidenceByTransactionHash(@PathVariable String transactionHash) {
        Evidence evidence = evidenceService.getEvidenceByTransactionHash(transactionHash)
            .orElseThrow(() -> new RuntimeException("Evidence not found with transaction hash: " + transactionHash));
        
        EvidenceDTO evidenceDTO = convertToDTO(evidence);
        
        return ResponseEntity.ok(ApiResponse.success(evidenceDTO));
    }
    
    @GetMapping("/user/{userAddress}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<EvidenceDTO>>> getEvidenceByUserAddress(
            @PathVariable String userAddress,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        
        Pageable pageable = createPageable(page, size, sort);
        Page<Evidence> evidencePage = evidenceService.getEvidenceByUserAddress(userAddress, pageable);
        Page<EvidenceDTO> dtoPage = evidencePage.map(this::convertToDTO);
        
        return ResponseEntity.ok(ApiResponse.success(dtoPage));
    }
    
    @GetMapping("/hash/{hashValue}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<EvidenceDTO>>> getEvidenceByHashValue(@PathVariable String hashValue) {
        List<Evidence> evidenceList = evidenceService.getEvidenceByHashValue(hashValue);
        List<EvidenceDTO> dtoList = evidenceList.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(dtoList));
    }
    
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<EvidenceDTO>>> getEvidenceByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        
        Pageable pageable = createPageable(page, size, sort);
        Page<Evidence> evidencePage = evidenceService.getEvidenceByStatus(status, pageable);
        Page<EvidenceDTO> dtoPage = evidencePage.map(this::convertToDTO);
        
        return ResponseEntity.ok(ApiResponse.success(dtoPage));
    }
    
    @GetMapping("/search")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<EvidenceDTO>>> searchEvidence(
            @RequestParam(required = false) String evidenceId,
            @RequestParam(required = false) String userAddress,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        
        Pageable pageable = createPageable(page, size, sort);
        Page<Evidence> evidencePage = evidenceService.searchEvidence(evidenceId, userAddress, status, pageable);
        Page<EvidenceDTO> dtoPage = evidencePage.map(this::convertToDTO);
        
        return ResponseEntity.ok(ApiResponse.success(dtoPage));
    }
    
    @GetMapping("/stats/user/{userAddress}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EvidenceStatsDTO>> getEvidenceStatsByUser(@PathVariable String userAddress) {
        long totalCount = evidenceService.countByUserAddress(userAddress);
        long effectiveCount = evidenceService.countByUserAddressAndStatus(userAddress, "effective");
        long verifiedCount = 0; // Verification is not stored as it's just an operation
        
        EvidenceStatsDTO stats = new EvidenceStatsDTO(totalCount, effectiveCount, verifiedCount);
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    @GetMapping("/stats/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OverviewStatsDTO>> getOverviewStats() {
        long totalCount = evidenceService.count();
        long effectiveCount = evidenceService.countByStatus("effective");
        long verifiedCount = 0; // Verification is not stored as it's just an operation
        long revokedCount = evidenceService.countByStatus("revoked");
        
        OverviewStatsDTO stats = new OverviewStatsDTO(totalCount, effectiveCount, verifiedCount, revokedCount);
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    private Pageable createPageable(int page, int size, String sort) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1]) 
            ? Sort.Direction.DESC : Sort.Direction.ASC;
        
        return PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
    }
    
    private EvidenceDTO convertToDTO(Evidence evidence) {
        EvidenceDTO dto = new EvidenceDTO();
        dto.setId(evidence.getId());
        dto.setEvidenceId(evidence.getEvidenceId());
        dto.setUserAddress(evidence.getUserAddress());
        
        // File metadata
        dto.setFileName(evidence.getFileName());
        dto.setMimeType(evidence.getMimeType());
        dto.setFileSize(evidence.getFileSize());
        dto.setFileCreationTime(evidence.getFileCreationTime());
        
        // Hash information
        dto.setHashAlgorithm(evidence.getHashAlgorithm());
        dto.setHashValue(evidence.getHashValue());
        
        dto.setStatus(evidence.getStatus());
        dto.setBlockNumber(evidence.getBlockNumber());
        dto.setTransactionHash(evidence.getTransactionHash());
        dto.setBlockTimestamp(evidence.getBlockTimestamp());
        dto.setMemo(evidence.getMemo());
        dto.setRevokedAt(evidence.getRevokedAt());
        dto.setRevokerAddress(evidence.getRevokerAddress());
        dto.setCreatedAt(evidence.getCreatedAt());
        dto.setUpdatedAt(evidence.getUpdatedAt());
        
        return dto;
    }
    
        
    // DTO classes
    public static class EvidenceStatsDTO {
        private long totalCount;
        private long effectiveCount;
        private long verifiedCount;
        
        public EvidenceStatsDTO(long totalCount, long effectiveCount, long verifiedCount) {
            this.totalCount = totalCount;
            this.effectiveCount = effectiveCount;
            this.verifiedCount = verifiedCount;
        }
        
        // Getters
        public long getTotalCount() { return totalCount; }
        public long getEffectiveCount() { return effectiveCount; }
        public long getVerifiedCount() { return verifiedCount; }
    }
    
    public static class OverviewStatsDTO {
        private long totalCount;
        private long effectiveCount;
        private long verifiedCount;
        private long revokedCount;
        
        public OverviewStatsDTO(long totalCount, long effectiveCount, long verifiedCount, long revokedCount) {
            this.totalCount = totalCount;
            this.effectiveCount = effectiveCount;
            this.verifiedCount = verifiedCount;
            this.revokedCount = revokedCount;
        }
        
        // Getters
        public long getTotalCount() { return totalCount; }
        public long getEffectiveCount() { return effectiveCount; }
        public long getVerifiedCount() { return verifiedCount; }
        public long getRevokedCount() { return revokedCount; }
    }
}