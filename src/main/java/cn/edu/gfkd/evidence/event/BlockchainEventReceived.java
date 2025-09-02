package cn.edu.gfkd.evidence.event;

import java.math.BigInteger;

public class BlockchainEventReceived {
    
    private final String contractAddress;
    private final String eventName;
    private final String evidenceId;
    private final String userAddress;
    private final String hashValue;
    private final String oldStatus;
    private final String newStatus;
    private final BigInteger blockNumber;
    private final String transactionHash;
    private final BigInteger logIndex;
    private final BigInteger blockTimestamp;
    private final boolean isValid;
    
    public BlockchainEventReceived(String contractAddress, String eventName, String evidenceId, String userAddress, 
                                  String hashValue, String oldStatus, String newStatus,
                                  BigInteger blockNumber, String transactionHash, 
                                  BigInteger logIndex, BigInteger blockTimestamp, 
                                  boolean isValid) {
        this.contractAddress = contractAddress;
        this.eventName = eventName;
        this.evidenceId = evidenceId;
        this.userAddress = userAddress;
        this.hashValue = hashValue;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.blockNumber = blockNumber;
        this.transactionHash = transactionHash;
        this.logIndex = logIndex;
        this.blockTimestamp = blockTimestamp;
        this.isValid = isValid;
    }
    
    // Getters
    public String getContractAddress() {
        return contractAddress;
    }
    
    public String getEventName() {
        return eventName;
    }
    
    public String getEvidenceId() {
        return evidenceId;
    }
    
    public String getUserAddress() {
        return userAddress;
    }
    
    public String getHashValue() {
        return hashValue;
    }
    
    public String getOldStatus() {
        return oldStatus;
    }
    
    public String getNewStatus() {
        return newStatus;
    }
    
    public BigInteger getBlockNumber() {
        return blockNumber;
    }
    
    public String getTransactionHash() {
        return transactionHash;
    }
    
    public BigInteger getLogIndex() {
        return logIndex;
    }
    
    public BigInteger getBlockTimestamp() {
        return blockTimestamp;
    }
    
    public boolean isValid() {
        return isValid;
    }
}