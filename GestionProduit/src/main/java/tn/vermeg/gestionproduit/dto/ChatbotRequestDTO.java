package tn.vermeg.gestionproduit.dto;

import jakarta.validation.constraints.NotBlank;
public class ChatbotRequestDTO {
    
    @NotBlank(message = "Le prompt est requis")
    private String prompt;
    
    private String sessionId;

    public ChatbotRequestDTO() {}

    public ChatbotRequestDTO(String prompt) {
        this.prompt = prompt;
    }

    public ChatbotRequestDTO(String prompt, String sessionId) {
        this.prompt = prompt;
        this.sessionId = sessionId;
    }

    // Getters et setters
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return String.format("ChatbotRequestDTO{prompt='%s', sessionId='%s'}", 
                           prompt, sessionId != null ? sessionId : "null");
    }
}
