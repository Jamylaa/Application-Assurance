package tn.vermeg.gestionproduit.exceptions;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final String resourceType;
    private final String resourceId;
    private final String errorCode;
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s with id '%s' not found", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.errorCode = "RESOURCE_NOT_FOUND";
    }

    public ResourceNotFoundException(String resourceType, String resourceId, String message) {
        super(message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.errorCode = "RESOURCE_NOT_FOUND";
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceType = null;
        this.resourceId = null;
        this.errorCode = "BUSINESS_ERROR";
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.resourceType = null;
        this.resourceId = null;
        this.errorCode = "BUSINESS_ERROR";
    }

    public ResourceNotFoundException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.resourceType = null;
        this.resourceId = null;
        this.errorCode = errorCode;
    }
}
