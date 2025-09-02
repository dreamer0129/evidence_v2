package cn.edu.gfkd.evidence.exception;

public class EvidenceNotFoundException extends RuntimeException {
    public EvidenceNotFoundException(String message) {
        super(message);
    }

    public EvidenceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}