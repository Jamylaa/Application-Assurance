package tn.vermeg.gestionproduit.dto;
import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated Recommendation logic has been moved to the Python chatbot-service.
 * This DTO is kept for backward compatibility but should not be used.
 * Use the chatbot-service API for recommendation functionality instead.
 */
@Deprecated
public class ScoringResult {
    private double globalScore;
    private double ageScore;
    private double budgetScore;
    private double beneficiaryScore;
    private double medicalRiskScore;
    private double guaranteeMatchScore;
    private double coverageLevelScore;
    private double genderScore;
    private double ceilingScore;
    private double contractTypeScore;
    private double compatibilityScore;
    private double exclusionScore;
    private double medicalPriorityScore;
    private double coherenceScore;
    private Map<String, Double> weights;
    private Map<String, String> scoringDetails;
    private String justification;
    private boolean recommended;
    private boolean hasExclusions;
    private boolean exceedsBudget;
    private boolean respectsMinimums;

    // Getters & Setters
    public double getGlobalScore() {
        return globalScore;
    }
    public void setGlobalScore(double globalScore) {
        this.globalScore = globalScore;
    }
    public double getAgeScore() {
        return ageScore;
    }
    public void setAgeScore(double ageScore) {
        this.ageScore = ageScore;
    }
    public double getBudgetScore() {
        return budgetScore;
    }
    public void setBudgetScore(double budgetScore) {
        this.budgetScore = budgetScore;
    }
    public double getBeneficiaryScore() {
        return beneficiaryScore;
    }
    public void setBeneficiaryScore(double beneficiaryScore) {
        this.beneficiaryScore = beneficiaryScore;
    }
    public double getMedicalRiskScore() {
        return medicalRiskScore;
    }
    public void setMedicalRiskScore(double medicalRiskScore) {
        this.medicalRiskScore = medicalRiskScore;
    }
    public double getGuaranteeMatchScore() {
        return guaranteeMatchScore;
    }
    public void setGuaranteeMatchScore(double guaranteeMatchScore) {
        this.guaranteeMatchScore = guaranteeMatchScore;
    }
    public double getCoverageLevelScore() {
        return coverageLevelScore;
    }
    public void setCoverageLevelScore(double coverageLevelScore) {
        this.coverageLevelScore = coverageLevelScore;
    }
    public double getGenderScore() {
        return genderScore;
    }
    public void setGenderScore(double genderScore) {
        this.genderScore = genderScore;
    }
    public double getCeilingScore() {
        return ceilingScore;
    }
    public void setCeilingScore(double ceilingScore) {
        this.ceilingScore = ceilingScore;
    }
    public double getContractTypeScore() {
        return contractTypeScore;
    }
    public void setContractTypeScore(double contractTypeScore) {
        this.contractTypeScore = contractTypeScore;
    }
    public double getCompatibilityScore() {
        return compatibilityScore;
    }
    public void setCompatibilityScore(double compatibilityScore) {
        this.compatibilityScore = compatibilityScore;
    }
    public double getExclusionScore() {
        return exclusionScore;
    }
    public void setExclusionScore(double exclusionScore) {
        this.exclusionScore = exclusionScore;
    }
    public double getMedicalPriorityScore() {
        return medicalPriorityScore;
    }
    public void setMedicalPriorityScore(double medicalPriorityScore) {this.medicalPriorityScore = medicalPriorityScore;}
    public double getCoherenceScore() {
        return coherenceScore;
    }
    public void setCoherenceScore(double coherenceScore) {
        this.coherenceScore = coherenceScore;
    }
    public Map<String, Double> getWeights() {
        if (weights == null) {weights = new HashMap<>();}
        return weights;
    }
    public void setWeights(Map<String, Double> weights) {
        this.weights = weights;
    }
    public Map<String, String> getScoringDetails() {
        if (scoringDetails == null) {scoringDetails = new HashMap<>();}
        return scoringDetails;
    }

    public void setScoringDetails(Map<String, String> scoringDetails) {
        this.scoringDetails = scoringDetails;
    }
    public String getJustification() {
        return justification;
    }
    public void setJustification(String justification) {
        this.justification = justification;
    }
    public boolean isRecommended() {
        return recommended;
    }
    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }
    public boolean isHasExclusions() {
        return hasExclusions;
    }
    public void setHasExclusions(boolean hasExclusions) {
        this.hasExclusions = hasExclusions;
    }
    public boolean isExceedsBudget() {
        return exceedsBudget;
    }
    public void setExceedsBudget(boolean exceedsBudget) {
        this.exceedsBudget = exceedsBudget;
    }
    public boolean isRespectsMinimums() {
        return respectsMinimums;
    }
    public void setRespectsMinimums(boolean respectsMinimums) {
        this.respectsMinimums = respectsMinimums;
    }
}