package cn.edu.gfkd.evidence.repository;

import cn.edu.gfkd.evidence.entity.Evidence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {
    
    Optional<Evidence> findByEvidenceId(String evidenceId);
    
    Optional<Evidence> findByTransactionHash(String transactionHash);
    
    List<Evidence> findByUserAddress(String userAddress);
    
    List<Evidence> findByHashValue(String hashValue);
    
    List<Evidence> findByStatus(String status);
    
    Page<Evidence> findByUserAddress(String userAddress, Pageable pageable);
    
    Page<Evidence> findByStatus(String status, Pageable pageable);
    
    @Query("SELECT e FROM Evidence e WHERE " +
           "(:evidenceId IS NULL OR e.evidenceId LIKE %:evidenceId%) AND " +
           "(:userAddress IS NULL OR e.userAddress = :userAddress) AND " +
           "(:status IS NULL OR e.status = :status)")
    Page<Evidence> findByFilters(@Param("evidenceId") String evidenceId,
                                @Param("userAddress") String userAddress,
                                @Param("status") String status,
                                Pageable pageable);
    
    @Query("SELECT MAX(e.blockNumber) FROM Evidence e")
    BigInteger findMaxBlockNumber();
    
    boolean existsByEvidenceId(String evidenceId);
    
    boolean existsByTransactionHash(String transactionHash);
    
    long countByUserAddress(String userAddress);
    
    long countByStatus(String status);
}