package cn.edu.gfkd.evidence.repository;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;

@Repository
public interface BlockchainEventRepository extends JpaRepository<BlockchainEvent, Long> {
    
    List<BlockchainEvent> findByEventName(String eventName);
    
    List<BlockchainEvent> findByContractAddress(String contractAddress);
    
    List<BlockchainEvent> findByContractAddressAndEventName(String contractAddress, String eventName);
    
    List<BlockchainEvent> findByIsProcessed(Boolean isProcessed);
    
    List<BlockchainEvent> findByBlockNumberGreaterThanAndIsProcessed(BigInteger blockNumber, Boolean isProcessed);
    
    @Query("SELECT e FROM BlockchainEvent e WHERE e.isProcessed = false ORDER BY e.blockNumber ASC, e.logIndex ASC")
    List<BlockchainEvent> findUnprocessedEvents(Pageable pageable);
    
    @Query("SELECT MAX(e.blockNumber) FROM BlockchainEvent e WHERE e.isProcessed = true")
    BigInteger findMaxProcessedBlockNumber();
    
    boolean existsByTransactionHash(String transactionHash);
    
    @Query("SELECT e FROM BlockchainEvent e WHERE e.blockNumber >= :startBlock AND e.blockNumber <= :endBlock ORDER BY e.blockNumber ASC, e.logIndex ASC")
    List<BlockchainEvent> findByBlockNumberRange(@Param("startBlock") BigInteger startBlock, 
                                                @Param("endBlock") BigInteger endBlock);
    
    @Query("SELECT e FROM BlockchainEvent e WHERE e.contractAddress = :contractAddress AND e.blockNumber >= :startBlock AND e.blockNumber <= :endBlock ORDER BY e.blockNumber ASC, e.logIndex ASC")
    List<BlockchainEvent> findByContractAddressAndBlockNumberRange(@Param("contractAddress") String contractAddress,
                                                                  @Param("startBlock") BigInteger startBlock,
                                                                  @Param("endBlock") BigInteger endBlock);
    
    long countByEventNameAndIsProcessed(String eventName, Boolean isProcessed);
    
    void deleteByBlockNumberLessThan(BigInteger blockNumber);
    
    List<BlockchainEvent> findByTransactionHash(String transactionHash);
}