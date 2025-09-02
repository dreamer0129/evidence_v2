package cn.edu.gfkd.evidence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Table(name = "evidence")
public class Evidence {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 100)
    private String evidenceId;
    
    @Column(name = "user_address", nullable = false, length = 42)
    private String userAddress;
    
    // File metadata
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    
    @Column(name = "mime_type", length = 100)
    private String mimeType;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "file_creation_time")
    private BigInteger fileCreationTime;
    
    // Hash information
    @Column(name = "hash_algorithm", nullable = false, length = 20)
    private String hashAlgorithm;
    
    @Column(name = "hash_value", nullable = false, length = 66)
    private String hashValue;
    
    @Column(nullable = false, length = 20)
    private String status = "effective";
    
    @Column(name = "block_number", nullable = false)
    private BigInteger blockNumber;
    
    @Column(name = "transaction_hash", unique = true, nullable = false, length = 66)
    private String transactionHash;
    
    @Column(name = "block_timestamp", nullable = false)
    private BigInteger blockTimestamp;
    
    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;
    
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    @Column(name = "revoker_address", length = 42)
    private String revokerAddress;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public Evidence() {}
    
    public Evidence(String evidenceId, String userAddress, String fileName, 
                   String mimeType, Long fileSize, BigInteger fileCreationTime,
                   String hashAlgorithm, String hashValue, BigInteger blockNumber, 
                   String transactionHash, BigInteger blockTimestamp, String memo) {
        this.evidenceId = evidenceId;
        this.userAddress = userAddress;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.fileCreationTime = fileCreationTime;
        this.hashAlgorithm = hashAlgorithm;
        this.hashValue = hashValue;
        this.blockNumber = blockNumber;
        this.transactionHash = transactionHash;
        this.blockTimestamp = blockTimestamp;
        this.memo = memo;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEvidenceId() {
        return evidenceId;
    }
    
    public void setEvidenceId(String evidenceId) {
        this.evidenceId = evidenceId;
    }
    
    public String getUserAddress() {
        return userAddress;
    }
    
    public void setUserAddress(String userAddress) {
        this.userAddress = userAddress;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public BigInteger getFileCreationTime() {
        return fileCreationTime;
    }
    
    public void setFileCreationTime(BigInteger fileCreationTime) {
        this.fileCreationTime = fileCreationTime;
    }
    
    public String getHashAlgorithm() {
        return hashAlgorithm;
    }
    
    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }
    
    public String getHashValue() {
        return hashValue;
    }
    
    public void setHashValue(String hashValue) {
        this.hashValue = hashValue;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public BigInteger getBlockNumber() {
        return blockNumber;
    }
    
    public void setBlockNumber(BigInteger blockNumber) {
        this.blockNumber = blockNumber;
    }
    
    public String getTransactionHash() {
        return transactionHash;
    }
    
    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }
    
    public BigInteger getBlockTimestamp() {
        return blockTimestamp;
    }
    
    public void setBlockTimestamp(BigInteger blockTimestamp) {
        this.blockTimestamp = blockTimestamp;
    }
    
    public String getMemo() {
        return memo;
    }
    
    public void setMemo(String memo) {
        this.memo = memo;
    }
    
    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }
    
    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
    
    public String getRevokerAddress() {
        return revokerAddress;
    }
    
    public void setRevokerAddress(String revokerAddress) {
        this.revokerAddress = revokerAddress;
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