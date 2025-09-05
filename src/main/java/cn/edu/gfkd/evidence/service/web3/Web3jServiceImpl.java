package cn.edu.gfkd.evidence.service.web3;

import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;

import cn.edu.gfkd.evidence.exception.BlockchainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Web3j服务实现
 * 
 * 主要职责：
 * 1. 提供Web3j客户端的统一访问
 * 2. 管理Web3j连接状态
 * 3. 提供连接健康检查功能
 */
@Service @RequiredArgsConstructor @Slf4j
public class Web3jServiceImpl implements Web3jService {

    private final Web3j web3j;

    @Override
    public Web3j getWeb3j() {
        if (!isConnectionValid()) {
            throw new BlockchainException("Web3j connection is not valid");
        }
        return web3j;
    }

    @Override
    public boolean isConnectionValid() {
        try {
            // 简单的连接检查
            return web3j != null && web3j.ethBlockNumber().send().getBlockNumber() != null;
        } catch (Exception e) {
            log.warn("Web3j connection check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getConnectionStatus() {
        try {
            if (web3j == null) {
                return "DISCONNECTED - Web3j client is null";
            }
            
            org.web3j.protocol.core.methods.response.EthBlockNumber blockNumber = 
                web3j.ethBlockNumber().send();
            
            if (blockNumber == null) {
                return "ERROR - Block number response is null";
            }
            
            return String.format("CONNECTED - Current block: %d", blockNumber.getBlockNumber());
            
        } catch (Exception e) {
            return String.format("ERROR - %s", e.getMessage());
        }
    }
}