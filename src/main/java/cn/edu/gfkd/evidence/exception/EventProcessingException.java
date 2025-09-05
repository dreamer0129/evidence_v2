package cn.edu.gfkd.evidence.exception;

/**
 * 事件处理异常类
 * 
 * 用于标识事件处理过程中的特定异常
 */
public class EventProcessingException extends BlockchainException {

    private final String eventType;
    private final String eventId;
    private final boolean retryable;

    public EventProcessingException(String message, String eventType, String eventId) {
        super(message);
        this.eventType = eventType;
        this.eventId = eventId;
        this.retryable = true; // 默认可重试
    }

    public EventProcessingException(String message, String eventType, String eventId, Throwable cause) {
        super(message, cause);
        this.eventType = eventType;
        this.eventId = eventId;
        this.retryable = true; // 默认可重试
    }

    public EventProcessingException(String message, String eventType, String eventId, boolean retryable) {
        super(message);
        this.eventType = eventType;
        this.eventId = eventId;
        this.retryable = retryable;
    }

    public EventProcessingException(String message, String eventType, String eventId, Throwable cause, boolean retryable) {
        super(message, cause);
        this.eventType = eventType;
        this.eventId = eventId;
        this.retryable = retryable;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventId() {
        return eventId;
    }

    public boolean isRetryable() {
        return retryable;
    }

    @Override
    public String getMessage() {
        return String.format("EventProcessingException[eventType=%s, eventId=%s, retryable=%s]: %s", 
                eventType, eventId, retryable, super.getMessage());
    }
}