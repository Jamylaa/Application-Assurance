package tn.vermeg.gestionuser.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ValidationException extends AppException {
    private final String validationField;

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
        this.validationField = null;
    }

    public ValidationException(String field, String message) {
        super("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
        this.validationField = field;
    }

    public ValidationException(String errorCode, String field, String message) {
        super(errorCode, message, HttpStatus.BAD_REQUEST);
        this.validationField = field;
    }
    public ValidationException(String message, Throwable cause) {
        super("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST, cause);
        this.validationField = null;
    }
    public ValidationException(String field, String message, Throwable cause) {
        super("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST, cause);
        this.validationField = field;
    }
    public ValidationException(String errorCode, String field, String message, Throwable cause) {
        super(errorCode, message, HttpStatus.BAD_REQUEST, cause);
        this.validationField = field;
    }
    public static ValidationException typeGarantie(String message) {
        return new ValidationException("TYPE_GARANTIE_ERROR", "typeGarantie", message);
    }

    public static ValidationException typeGarantie(String message, Throwable cause) {
        return new ValidationException("TYPE_GARANTIE_ERROR", "typeGarantie", message, cause);
    }
}