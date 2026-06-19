package tn.vermeg.gestionproduit.dto;

import java.util.List;

public class RecommendationRequestDTO {
    private String sessionId;
    private Integer age;
    private String gender;
    private String maritalStatus;
    private Integer numberOfChildren;
    private String medicalNeeds;
    private String familySituation;
    private String careFrequency;
    private Double monthlyBudget;
    private List<String> chronicDiseases;

    // Getters & Setters
    public String getSessionId() {
        return sessionId;
    }
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
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
    public String getMedicalNeeds() {
        return medicalNeeds;
    }
    public void setMedicalNeeds(String medicalNeeds) {
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
    public Double getMonthlyBudget() {
        return monthlyBudget;
    }
    public void setMonthlyBudget(Double monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }
    public List<String> getChronicDiseases() {
        return chronicDiseases;
    }
    public void setChronicDiseases(List<String> chronicDiseases) {
        this.chronicDiseases = chronicDiseases;
    }
}
