package cn.edu.gfkd.evidence.service;

import java.math.BigInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.edu.gfkd.evidence.exception.BlockchainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 统一重试处理器 - 提供数据库操作的重试机制
 * 
 * 主要职责：
 * 1. 为数据库操作提供重试逻辑，处理SQLite锁冲突
 * 2. 支持可配置的重试次数和延迟时间
 * 3. 提供指数退避策略避免频繁重试
 * 4. 统一的异常处理和日志记录
 */
@Service @RequiredArgsConstructor @Slf4j
public class RetryHandlerImpl implements RetryHandler {

    // SQLite数据库锁重试配置
    @Value("${db.retry.max-attempts:3}")
    private int maxAttempts;
    
    @Value("${db.retry.initial-delay-ms:100}")
    private long initialDelayMs;
    
    @Value("${db.retry.backoff-multiplier:2}")
    private double backoffMultiplier;
    
    @Value("${db.retry.max-delay-ms:1000}")
    private long maxDelayMs;

    @Override
    public <T> T executeWithRetry(RetryableOperation<T> operation, String operationName) {
        int attempt = 0;
        Exception lastException = null;
        long currentDelay = initialDelayMs;

        log.debug("Starting operation {} with retry configuration: maxAttempts={}, initialDelay={}ms", 
                operationName, maxAttempts, initialDelayMs);

        while (attempt < maxAttempts) {
            attempt++;
            try {
                log.debug("Attempt {}/{} for operation {}", attempt, maxAttempts, operationName);
                T result = operation.execute();
                
                if (attempt > 1) {
                    log.info("Operation {} succeeded on attempt {}/{}", operationName, attempt, maxAttempts);
                }
                
                return result;
                
            } catch (Exception e) {
                lastException = e;
                
                if (attempt < maxAttempts) {
                    log.warn("Attempt {}/{} failed for {}: {}. Retrying in {}ms...", 
                            attempt, maxAttempts, operationName, e.getMessage(), currentDelay);
                    
                    try {
                        Thread.sleep(currentDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BlockchainException(
                                "Interrupted during retry for " + operationName, ie);
                    }
                    
                    // 指数退避策略
                    currentDelay = (long) Math.min(currentDelay * backoffMultiplier, maxDelayMs);
                    
                } else {
                    log.error("All {} attempts failed for {}: {}", maxAttempts, operationName, e.getMessage());
                }
            }
        }

        throw new BlockchainException(
                "Failed to execute " + operationName + " after " + maxAttempts + " attempts",
                lastException);
    }

    /**
     * 同步方法版本 - 使用synchronized确保线程安全
     */
    @Override
    public <T> T executeWithRetrySync(RetryableOperation<T> operation, String operationName, Object lock) {
        int attempt = 0;
        Exception lastException = null;
        long currentDelay = initialDelayMs;

        log.debug("Starting sync operation {} with lock protection", operationName);

        while (attempt < maxAttempts) {
            attempt++;
            try {
                synchronized (lock) {
                    log.debug("Attempt {}/{} for sync operation {} (with lock)", attempt, maxAttempts, operationName);
                    T result = operation.execute();
                    
                    if (attempt > 1) {
                        log.info("Sync operation {} succeeded on attempt {}/{}", operationName, attempt, maxAttempts);
                    }
                    
                    return result;
                }
                
            } catch (Exception e) {
                lastException = e;
                
                if (attempt < maxAttempts) {
                    log.warn("Attempt {}/{} failed for sync operation {}: {}. Retrying in {}ms...", 
                            attempt, maxAttempts, operationName, e.getMessage(), currentDelay);
                    
                    try {
                        Thread.sleep(currentDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BlockchainException(
                                "Interrupted during retry for sync " + operationName, ie);
                    }
                    
                    // 指数退避策略
                    currentDelay = (long) Math.min(currentDelay * backoffMultiplier, maxDelayMs);
                    
                } else {
                    log.error("All {} attempts failed for sync operation {}: {}", maxAttempts, operationName, e.getMessage());
                }
            }
        }

        throw new BlockchainException(
                "Failed to execute sync " + operationName + " after " + maxAttempts + " attempts",
                lastException);
    }

    /**
     * 带事务的重试方法 - 确保数据库操作的事务性
     */
    @Transactional
    @Override
    public <T> T executeWithRetryTransactional(RetryableOperation<T> operation, String operationName) {
        log.debug("Starting transactional operation {}", operationName);
        return executeWithRetry(operation, operationName);
    }

    /**
     * 检查异常是否应该重试
     */
    private boolean shouldRetry(Exception e) {
        // 检查是否是数据库锁异常或其他可重试异常
        String message = e.getMessage();
        return message != null && (
            message.contains("database is locked") ||
            message.contains("SQLITE_BUSY") ||
            message.contains("Connection is closed") ||
            message.contains("timeout") ||
            message.contains("Timeout")
        );
    }

    /**
     * 计算下次重试的延迟时间（指数退避）
     */
    private long calculateDelay(int attempt) {
        long delay = (long) (initialDelayMs * Math.pow(backoffMultiplier, attempt - 1));
        return Math.min(delay, maxDelayMs);
    }
}