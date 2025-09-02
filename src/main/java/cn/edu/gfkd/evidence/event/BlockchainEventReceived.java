package cn.edu.gfkd.evidence.event;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic blockchain event wrapper that can handle different types of events
 * Based on Web3j's BaseEventResponse and Log structure
 */
public class BlockchainEventReceived {
    
    // Basic log information from Web3j Log object
    private final String contractAddress;
    private final String eventName;
    private final BigInteger blockNumber;
    private final String transactionHash;
    private final BigInteger logIndex;
    private final BigInteger blockTimestamp;
    
    // Raw event data as JSON string
    private final String rawData;
    
    // Event-specific parameters
    private final Map<String, Object> parameters;
    
    public BlockchainEventReceived(String contractAddress, String eventName, 
                                  BigInteger blockNumber, String transactionHash, 
                                  BigInteger logIndex, BigInteger blockTimestamp, 
                                  String rawData) {
        this.contractAddress = contractAddress;
        this.eventName = eventName;
        this.blockNumber = blockNumber;
        this.transactionHash = transactionHash;
        this.logIndex = logIndex;
        this.blockTimestamp = blockTimestamp;
        this.rawData = rawData;
        this.parameters = new HashMap<>();
    }
    
    public BlockchainEventReceived(String contractAddress, String eventName,
                                  BigInteger blockNumber, String transactionHash,
                                  BigInteger logIndex, BigInteger blockTimestamp,
                                  String rawData, Map<String, Object> parameters) {
        this.contractAddress = contractAddress;
        this.eventName = eventName;
        this.blockNumber = blockNumber;
        this.transactionHash = transactionHash;
        this.logIndex = logIndex;
        this.blockTimestamp = blockTimestamp;
        this.rawData = rawData;
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }
    
    // Getters for basic log information
    public String getContractAddress() {
        return contractAddress;
    }
    
    public String getEventName() {
        return eventName;
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
    
    public String getRawData() {
        return rawData;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    

    // Builder pattern for easier construction
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String contractAddress;
        private String eventName;
        private BigInteger blockNumber;
        private String transactionHash;
        private BigInteger logIndex;
        private BigInteger blockTimestamp;
        private String rawData;
        private Map<String, Object> parameters = new HashMap<>();
        
        public Builder contractAddress(String contractAddress) {
            this.contractAddress = contractAddress;
            return this;
        }
        
        public Builder eventName(String eventName) {
            this.eventName = eventName;
            return this;
        }
        
        public Builder blockNumber(BigInteger blockNumber) {
            this.blockNumber = blockNumber;
            return this;
        }
        
        public Builder transactionHash(String transactionHash) {
            this.transactionHash = transactionHash;
            return this;
        }
        
        public Builder logIndex(BigInteger logIndex) {
            this.logIndex = logIndex;
            return this;
        }
        
        public Builder blockTimestamp(BigInteger blockTimestamp) {
            this.blockTimestamp = blockTimestamp;
            return this;
        }
        
        public Builder rawData(String rawData) {
            this.rawData = rawData;
            return this;
        }
        
        public Builder parameter(String key, Object value) {
            this.parameters.put(key, value);
            return this;
        }
        
        
        public BlockchainEventReceived build() {
            return new BlockchainEventReceived(
                contractAddress, eventName, blockNumber, transactionHash,
                logIndex, blockTimestamp, rawData, parameters
            );
        }
    }
}