package cn.edu.gfkd.evidence.service;

import org.springframework.stereotype.Service;

/**
 * 区块链事件监听器 - 新架构版本
 * 
 * 主要职责：
 * 1. 提供统一的事件监听入口
 * 2. 委托给EventOrchestratorService处理具体逻辑
 * 3. 保持API简洁性
 */
@Service
public class EvidenceEventListener {

    private final EventOrchestratorService eventOrchestratorService;

    public EvidenceEventListener(EventOrchestratorService eventOrchestratorService) {
        this.eventOrchestratorService = eventOrchestratorService;
    }

    /**
     * 初始化事件监听器
     */
    public void init() {
        eventOrchestratorService.init();
    }

    /**
     * 获取系统状态
     */
    public String getSystemStatus() {
        return eventOrchestratorService.getSystemStatus();
    }

    /**
     * 关闭事件监听器
     */
    public void shutdown() {
        eventOrchestratorService.shutdown();
    }
}