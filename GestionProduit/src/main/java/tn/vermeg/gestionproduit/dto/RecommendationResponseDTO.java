package tn.vermeg.gestionproduit.dto;

import java.util.List;

/**
 * @deprecated Recommendation logic has been moved to the Python chatbot-service.
 * This DTO is kept for backward compatibility but should not be used.
 * Use the chatbot-service API for recommendation functionality instead.
 */
@Deprecated
public class RecommendationResponseDTO {
    private String sessionId;
    private List<RecommendationResultDTO> recommendedPacks;
    private List<RecommendationResultDTO> recommendedProducts;
    private String explanation;
    private boolean success;
    private String message;

    // Getters & Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<RecommendationResultDTO> getRecommendedPacks() {
        return recommendedPacks;
    }

    public void setRecommendedPacks(List<RecommendationResultDTO> recommendedPacks) {
        this.recommendedPacks = recommendedPacks;
    }

    public List<RecommendationResultDTO> getRecommendedProducts() {
        return recommendedProducts;
    }

    public void setRecommendedProducts(List<RecommendationResultDTO> recommendedProducts) {
        this.recommendedProducts = recommendedProducts;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Méthodes pour compatibilité avec ConversationMemoryService
    public List<RecommendationResultDTO> getTopPacks() {
        return recommendedPacks;
    }

    public List<RecommendationResultDTO> getTopProducts() {
        return recommendedProducts;
    }

    public String getId() {
        return sessionId;
    }
}
