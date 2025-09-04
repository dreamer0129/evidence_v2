package cn.edu.gfkd.evidence.service;

import java.math.BigInteger;

/**
 * 区块链合约事件监听服务接口
 * 
 * 主要职责：
 * 1. 管理区块链事件的实时监听
 * 2. 处理历史事件获取
 * 3. 提供事件订阅和取消订阅功能
 * 4. 管理监听生命周期
 */
public interface ContractListenerService {

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
     * 重启事件监听
     * 
     * @throws BlockchainException 重启失败时抛出
     */
    void restartEventListening();

    /**
     * 获取指定区块范围的历史事件
     * 
     * @param startBlock 起始区块
     * @param endBlock 结束区块
     * @throws BlockchainException 获取失败时抛出
     */
    void getHistoricalEvents(BigInteger startBlock, BigInteger endBlock);

    /**
     * 获取合约地址
     * 
     * @return 合约地址
     */
    String getContractAddress();

    /**
     * 检查监听是否正在运行
     * 
     * @return 是否正在监听
     */
    boolean isListening();

    /**
     * 获取监听状态信息
     * 
     * @return 监听状态描述
     */
    String getListenerStatus();

    /**
     * 设置事件处理器
     * 
     * @param eventProcessor 事件处理器
     */
    void setEventProcessor(BlockchainEventProcessor eventProcessor);

    /**
     * 移除事件处理器
     * 
     * @param eventProcessor 事件处理器
     */
    void removeEventProcessor(BlockchainEventProcessor eventProcessor);

    /**
     * 初始化监听服务
     * 
     * @throws BlockchainException 初始化失败时抛出
     */
    void init();

    /**
     * 关闭监听服务
     */
    void shutdown();

    /**
     * 处理未处理的事件
     * 
     * @throws BlockchainException 处理失败时抛出
     */
    void processUnprocessedEvents();

    /**
     * 获取监听统计信息
     * 
     * @return 统计信息
     */
    String getListenerStatistics();
}