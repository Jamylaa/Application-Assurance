package tn.vermeg.gestionuser.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ResourceException extends AppException {
    private final String resourceType;
    private final String resourceId;
    private final OperationType operationType;

    public enum OperationType {
        NOT_FOUND("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND),
        ALREADY_EXISTS("RESOURCE_ALREADY_EXISTS", HttpStatus.CONFLICT),
        CONFLICT("RESOURCE_CONFLICT", HttpStatus.CONFLICT);

        private final String errorCode;
        private final HttpStatus httpStatus;

        OperationType(String errorCode, HttpStatus httpStatus) {
            this.errorCode = errorCode;
            this.httpStatus = httpStatus;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public HttpStatus getHttpStatus() {
            return httpStatus;
        }
    }
    public ResourceException(String resourceType, String resourceId) {
        super(
            OperationType.NOT_FOUND.getErrorCode(),
            String.format("%s with id '%s' not found", resourceType, resourceId),
            OperationType.NOT_FOUND.getHttpStatus()
        );
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.operationType = OperationType.NOT_FOUND;
    }

    public ResourceException(String resourceType, String resourceId, String message) {
        super(
            OperationType.NOT_FOUND.getErrorCode(),
            message,
            OperationType.NOT_FOUND.getHttpStatus()
        );
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.operationType = OperationType.NOT_FOUND;
    }
    public ResourceException(OperationType operationType, String resourceType, String resourceId) {
        super(
            operationType.getErrorCode(),
            buildDefaultMessage(operationType, resourceType, resourceId),
            operationType.getHttpStatus()
        );
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.operationType = operationType;
    }
    public ResourceException(OperationType operationType, String resourceType, String resourceId, String message) {
        super(
            operationType.getErrorCode(),
            message,
            operationType.getHttpStatus()
        );
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.operationType = operationType;
    }
    public ResourceException(OperationType operationType, String resourceType, String resourceId, String message, Throwable cause) {
        super(
            operationType.getErrorCode(),
            message,
            operationType.getHttpStatus(),
            cause
        );
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.operationType = operationType;
    }

    private static String buildDefaultMessage(OperationType type, String resourceType, String resourceId) {
        return switch (type) {
            case NOT_FOUND -> String.format("%s with id '%s' not found", resourceType, resourceId);
            case ALREADY_EXISTS -> String.format("%s with id '%s' already exists", resourceType, resourceId);
            case CONFLICT -> String.format("Conflict for %s with id '%s'", resourceType, resourceId);
        };
    }
}