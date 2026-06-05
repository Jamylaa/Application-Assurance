package tn.vermeg.gestionproduit.exceptions;

public class ResourceException extends RuntimeException {
    private final OperationType operationType;
    private final String resourceType;
    private final String resourceId;

    public ResourceException(OperationType operationType, String resourceType, String resourceId, String message) {
        super(message);
        this.operationType = operationType;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public enum OperationType {
        NOT_FOUND,
        ALREADY_EXISTS,
        VALIDATION_ERROR,
        CONFLICT,
        DELETION_FAILED,
        UPDATE_FAILED,
        CREATION_FAILED
    }
}
