package cn.edu.gfkd.evidence.service;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.edu.gfkd.evidence.entity.SyncStatus;
import cn.edu.gfkd.evidence.exception.BlockchainException;
import cn.edu.gfkd.evidence.generated.EvidenceStorageContract;
import cn.edu.gfkd.evidence.repository.SyncStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 区块链同步服务实现
 * 
 * 主要职责：
 * 1. 管理区块链事件同步状态和进度
 * 2. 处理历史事件同步和缺失事件补全
 * 3. 提供同步状态查询和验证功能
 * 4. 确保同步过程的可靠性和一致性
 */
@Service @RequiredArgsConstructor @Slf4j
public class BlockchainSyncServiceImpl implements BlockchainSyncService {

    private final Web3jService web3jService;
    private final SyncStatusRepository syncStatusRepository;
    private final EventStorageService eventStorageService;
    private final RetryHandler retryHandler;
    private final EvidenceStorageContract evidenceStorageContract;

    // 同步状态控制
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

    // 同步配置
    @Value("${blockchain.sync.batch-size:1000}")
    private int batchSize;
    
    @Value("${blockchain.sync.max-blocks-behind:100}")
    private int maxBlocksBehind;
    
    @Value("${blockchain.sync.delay-between-batches-ms:50}")
    private long delayBetweenBatchesMs;

    @Override
    @Transactional
    public void syncPastEvents(BigInteger startBlock, BigInteger endBlock) {
        log.info("Starting sync of past events from block {} to {}", startBlock, endBlock);
        
        if (syncInProgress.get()) {
            log.warn("Sync already in progress, skipping new sync request");
            return;
        }

        try {
            setSyncInProgress(true);
            
            // 验证区块范围
            if (startBlock.compareTo(endBlock) > 0) {
                throw new BlockchainException("Start block cannot be greater than end block");
            }
            
            // 分批次同步
            syncInBatches(startBlock, endBlock, BigInteger.valueOf(batchSize));
            
            // 更新同步状态
            updateSyncStatus(endBlock);
            
            log.info("Successfully synced past events from block {} to {}", startBlock, endBlock);
            
        } catch (Exception e) {
            log.error("Failed to sync past events from block {} to {}", startBlock, endBlock, e);
            throw new BlockchainException("Failed to sync past events", e);
        } finally {
            setSyncInProgress(false);
        }
    }

    @Override
    public void syncPastEventsInBatches(BigInteger startBlock, BigInteger endBlock, BigInteger batchSize) {
        log.info("Starting batch sync from block {} to {} with batch size {}", startBlock, endBlock, batchSize);
        
        if (syncInProgress.get()) {
            log.warn("Sync already in progress, skipping batch sync request");
            return;
        }

        try {
            setSyncInProgress(true);
            syncInBatches(startBlock, endBlock, batchSize);
            log.info("Successfully completed batch sync from block {} to {}", startBlock, endBlock);
            
        } catch (Exception e) {
            log.error("Failed to complete batch sync from block {} to {}", startBlock, endBlock, e);
            throw new BlockchainException("Failed to complete batch sync", e);
        } finally {
            setSyncInProgress(false);
        }
    }

    @Override
    public void syncMissingEventsOnStartup() {
        log.info("Checking for missing events on startup");
        
        try {
            BigInteger currentBlock = getCurrentBlockNumber();
            BigInteger lastSyncedBlock = getLastSyncedBlockNumber();
            
            BigInteger blocksBehind = currentBlock.subtract(lastSyncedBlock);
            log.info("Current block: {}, Last synced: {}, Blocks behind: {}", 
                    currentBlock, lastSyncedBlock, blocksBehind);
            
            if (blocksBehind.compareTo(BigInteger.TEN) > 0) {
                log.info("Detected {} blocks behind current block. Syncing missing events...", blocksBehind);
                syncPastEventsInBatches(lastSyncedBlock.add(BigInteger.ONE), currentBlock, 
                        BigInteger.valueOf(batchSize));
            } else {
                log.info("No significant block gap detected ({} blocks), skipping historical sync", blocksBehind);
            }
            
        } catch (Exception e) {
            log.error("Failed to sync missing events on startup", e);
            throw new BlockchainException("Failed to sync missing events on startup", e);
        }
    }

    @Override
    @Transactional
    public void updateSyncStatus(BigInteger blockNumber) {
        log.debug("Updating sync status to block {}", blockNumber);
        
        retryHandler.executeWithRetryTransactional(
            () -> {
                SyncStatus syncStatus = getOrCreateSyncStatus();
                syncStatus.setLastBlockNumber(blockNumber);
                syncStatus.setLastSyncTimestamp(LocalDateTime.now());
                syncStatus.setSyncStatus("SYNCED");
                
                SyncStatus savedStatus = syncStatusRepository.save(syncStatus);
                log.info("Updated sync status: contract={}, blockNumber={}, syncStatus={}", 
                        savedStatus.getContractAddress(), savedStatus.getLastBlockNumber(), 
                        savedStatus.getSyncStatus());
                
                return null;
            },
            "update sync status"
        );
    }

    @Override
    public BigInteger getLastSyncedBlockNumber() {
        log.debug("Getting last synced block number");
        
        return retryHandler.executeWithRetry(
            () -> {
                SyncStatus syncStatus = getOrCreateSyncStatus();
                return syncStatus.getLastBlockNumber();
            },
            "get last synced block number"
        );
    }

    @Override
    public BigInteger getCurrentBlockNumber() {
        log.debug("Getting current block number");
        
        return retryHandler.executeWithRetry(
            () -> {
                try {
                    org.web3j.protocol.core.methods.response.EthBlockNumber blockNumber = 
                            web3jService.getWeb3j().ethBlockNumber().send();
                    
                    if (blockNumber == null) {
                        throw new BlockchainException("Block number response is null");
                    }
                    
                    return blockNumber.getBlockNumber();
                } catch (IOException e) {
                    throw new BlockchainException("Failed to get current block number", e);
                }
            },
            "get current block number"
        );
    }

    @Override
    public boolean needsHistoricalSync() {
        try {
            BigInteger blocksBehind = calculateBlocksBehind();
            return blocksBehind.compareTo(BigInteger.TEN) > 0;
        } catch (Exception e) {
            log.warn("Failed to determine if historical sync is needed, assuming yes", e);
            return true;
        }
    }

    @Override
    public BigInteger calculateBlocksBehind() {
        BigInteger currentBlock = getCurrentBlockNumber();
        BigInteger lastSyncedBlock = getLastSyncedBlockNumber();
        return currentBlock.subtract(lastSyncedBlock);
    }

    @Override
    @Transactional
    public void resetSyncStatus(BigInteger blockNumber) {
        log.warn("Resetting sync status to block {}", blockNumber);
        
        retryHandler.executeWithRetryTransactional(
            () -> {
                SyncStatus syncStatus = getOrCreateSyncStatus();
                syncStatus.setLastBlockNumber(blockNumber);
                syncStatus.setLastSyncTimestamp(LocalDateTime.now());
                syncStatus.setSyncStatus("RESET");
                
                syncStatusRepository.save(syncStatus);
                log.info("Reset sync status to block {}", blockNumber);
                
                return null;
            },
            "reset sync status"
        );
    }

    @Override
    public String getSyncStatus() {
        try {
            SyncStatus syncStatus = getOrCreateSyncStatus();
            BigInteger currentBlock = getCurrentBlockNumber();
            BigInteger lastSyncedBlock = syncStatus.getLastBlockNumber();
            BigInteger blocksBehind = currentBlock.subtract(lastSyncedBlock);
            
            return String.format("Status: %s, Last Block: %d, Current Block: %d, Blocks Behind: %d", 
                    syncStatus.getSyncStatus(), lastSyncedBlock, currentBlock, blocksBehind);
                    
        } catch (Exception e) {
            log.error("Failed to get sync status", e);
            return "Status: ERROR";
        }
    }

    @Override
    public boolean isSyncInProgress() {
        return syncInProgress.get();
    }

    @Override
    public void setSyncInProgress(boolean inProgress) {
        syncInProgress.set(inProgress);
        log.debug("Sync progress set to: {}", inProgress);
    }

    @Override
    public double getSyncProgressPercentage() {
        try {
            BigInteger currentBlock = getCurrentBlockNumber();
            BigInteger lastSyncedBlock = getLastSyncedBlockNumber();
            
            if (currentBlock.compareTo(BigInteger.ZERO) == 0) {
                return 0.0;
            }
            
            double percentage = lastSyncedBlock.doubleValue() / currentBlock.doubleValue() * 100;
            return Math.min(100.0, Math.max(0.0, percentage));
            
        } catch (Exception e) {
            log.error("Failed to calculate sync progress", e);
            return 0.0;
        }
    }

    @Override
    public String getContractAddress() {
        return evidenceStorageContract.getContractAddress();
    }

    @Override
    public boolean validateSyncConsistency() {
        try {
            BigInteger lastSyncedBlock = getLastSyncedBlockNumber();
            BigInteger currentBlock = getCurrentBlockNumber();
            
            // 检查同步区块是否合理
            if (lastSyncedBlock.compareTo(BigInteger.ZERO) < 0) {
                log.warn("Invalid last synced block number: {}", lastSyncedBlock);
                return false;
            }
            
            // 检查是否落后太多
            BigInteger blocksBehind = currentBlock.subtract(lastSyncedBlock);
            if (blocksBehind.compareTo(BigInteger.valueOf(maxBlocksBehind)) > 0) {
                log.warn("Sync is too far behind: {} blocks", blocksBehind);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Failed to validate sync consistency", e);
            return false;
        }
    }

    /**
     * 分批次同步事件
     * 
     * @param startBlock 起始区块
     * @param endBlock 结束区块
     * @param batchSize 每批大小
     */
    private void syncInBatches(BigInteger startBlock, BigInteger endBlock, BigInteger batchSize) {
        log.info("Starting sync of blocks {} to {} with batch size {}", startBlock, endBlock, batchSize);
        BigInteger current = startBlock;

        while (current.compareTo(endBlock) <= 0) {
            BigInteger batchEnd = current.add(batchSize).subtract(BigInteger.ONE);
            if (batchEnd.compareTo(endBlock) > 0) {
                batchEnd = endBlock;
            }

            try {
                log.debug("Syncing batch: blocks {} to {}", current, batchEnd);
                
                // 这里应该调用事件存储服务来同步该批次的事件
                // 具体的同步逻辑将在后续的事件获取服务中实现
                
                // 更新进度
                updateSyncStatus(batchEnd);
                
                log.info("Synced blocks {} to {}", current, batchEnd);
                current = batchEnd.add(BigInteger.ONE);

                // 添加延迟避免过度消耗节点资源
                if (current.compareTo(endBlock) <= 0) {
                    try {
                        Thread.sleep(delayBetweenBatchesMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

            } catch (Exception e) {
                log.error("Failed to sync blocks {} to {}", current, batchEnd, e);
                current = batchEnd.add(BigInteger.ONE); // 继续下一批次
            }
        }

        log.info("Completed sync from block {} to {}", startBlock, endBlock);
    }

    /**
     * 获取或创建同步状态
     */
    private SyncStatus getOrCreateSyncStatus() {
        String contractAddress = evidenceStorageContract.getContractAddress();
        return syncStatusRepository.findById(contractAddress).orElseGet(() -> {
            SyncStatus newStatus = new SyncStatus(contractAddress, BigInteger.ZERO);
            return syncStatusRepository.save(newStatus);
        });
    }
}