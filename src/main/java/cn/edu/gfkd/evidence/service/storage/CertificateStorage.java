package cn.edu.gfkd.evidence.service.storage;

import cn.edu.gfkd.evidence.entity.EvidenceEntity;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * 证书存储抽象接口
 * 定义了证书存储的基本操作，支持不同的存储实现
 */
public interface CertificateStorage {
    
    /**
     * 存储证书
     * @param evidence 证据实体
     * @param certificateData 证书数据流
     * @return 证书ID
     * @throws IOException 存储失败时抛出异常
     */
    String storeCertificate(EvidenceEntity evidence, InputStream certificateData) throws IOException;
    
    /**
     * 获取证书数据
     * @param certificateId 证书ID
     * @return 证书数据流，如果不存在返回Optional.empty()
     * @throws IOException 读取失败时抛出异常
     */
    Optional<InputStream> getCertificate(String certificateId) throws IOException;
    
    /**
     * 删除证书
     * @param certificateId 证书ID
     * @return 删除是否成功
     * @throws IOException 删除失败时抛出异常
     */
    boolean deleteCertificate(String certificateId) throws IOException;
    
    /**
     * 检查证书是否存在
     * @param certificateId 证书ID
     * @return 证书是否存在
     */
    boolean certificateExists(String certificateId);
    
    /**
     * 获取证书文件大小
     * @param certificateId 证书ID
     * @return 文件大小（字节），如果不存在返回0
     * @throws IOException 获取大小时抛出异常
     */
    long getCertificateSize(String certificateId) throws IOException;
    
    /**
     * 获取证书的公开访问URL（如果适用）
     * @param certificateId 证书ID
     * @return 公开访问URL，如果不适用返回Optional.empty()
     */
    Optional<String> getPublicUrl(String certificateId);
    
    /**
     * 获取存储类型标识
     * @return 存储类型名称
     */
    String getStorageType();
    
    /**
     * 初始化存储
     * @throws IOException 初始化失败时抛出异常
     */
    default void initialize() throws IOException {
        // 默认空实现，子类可以根据需要重写
    }
    
    /**
     * 清理资源
     * @throws IOException 清理失败时抛出异常
     */
    default void cleanup() throws IOException {
        // 默认空实现，子类可以根据需要重写
    }
}