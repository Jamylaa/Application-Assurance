package tn.vermeg.gestionproduit.exceptions;
import lombok.Data;
import java.util.Map;
@Data
public class ErrorResponse {
    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> details;
    public ErrorResponse(int status, String error, String message, String path) {
        this.timestamp = java.time.LocalDateTime.now().toString();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
}