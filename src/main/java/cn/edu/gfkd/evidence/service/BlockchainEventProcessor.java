package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;

/**
 * 区块链事件处理器接口
 * 
 * 主要职责：
 * 1. 定义标准的事件处理接口
 * 2. 支持不同类型的事件处理
 * 3. 提供事件处理优先级管理
 * 4. 统一异常处理策略
 */
public interface BlockchainEventProcessor {

    /**
     * 处理区块链事件
     * 
     * @param event 要处理的事件
     * @throws EventProcessingException 处理失败时抛出
     */
    void processEvent(BlockchainEvent event) throws EventProcessingException;

    /**
     * 获取支持的事件类型
     * 
     * @return 支持的事件类型名称
     */
    String getSupportedEventType();

    /**
     * 获取处理器优先级（数值越小优先级越高）
     * 
     * @return 优先级数值
     */
    int getPriority();

    /**
     * 检查是否能够处理指定的事件
     * 
     * @param event 要检查的事件
     * @return 是否能够处理
     */
    boolean canProcess(BlockchainEvent event);

    /**
     * 获取处理器描述信息
     * 
     * @return 处理器描述
     */
    String getProcessorDescription();

    /**
     * 初始化处理器
     * 
     * @throws EventProcessingException 初始化失败时抛出
     */
    default void initialize() throws EventProcessingException {
        // 默认空实现
    }

    /**
     * 销毁处理器，释放资源
     */
    default void destroy() {
        // 默认空实现
    }

    /**
     * 处理器健康检查
     * 
     * @return 是否健康
     */
    default boolean isHealthy() {
        return true;
    }

    /**
     * 获取处理器统计信息
     * 
     * @return 统计信息JSON字符串
     */
    default String getStatistics() {
        return "{}";
    }
}