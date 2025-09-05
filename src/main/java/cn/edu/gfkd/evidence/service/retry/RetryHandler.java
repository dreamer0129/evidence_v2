package cn.edu.gfkd.evidence.service.retry;

import cn.edu.gfkd.evidence.exception.BlockchainException;

/**
 * 重试处理器接口 - 定义统一的重试机制
 * 
 * 主要职责：
 * 1. 为数据库操作提供可重试的执行机制
 * 2. 支持不同的重试策略（同步、事务等）
 * 3. 统一异常处理和日志记录
 * 4. 可配置的重试参数
 */
public interface RetryHandler {

    /**
     * 执行可重试操作 - 基础版本
     * 
     * @param <T> 返回类型
     * @param operation 要执行的操作
     * @param operationName 操作名称（用于日志）
     * @return 操作结果
     * @throws BlockchainException 当所有重试都失败时抛出
     */
    <T> T executeWithRetry(RetryableOperation<T> operation, String operationName);

    /**
     * 执行可重试操作 - 同步版本（带锁保护）
     * 
     * @param <T> 返回类型
     * @param operation 要执行的操作
     * @param operationName 操作名称（用于日志）
     * @param lock 同步锁对象
     * @return 操作结果
     * @throws BlockchainException 当所有重试都失败时抛出
     */
    <T> T executeWithRetrySync(RetryableOperation<T> operation, String operationName, Object lock);

    /**
     * 执行可重试操作 - 事务版本
     * 
     * @param <T> 返回类型
     * @param operation 要执行的操作
     * @param operationName 操作名称（用于日志）
     * @return 操作结果
     * @throws BlockchainException 当所有重试都失败时抛出
     */
    <T> T executeWithRetryTransactional(RetryableOperation<T> operation, String operationName);

    /**
     * 可重试操作函数式接口
     * 
     * @param <T> 返回类型
     */
    @FunctionalInterface
    interface RetryableOperation<T> {
        /**
         * 执行操作
         * 
         * @return 操作结果
         * @throws Exception 操作可能抛出的异常
         */
        T execute() throws Exception;
    }
}