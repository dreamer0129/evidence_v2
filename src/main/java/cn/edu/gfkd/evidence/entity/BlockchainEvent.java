package cn.edu.gfkd.evidence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Table(name = "blockchain_event")
public class BlockchainEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "contract_address", nullable = false, length = 42)
    private String contractAddress;
    
    @Column(name = "event_name", nullable = false, length = 50)
    private String eventName;
    
    @Column(name = "block_number", nullable = false)
    private BigInteger blockNumber;
    
    @Column(name = "transaction_hash", nullable = false, length = 66)
    private String transactionHash;
    
    @Column(name = "log_index", nullable = false)
    private BigInteger logIndex;
    
    @Column(name = "block_timestamp", nullable = false)
    private BigInteger blockTimestamp;
    
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
    
    @Column(name = "is_processed", nullable = false)
    private Boolean isProcessed = false;
    
    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public BlockchainEvent() {}
    
    public BlockchainEvent(String contractAddress, String eventName, BigInteger blockNumber, 
                          String transactionHash, BigInteger logIndex, 
                          BigInteger blockTimestamp, String eventData) {
        this.contractAddress = contractAddress;
        this.eventName = eventName;
        this.blockNumber = blockNumber;
        this.transactionHash = transactionHash;
        this.logIndex = logIndex;
        this.blockTimestamp = blockTimestamp;
        this.eventData = eventData;
        this.processedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getContractAddress() {
        return contractAddress;
    }
    
    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }
    
    public String getEventName() {
        return eventName;
    }
    
    public void setEventName(String eventName) {
        this.eventName = eventName;
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
    
    public BigInteger getLogIndex() {
        return logIndex;
    }
    
    public void setLogIndex(BigInteger logIndex) {
        this.logIndex = logIndex;
    }
    
    public BigInteger getBlockTimestamp() {
        return blockTimestamp;
    }
    
    public void setBlockTimestamp(BigInteger blockTimestamp) {
        this.blockTimestamp = blockTimestamp;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
    
    public Boolean getIsProcessed() {
        return isProcessed;
    }
    
    public void setIsProcessed(Boolean isProcessed) {
        this.isProcessed = isProcessed;
    }
    
    public String getEventData() {
        return eventData;
    }
    
    public void setEventData(String eventData) {
        this.eventData = eventData;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}