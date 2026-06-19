package tn.vermeg.gestionproduit.dto;

public class RecommendationResultDTO {
    private String id;
    private String nom;
    private String description;
    private double compatibilityScore;
    private ScoringResult scoringResult;
    private String whyRecommended;
    private String whyNotRecommended;
    private String explanation;
    private Double monthlyPrice;
    private String coverageLevel;

    // Getters & Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getCompatibilityScore() {
        return compatibilityScore;
    }

    public void setCompatibilityScore(double compatibilityScore) {
        this.compatibilityScore = compatibilityScore;
    }

    public ScoringResult getScoringResult() {
        return scoringResult;
    }

    public void setScoringResult(ScoringResult scoringResult) {
        this.scoringResult = scoringResult;
    }

    public String getWhyRecommended() {
        return whyRecommended;
    }

    public void setWhyRecommended(String whyRecommended) {
        this.whyRecommended = whyRecommended;
    }

    public String getWhyNotRecommended() {
        return whyNotRecommended;
    }

    public void setWhyNotRecommended(String whyNotRecommended) {
        this.whyNotRecommended = whyNotRecommended;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Double getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(Double monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public String getCoverageLevel() {
        return coverageLevel;
    }

    public void setCoverageLevel(String coverageLevel) {
        this.coverageLevel = coverageLevel;
    }
}
