package tn.vermeg.gestionproduit.exceptions;

public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;

    public ApiException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        RESOURCE_NOT_FOUND,
        RESOURCE_ALREADY_EXISTS,
        VALIDATION_ERROR,
        CONFLICT_ERROR,
        INVALID_STATE,
        INTERNAL_ERROR
    }
}
