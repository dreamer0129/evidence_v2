package cn.edu.gfkd.evidence.service;

import org.web3j.protocol.Web3j;

/**
 * Web3j服务接口 - 提供Web3j客户端的统一访问
 * 
 * 主要职责：
 * 1. 提供Web3j客户端的访问
 * 2. 管理Web3j连接状态
 * 3. 提供Web3j相关的工具方法
 */
public interface Web3jService {

    /**
     * 获取Web3j客户端
     * 
     * @return Web3j客户端实例
     */
    Web3j getWeb3j();

    /**
     * 检查Web3j连接是否正常
     * 
     * @return 是否连接正常
     */
    boolean isConnectionValid();

    /**
     * 获取连接状态信息
     * 
     * @return 连接状态描述
     */
    String getConnectionStatus();
}