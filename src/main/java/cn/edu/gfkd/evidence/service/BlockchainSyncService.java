package cn.edu.gfkd.evidence.service;

import java.math.BigInteger;

/**
 * 区块链同步服务接口
 * 
 * 主要职责：
 * 1. 管理区块链事件同步状态
 * 2. 处理历史事件同步
 * 3. 跟踪同步进度和状态
 * 4. 提供同步相关的查询功能
 */
public interface BlockchainSyncService {

    /**
     * 同步指定区块范围的历史事件
     * 
     * @param startBlock 起始区块号
     * @param endBlock 结束区块号
     * @throws BlockchainException 同步失败时抛出
     */
    void syncPastEvents(BigInteger startBlock, BigInteger endBlock);

    /**
     * 批量同步历史事件（分批次处理）
     * 
     * @param startBlock 起始区块号
     * @param endBlock 结束区块号
     * @param batchSize 每批次的区块数量
     * @throws BlockchainException 同步失败时抛出
     */
    void syncPastEventsInBatches(BigInteger startBlock, BigInteger endBlock, BigInteger batchSize);

    /**
     * 在启动时同步缺失的事件
     * 
     * @throws BlockchainException 同步失败时抛出
     */
    void syncMissingEventsOnStartup();

    /**
     * 更新同步状态到指定区块
     * 
     * @param blockNumber 最后同步的区块号
     * @throws BlockchainException 更新失败时抛出
     */
    void updateSyncStatus(BigInteger blockNumber);

    /**
     * 获取最后同步的区块号
     * 
     * @return 最后同步的区块号
     * @throws BlockchainException 查询失败时抛出
     */
    BigInteger getLastSyncedBlockNumber();

    /**
     * 获取当前区块链的最新区块号
     * 
     * @return 当前最新区块号
     * @throws BlockchainException 查询失败时抛出
     */
    BigInteger getCurrentBlockNumber();

    /**
     * 检查是否需要同步历史事件
     * 
     * @return 是否需要同步
     * @throws BlockchainException 检查失败时抛出
     */
    boolean needsHistoricalSync();

    /**
     * 计算需要同步的区块数量
     * 
     * @return 需要同步的区块数量
     * @throws BlockchainException 计算失败时抛出
     */
    BigInteger calculateBlocksBehind();

    /**
     * 重置同步状态到指定区块
     * 
     * @param blockNumber 重置到的区块号
     * @throws BlockchainException 重置失败时抛出
     */
    void resetSyncStatus(BigInteger blockNumber);

    /**
     * 获取同步状态信息
     * 
     * @return 同步状态描述
     * @throws BlockchainException 查询失败时抛出
     */
    String getSyncStatus();

    /**
     * 检查同步是否正在进行
     * 
     * @return 是否正在同步
     */
    boolean isSyncInProgress();

    /**
     * 设置同步状态
     * 
     * @param inProgress 是否正在同步
     */
    void setSyncInProgress(boolean inProgress);

    /**
     * 获取同步进度百分比
     * 
     * @return 同步进度（0-100）
     * @throws BlockchainException 计算失败时抛出
     */
    double getSyncProgressPercentage();

    /**
     * 获取合约地址
     * 
     * @return 合约地址
     */
    String getContractAddress();

    /**
     * 验证同步状态的一致性
     * 
     * @return 是否一致
     * @throws BlockchainException 验证失败时抛出
     */
    boolean validateSyncConsistency();
}