package tn.vermeg.gestionproduit.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatbotPromptRequestDTO {

    @NotBlank(message = "Le champ 'prompt' est obligatoire")
    private String prompt;

    public ChatbotPromptRequestDTO() {
    }
    public ChatbotPromptRequestDTO(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}

