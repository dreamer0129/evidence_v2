package cn.edu.gfkd.evidence.repository;

import cn.edu.gfkd.evidence.entity.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface SyncStatusRepository extends JpaRepository<SyncStatus, String> {
    
    // Since contractAddress is now the primary key, findById can be used directly
    // No need for separate findByContractAddress method
    
    boolean existsByContractAddress(String contractAddress);
}