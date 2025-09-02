package cn.edu.gfkd.evidence.dto;

import java.math.BigInteger;
import java.time.LocalDateTime;

public class EvidenceDTO {
    private Long id;
    private String evidenceId;
    private String userAddress;
    
    // File metadata
    private String fileName;
    private String mimeType;
    private Long fileSize;
    private BigInteger fileCreationTime;
    
    // Hash information
    private String hashAlgorithm;
    private String hashValue;
    
    private String status;
    private BigInteger blockNumber;
    private String transactionHash;
    private BigInteger blockTimestamp;
    private String memo;
    private LocalDateTime revokedAt;
    private String revokerAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
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