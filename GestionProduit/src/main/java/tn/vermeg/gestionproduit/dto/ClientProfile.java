package tn.vermeg.gestionproduit.dto;

import java.util.ArrayList;
import java.util.List;

public class ClientProfile {
    private Integer age;
    private String gender;
    private String maritalStatus;
    private String profession;
    private Integer numberOfBeneficiaries;
    private List<Beneficiary> beneficiaries;
    private List<String> chronicDiseases;
    private String medicalHistory;
    private List<String> currentMedications;
    private Double monthlyBudget;
    private Double desiredCeiling;
    private String coverageLevel;
    private List<String> desiredGuarantees;
    private List<String> desiredMedicalDomains;
    private String coverageType;
    private String contractType;
    private String insuranceHistory;
    private String preferences;
    private String riskTolerance;
    private Boolean resiliableAnnuellement;

    public static class Beneficiary {
        private String relationship;
        private Integer age;
        private List<String> healthConditions;

        // Getters & Setters
        public String getRelationship() {
            return relationship;
        }

        public void setRelationship(String relationship) {
            this.relationship = relationship;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public List<String> getHealthConditions() {
            return healthConditions;
        }

        public void setHealthConditions(List<String> healthConditions) {
            this.healthConditions = healthConditions;
        }
    }

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

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public Integer getNumberOfBeneficiaries() {
        return numberOfBeneficiaries;
    }

    public void setNumberOfBeneficiaries(Integer numberOfBeneficiaries) {
        this.numberOfBeneficiaries = numberOfBeneficiaries;
    }

    public List<Beneficiary> getBeneficiaries() {
        if (beneficiaries == null) {
            beneficiaries = new ArrayList<>();
        }
        return beneficiaries;
    }

    public void setBeneficiaries(List<Beneficiary> beneficiaries) {
        this.beneficiaries = beneficiaries;
    }

    public List<String> getChronicDiseases() {
        return chronicDiseases;
    }

    public void setChronicDiseases(List<String> chronicDiseases) {
        this.chronicDiseases = chronicDiseases;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    public List<String> getCurrentMedications() {
        return currentMedications;
    }

    public void setCurrentMedications(List<String> currentMedications) {
        this.currentMedications = currentMedications;
    }

    public Double getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(Double monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }

    public Double getDesiredCeiling() {
        return desiredCeiling;
    }

    public void setDesiredCeiling(Double desiredCeiling) {
        this.desiredCeiling = desiredCeiling;
    }

    public String getCoverageLevel() {
        return coverageLevel;
    }

    public void setCoverageLevel(String coverageLevel) {
        this.coverageLevel = coverageLevel;
    }

    public List<String> getDesiredGuarantees() {
        return desiredGuarantees;
    }

    public void setDesiredGuarantees(List<String> desiredGuarantees) {
        this.desiredGuarantees = desiredGuarantees;
    }

    public List<String> getDesiredMedicalDomains() {
        return desiredMedicalDomains;
    }

    public void setDesiredMedicalDomains(List<String> desiredMedicalDomains) {
        this.desiredMedicalDomains = desiredMedicalDomains;
    }

    public String getCoverageType() {
        return coverageType;
    }

    public void setCoverageType(String coverageType) {
        this.coverageType = coverageType;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public Boolean getResiliableAnnuellement() {
        return resiliableAnnuellement;
    }

    public void setResiliableAnnuellement(Boolean resiliableAnnuellement) {
        this.resiliableAnnuellement = resiliableAnnuellement;
    }

    public String getInsuranceHistory() {
        return insuranceHistory;
    }

    public void setInsuranceHistory(String insuranceHistory) {
        this.insuranceHistory = insuranceHistory;
    }

    public String getPreferences() {
        return preferences;
    }

    public void setPreferences(String preferences) {
        this.preferences = preferences;
    }

    public String getRiskTolerance() {
        return riskTolerance;
    }

    public void setRiskTolerance(String riskTolerance) {
        this.riskTolerance = riskTolerance;
    }
}
