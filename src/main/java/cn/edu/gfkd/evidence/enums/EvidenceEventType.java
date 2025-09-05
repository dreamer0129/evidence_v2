package cn.edu.gfkd.evidence.enums;

/**
 * Evidence事件类型枚举
 * 
 * 定义了区块链证据管理系统中支持的三种核心事件类型：
 * 1. EvidenceSubmitted - 证据提交事件
 * 2. EvidenceStatusChanged - 证据状态变更事件  
 * 3. EvidenceRevoked - 证据撤销事件
 */
public enum EvidenceEventType {
    
    /**
     * 证据提交事件
     * 当新证据被提交到区块链时触发
     */
    EVIDENCE_SUBMITTED("EvidenceSubmitted", "证据提交事件"),
    
    /**
     * 证据状态变更事件
     * 当证据状态发生变化时触发（如从pending变为effective）
     */
    EVIDENCE_STATUS_CHANGED("EvidenceStatusChanged", "证据状态变更事件"),
    
    /**
     * 证据撤销事件
     * 当证据被撤销时触发
     */
    EVIDENCE_REVOKED("EvidenceRevoked", "证据撤销事件");
    
    private final String eventName;
    private final String description;
    
    EvidenceEventType(String eventName, String description) {
        this.eventName = eventName;
        this.description = description;
    }
    
    /**
     * 获取事件名称
     * 
     * @return 事件名称字符串
     */
    public String getEventName() {
        return eventName;
    }
    
    /**
     * 获取事件描述
     * 
     * @return 事件描述信息
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据事件名称查找对应的枚举值
     * 
     * @param eventName 事件名称
     * @return 对应的枚举值，如果不存在则返回null
     */
    public static EvidenceEventType fromEventName(String eventName) {
        if (eventName == null) {
            return null;
        }
        
        for (EvidenceEventType type : values()) {
            if (type.eventName.equals(eventName)) {
                return type;
            }
        }
        
        return null;
    }
    
    /**
     * 检查给定的事件名称是否为有效的事件类型
     * 
     * @param eventName 事件名称
     * @return 如果是有效的事件类型返回true，否则返回false
     */
    public static boolean isValidEventType(String eventName) {
        return fromEventName(eventName) != null;
    }
}