package cn.edu.gfkd.evidence.exception;

public class CertificateGenerationException extends RuntimeException {
    
    public CertificateGenerationException(String message) {
        super(message);
    }
    
    public CertificateGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CertificateGenerationException(Throwable cause) {
        super(cause);
    }
}