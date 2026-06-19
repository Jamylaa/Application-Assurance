package tn.vermeg.gestionproduit.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "conversation_memory")
public class ConversationMemory {

    @Id
    private String id;
    @Indexed
    private String userId; // ID de l'utilisateur (peut être l'ID Keycloak ou un identifiant session)

    @Indexed
    private String sessionId; // ID de session pour distinguer différentes conversations

    private String userType; // Type d'utilisateur: "CLIENT", "AGENT", "ADMIN"

    // Profil client extrait des conversations
    private CustomerProfile customerProfile;

    // Historique des échanges
    private List<MessageExchange> messageHistory;

    // Préférences utilisateur déduites
    private UserPreferences preferences;

    // Anciennes recommandations
    private List<RecommendationSnapshot> pastRecommendations;

    // Contexte de la conversation
    private Map<String, Object> context;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // Constructeurs
    public ConversationMemory() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public ConversationMemory(String userId, String sessionId) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters & Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public CustomerProfile getCustomerProfile() {
        return customerProfile;
    }

    public void setCustomerProfile(CustomerProfile customerProfile) {
        this.customerProfile = customerProfile;
    }

    public List<MessageExchange> getMessageHistory() {
        return messageHistory;
    }

    public void setMessageHistory(List<MessageExchange> messageHistory) {
        this.messageHistory = messageHistory;
    }

    public UserPreferences getPreferences() {
        return preferences;
    }

    public void setPreferences(UserPreferences preferences) {
        this.preferences = preferences;
    }

    public List<RecommendationSnapshot> getPastRecommendations() {
        return pastRecommendations;
    }

    public void setPastRecommendations(List<RecommendationSnapshot> pastRecommendations) {
        this.pastRecommendations = pastRecommendations;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Classes imbriquées
    public static class CustomerProfile {
        private Integer age;
        private String gender;
        private String maritalStatus;
        private Integer numberOfChildren;
        private List<String> medicalNeeds;
        private String familySituation;
        private String careFrequency;
        private Double budgetRangeMin;
        private Double budgetRangeMax;

        // Getters & Setters
        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getMaritalStatus() {
            return maritalStatus;
        }

        public void setMaritalStatus(String maritalStatus) {
            this.maritalStatus = maritalStatus;
        }

        public Integer getNumberOfChildren() {
            return numberOfChildren;
        }

        public void setNumberOfChildren(Integer numberOfChildren) {
            this.numberOfChildren = numberOfChildren;
        }

        public List<String> getMedicalNeeds() {
            return medicalNeeds;
        }

        public void setMedicalNeeds(List<String> medicalNeeds) {
            this.medicalNeeds = medicalNeeds;
        }

        public String getFamilySituation() {
            return familySituation;
        }

        public void setFamilySituation(String familySituation) {
            this.familySituation = familySituation;
        }

        public String getCareFrequency() {
            return careFrequency;
        }

        public void setCareFrequency(String careFrequency) {
            this.careFrequency = careFrequency;
        }

        public Double getBudgetRangeMin() {
            return budgetRangeMin;
        }

        public void setBudgetRangeMin(Double budgetRangeMin) {
            this.budgetRangeMin = budgetRangeMin;
        }

        public Double getBudgetRangeMax() {
            return budgetRangeMax;
        }

        public void setBudgetRangeMax(Double budgetRangeMax) {
            this.budgetRangeMax = budgetRangeMax;
        }
    }

    public static class MessageExchange {
        private String role; // "USER" ou "ASSISTANT"
        private String content;
        private Instant timestamp;
        private String action; // Action détectée (RECOMMANDATION, CREATION, etc.)
        private Map<String, Object> metadata;

        // Getters & Setters
        public String getRole() {
            return role;
        }
        public void setRole(String role) {
            this.role = role;
        }
        public String getContent() {
            return content;
        }
        public void setContent(String content) {
            this.content = content;
        }
        public Instant getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }
        public String getAction() {
            return action;
        }
        public void setAction(String action) {
            this.action = action;
        }
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }

    public static class UserPreferences {
        private List<String> preferredCoverageLevels; // GOLD, SILVER, etc.
        private List<String> preferredGuarantees;
        private Double maxMonthlyBudget;
        private String preferredPaymentFrequency;
        private Boolean prefersFamilyCoverage;
        private Boolean prefersComprehensiveCoverage;

        // Getters & Setters
        public List<String> getPreferredCoverageLevels() {
            return preferredCoverageLevels;
        }
        public void setPreferredCoverageLevels(List<String> preferredCoverageLevels) {
            this.preferredCoverageLevels = preferredCoverageLevels;
        }

        public List<String> getPreferredGuarantees() {
            return preferredGuarantees;
        }
        public void setPreferredGuarantees(List<String> preferredGuarantees) {
            this.preferredGuarantees = preferredGuarantees;
        }

        public Double getMaxMonthlyBudget() {
            return maxMonthlyBudget;
        }
        public void setMaxMonthlyBudget(Double maxMonthlyBudget) {
            this.maxMonthlyBudget = maxMonthlyBudget;
        }
        public String getPreferredPaymentFrequency() {
            return preferredPaymentFrequency;
        }

        public void setPreferredPaymentFrequency(String preferredPaymentFrequency) {
            this.preferredPaymentFrequency = preferredPaymentFrequency;
        }

        public Boolean getPrefersFamilyCoverage() {
            return prefersFamilyCoverage;
        }

        public void setPrefersFamilyCoverage(Boolean prefersFamilyCoverage) {
            this.prefersFamilyCoverage = prefersFamilyCoverage;
        }

        public Boolean getPrefersComprehensiveCoverage() {
            return prefersComprehensiveCoverage;
        }
        public void setPrefersComprehensiveCoverage(Boolean prefersComprehensiveCoverage) {
            this.prefersComprehensiveCoverage = prefersComprehensiveCoverage;
        }
    }

    public static class RecommendationSnapshot {
        private Instant timestamp;
        private List<String> recommendedPackIds;
        private List<String> recommendedProductIds;
        private Double averageScore;
        private String userFeedback; // "POSITIVE", "NEGATIVE", "NEUTRAL"

        // Getters & Setters
        public Instant getTimestamp() {
            return timestamp;
        }
        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }
        public List<String> getRecommendedPackIds() {
            return recommendedPackIds;
        }
        public void setRecommendedPackIds(List<String> recommendedPackIds) {
            this.recommendedPackIds = recommendedPackIds;
        }

        public List<String> getRecommendedProductIds() {
            return recommendedProductIds;
        }
        public void setRecommendedProductIds(List<String> recommendedProductIds) {
            this.recommendedProductIds = recommendedProductIds;
        }

        public Double getAverageScore() {
            return averageScore;
        }
        public void setAverageScore(Double averageScore) {
            this.averageScore = averageScore;
        }
        public String getUserFeedback() {
            return userFeedback;
        }
        public void setUserFeedback(String userFeedback) {
            this.userFeedback = userFeedback;
        }
    }
}
