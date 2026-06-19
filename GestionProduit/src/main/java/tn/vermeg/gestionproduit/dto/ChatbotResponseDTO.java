package tn.vermeg.gestionproduit.dto;

import java.util.List;
import java.util.Map;

public class ChatbotResponseDTO {
    
    private boolean success;
    private String action;
    private Object result;
    private String prompt;
    private String message;
    private List<String> errors;
    private List<String> warnings;
    private Long timestamp;
    private Map<String, Object> metadata;

    public ChatbotResponseDTO() {
        this.success = true;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters et setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public Map<String, Object> getData() {
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
        }
        return metadata;
    }

    /**
     * Ajoute un avertissement à la réponse
     */
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new java.util.ArrayList<>();
        }
        this.warnings.add(warning);
    }
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new java.util.ArrayList<>();
        }
        this.errors.add(error);
        this.success = false;
    }

    @Override
    public String toString() {
        return String.format("ChatbotResponseDTO{success=%s, action='%s', warnings=%d, errors=%d}", 
                           success, action, warnings != null ? warnings.size() : 0, 
                           errors != null ? errors.size() : 0);
    }
}
