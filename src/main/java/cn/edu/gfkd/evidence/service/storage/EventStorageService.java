package cn.edu.gfkd.evidence.service.storage;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import cn.edu.gfkd.evidence.exception.BlockchainException;
import org.springframework.data.domain.Pageable;

import cn.edu.gfkd.evidence.entity.BlockchainEvent;

/**
 * 区块链事件存储服务接口
 * 
 * 主要职责：
 * 1. 区块链事件的持久化存储
 * 2. 事件状态管理（已处理/未处理）
 * 3. 事件查询和检索
 * 4. 事务性操作保证
 */
public interface EventStorageService {

    /**
     * 保存区块链事件到数据库
     * 
     * @param event 要保存的事件
     * @return 保存后的事件（包含生成的ID）
     * @throws BlockchainException 保存失败时抛出
     */
    BlockchainEvent saveEvent(BlockchainEvent event);

    /**
     * 批量保存区块链事件
     * 
     * @param events 要保存的事件列表
     * @return 保存后的事件列表
     * @throws BlockchainException 批量保存失败时抛出
     */
    List<BlockchainEvent> saveEvents(List<BlockchainEvent> events);

    /**
     * 根据交易哈希查找事件
     * 
     * @param transactionHash 交易哈希
     * @return 匹配的事件列表
     * @throws BlockchainException 查询失败时抛出
     */
    List<BlockchainEvent> findByTransactionHash(String transactionHash);

    /**
     * 根据事件类型和交易哈希查找事件
     * 
     * @param transactionHash 交易哈希
     * @param eventType 事件类型
     * @return 匹配的事件（可选）
     * @throws BlockchainException 查询失败时抛出
     */
    Optional<BlockchainEvent> findByTransactionHashAndEventType(String transactionHash, String eventType);

    /**
     * 查找未处理的事件（分页）
     * 
     * @param pageable 分页参数
     * @return 未处理的事件列表
     * @throws BlockchainException 查询失败时抛出
     */
    List<BlockchainEvent> findUnprocessedEvents(Pageable pageable);

    /**
     * 查找指定事件类型的未处理事件
     * 
     * @param eventType 事件类型
     * @param pageable 分页参数
     * @return 指定类型的未处理事件列表
     * @throws BlockchainException 查询失败时抛出
     */
    List<BlockchainEvent> findUnprocessedEventsByEventType(String eventType, Pageable pageable);

    /**
     * 标记事件为已处理状态
     * 
     * @param eventId 事件ID
     * @throws BlockchainException 更新失败时抛出
     */
    void markEventAsProcessed(Long eventId);

    /**
     * 根据交易哈希和事件类型标记事件为已处理
     * 
     * @param transactionHash 交易哈希
     * @param eventType 事件类型
     * @throws BlockchainException 更新失败时抛出
     */
    void markEventAsProcessed(String transactionHash, String eventType);

    /**
     * 获取事件的原始数据
     * 
     * @param eventId 事件ID
     * @return 事件的原始数据JSON字符串
     * @throws BlockchainException 查询失败时抛出
     */
    String getEventRawData(Long eventId);

    /**
     * 删除指定时间之前的事件（清理用途）
     * 
     * @param beforeTime 删除时间点
     * @throws BlockchainException 删除失败时抛出
     */
    void deleteEventsBefore(LocalDateTime beforeTime);

    /**
     * 统计未处理事件的数量
     * 
     * @param eventType 事件类型（可选，null表示所有类型）
     * @return 未处理事件数量
     * @throws BlockchainException 统计失败时抛出
     */
    long countUnprocessedEvents(String eventType);

    /**
     * 获取事件处理失败的次数
     * 
     * @param eventId 事件ID
     * @return 处理失败次数
     * @throws BlockchainException 查询失败时抛出
     */
    int getEventProcessingFailures(Long eventId);

    /**
     * 增加事件处理失败次数
     * 
     * @param eventId 事件ID
     * @throws BlockchainException 更新失败时抛出
     */
    void incrementEventProcessingFailures(Long eventId);

    /**
     * 检查事件是否已存在（避免重复处理）
     * 
     * @param contractAddress 合约地址
     * @param blockNumber 区块号
     * @param transactionHash 交易哈希
     * @param eventType 事件类型
     * @return 是否已存在
     * @throws BlockchainException 检查失败时抛出
     */
    boolean eventExists(String contractAddress, BigInteger blockNumber, String transactionHash, String eventType);
}