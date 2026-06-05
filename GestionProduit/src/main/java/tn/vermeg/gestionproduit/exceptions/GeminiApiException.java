package tn.vermeg.gestionproduit.exceptions;

public class GeminiApiException extends RuntimeException {
    
    private final String errorType;
    
    public GeminiApiException(String message) {
        super(message);
        this.errorType = "GENERIC_ERROR";
    }
    
    public GeminiApiException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = "GENERIC_ERROR";
    }
    
    public GeminiApiException(String message, String errorType) {
        super(message);
        this.errorType = errorType;
    }
    
    public GeminiApiException(String message, String errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }
    
    public String getErrorType() {
        return errorType;
    }
}
