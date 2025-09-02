package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.entity.SyncStatus;
import cn.edu.gfkd.evidence.repository.SyncStatusRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlockNumber;

import java.math.BigInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class BlockchainSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(BlockchainSyncService.class);
    
    @Autowired
    private BlockchainEventListener blockchainEventListener;
    
    @Autowired
    private Web3j web3j;
    
    @Autowired
    private SyncStatusRepository syncStatusRepository;
    
    private ScheduledExecutorService scheduler;
    
    @PostConstruct
    public void init() {
        scheduler = Executors.newScheduledThreadPool(2);
        
        // Start event listening after a delay
        scheduler.schedule(this::startEventListening, 10, TimeUnit.SECONDS);
        
        // Schedule periodic sync checks
        scheduler.scheduleAtFixedRate(this::checkAndSyncMissingEvents, 30, 60, TimeUnit.SECONDS);
    }
    
    public void startEventListening() {
        logger.info("Starting blockchain event listener...");
        
        try {
            // Check if we need to sync past events
            syncMissingEventsOnStartup();
            
            // Start real-time event listening
            blockchainEventListener.startEventListening();
            
        } catch (Exception e) {
            logger.error("Failed to start blockchain event listener", e);
            // Retry after 30 seconds
            scheduler.schedule(this::startEventListening, 30, TimeUnit.SECONDS);
        }
    }
    
    private void syncMissingEventsOnStartup() {
        try {
            // Get current block number
            EthBlockNumber blockNumberResponse = web3j.ethBlockNumber().send();
            BigInteger currentBlock = blockNumberResponse.getBlockNumber();
            
            // Get last synced block
            SyncStatus syncStatus = syncStatusRepository.findById(
                blockchainEventListener.getContractAddress()
            ).orElseGet(() -> {
                SyncStatus newStatus = new SyncStatus(
                    blockchainEventListener.getContractAddress(), 
                    BigInteger.ZERO
                );
                return syncStatusRepository.save(newStatus);
            });
            
            BigInteger lastSyncedBlock = syncStatus.getLastBlockNumber();
            
            // If we're more than 10 blocks behind, sync the missing blocks
            if (currentBlock.subtract(lastSyncedBlock).compareTo(BigInteger.TEN) > 0) {
                logger.info("Detected {} blocks behind current block. Syncing missing events...", 
                    currentBlock.subtract(lastSyncedBlock));
                
                // Sync in batches of 1000 blocks
                BigInteger batchSize = BigInteger.valueOf(1000);
                BigInteger startBlock = lastSyncedBlock.add(BigInteger.ONE);
                
                while (startBlock.compareTo(currentBlock) <= 0) {
                    BigInteger endBlock = startBlock.add(batchSize).subtract(BigInteger.ONE);
                    if (endBlock.compareTo(currentBlock) > 0) {
                        endBlock = currentBlock;
                    }
                    
                    try {
                        blockchainEventListener.syncPastEvents(startBlock, endBlock);
                        logger.info("Synced blocks {} to {}", startBlock, endBlock);
                        
                        startBlock = endBlock.add(BigInteger.ONE);
                        
                        // Small delay to avoid overwhelming the node
                        Thread.sleep(100);
                        
                    } catch (Exception e) {
                        logger.error("Failed to sync blocks {} to {}", startBlock, endBlock, e);
                        // Continue with next batch
                        startBlock = endBlock.add(BigInteger.ONE);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to sync missing events on startup", e);
        }
    }
    
    public void checkAndSyncMissingEvents() {
        try {
            if (!isBlockchainConnected()) {
                logger.warn("Blockchain node not connected. Skipping sync check.");
                return;
            }
            
            EthBlockNumber blockNumberResponse = web3j.ethBlockNumber().send();
            BigInteger currentBlock = blockNumberResponse.getBlockNumber();
            
            SyncStatus syncStatus = syncStatusRepository.findById(
                blockchainEventListener.getContractAddress()
            ).orElse(null);
            
            if (syncStatus == null) {
                logger.warn("No sync status found. Skipping sync check.");
                return;
            }
            
            BigInteger lastSyncedBlock = syncStatus.getLastBlockNumber();
            
            // If we're more than 5 blocks behind, trigger a sync
            if (currentBlock.subtract(lastSyncedBlock).compareTo(BigInteger.valueOf(5)) > 0) {
                logger.info("Detected {} blocks behind. Triggering sync...", 
                    currentBlock.subtract(lastSyncedBlock));
                
                // Sync in smaller batches to avoid blocking
                BigInteger batchSize = BigInteger.valueOf(100);
                BigInteger startBlock = lastSyncedBlock.add(BigInteger.ONE);
                BigInteger endBlock = startBlock.add(batchSize).subtract(BigInteger.ONE);
                
                if (endBlock.compareTo(currentBlock) > 0) {
                    endBlock = currentBlock;
                }
                
                blockchainEventListener.syncPastEvents(startBlock, endBlock);
            }
            
        } catch (Exception e) {
            logger.error("Failed to check and sync missing events", e);
        }
    }
    
    private boolean isBlockchainConnected() {
        try {
            web3j.ethBlockNumber().send();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}