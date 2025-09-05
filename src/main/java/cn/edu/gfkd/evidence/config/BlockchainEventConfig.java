package cn.edu.gfkd.evidence.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import cn.edu.gfkd.evidence.service.EventOrchestratorService;

/**
 * 区块链事件处理配置类
 * 
 * 主要职责：
 * 1. 配置新架构的所有组件
 * 2. 确保依赖关系正确装配
 * 3. 提供统一的配置入口
 */
@Configuration
@Import({
    EventOrchestratorService.class
})
public class BlockchainEventConfig {

    // 配置类通过@Import引入所有必要的组件
    // Spring会自动装配这些组件及其依赖
}