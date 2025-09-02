package cn.edu.gfkd.evidence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Table(name = "sync_status")
public class SyncStatus {
    
    @Id
    @Column(name = "contract_address", nullable = false, length = 42)
    private String contractAddress;
    
    @Column(name = "last_block_number", nullable = false)
    private BigInteger lastBlockNumber;
    
    @Column(name = "last_sync_timestamp", nullable = false)
    private LocalDateTime lastSyncTimestamp;
    
    @Column(name = "sync_status", nullable = false, length = 20)
    private String syncStatus = "SYNCED";
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public SyncStatus() {}
    
    public SyncStatus(String contractAddress, BigInteger lastBlockNumber) {
        this.contractAddress = contractAddress;
        this.lastBlockNumber = lastBlockNumber;
        this.lastSyncTimestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getContractAddress() {
        return contractAddress;
    }
    
    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }
    
    public BigInteger getLastBlockNumber() {
        return lastBlockNumber;
    }
    
    public void setLastBlockNumber(BigInteger lastBlockNumber) {
        this.lastBlockNumber = lastBlockNumber;
    }
    
    public LocalDateTime getLastSyncTimestamp() {
        return lastSyncTimestamp;
    }
    
    public void setLastSyncTimestamp(LocalDateTime lastSyncTimestamp) {
        this.lastSyncTimestamp = lastSyncTimestamp;
    }
    
    public String getSyncStatus() {
        return syncStatus;
    }
    
    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}