package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.exception.BlockchainException;
import cn.edu.gfkd.evidence.service.processor.BlockchainEventProcessor;

import java.math.BigInteger;

/**
 * 区块链事件统一服务接口
 * 
 * 主要职责：
 * 1. 管理区块链事件的实时监听和历史同步
 * 2. 提供同步状态管理功能
 * 3. 处理事件处理器的注册和管理
 */
public interface BlockchainEvidenceEventService {

    /**
     * 启动事件监听
     * 
     * @throws BlockchainException 启动失败时抛出
     */
    void startEventListening();

    /**
     * 停止事件监听
     */
    void stopEventListening();

    /**
     * 检查监听是否正在运行
     * 
     * @return 是否正在监听
     */
    boolean isListening();

    /**
     * 同步指定区块范围的历史事件
     * 
     * @param startBlock 起始区块号
     * @param endBlock 结束区块号
     * @throws BlockchainException 同步失败时抛出
     */
    void syncHistoricalEvents(BigInteger startBlock, BigInteger endBlock);

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
     * 设置事件处理器
     * 
     * @param eventProcessor 事件处理器
     */
    void setEventProcessor(BlockchainEventProcessor eventProcessor);
}