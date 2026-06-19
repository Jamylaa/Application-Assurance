package tn.vermeg.gestionproduit.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
// Gère les exceptions de validation (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        logger.warn("Erreur de validation: {}", errors);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("VALIDATION_ERROR")
            .message("Erreur de validation des données")
            .details(errors)
            .path(request.getDescription(false))
            .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }
//Gère les exceptions d'arguments illégaux
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        logger.error("Argument illégal: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("ILLEGAL_ARGUMENT")
            .message(ex.getMessage())
            .path(request.getDescription(false))
            .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }
// Gère les exceptions de ressource non trouvée
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        logger.warn("Ressource non trouvée: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("RESOURCE_NOT_FOUND")
            .message(ex.getMessage())
            .path(request.getDescription(false))
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    // Gère les exceptions générales
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        logger.error("Erreur non gérée: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("INTERNAL_SERVER_ERROR")
            .message("Une erreur interne est survenue")
            .path(request.getDescription(false))
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
        //dto pour les réponses d'erreur
     public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private Map<String, String> details;
        private String path;
        public static Builder builder() {
            return new Builder();
        }

        // Getters et Setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Map<String, String> getDetails() { return details; }
        public void setDetails(Map<String, String> details) { this.details = details; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public static class Builder {
            private ErrorResponse errorResponse = new ErrorResponse();
            public Builder timestamp(LocalDateTime timestamp) {errorResponse.timestamp = timestamp;return this;}
            public Builder status(int status) {errorResponse.status = status;return this;}
            public Builder error(String error) {errorResponse.error = error;return this;}
            public Builder message(String message) {errorResponse.message = message;return this;}
            public Builder details(Map<String, String> details) {errorResponse.details = details;return this;}
            public Builder path(String path) {errorResponse.path = path;return this;}
            public ErrorResponse build() {
                return errorResponse;
            }
        }
    }
}
