package tn.vermeg.gestionproduit.dto;

import java.util.ArrayList;
import java.util.List;
public class ChatbotGarantieRequestDTO {
    
    // Données extraites
    private String originalPrompt;
    private String actionDetectee;
    private Double confianceExtraction;
    
    // Données de la garantie
    private String nomGarantie;
    private String description;
    private String type;
    private Double tauxRemboursement;
    private String typeMontant;
    private Double plafondAnnuel;
    private Double plafondMensuel;
    private Double plafondParActe;
    private Double franchise;
    private Double coutMoyenParSinistre;
    private Integer dureeMinContrat;
    private Integer dureeMaxContrat;
    private Boolean resiliableAnnuellement;
    private String statut;
    private String creePar;
    
    // Métadonnées
    private String businessHash;
    private List<String> warnings;
    private boolean duplicate;
    private String duplicateResolution;

    public ChatbotGarantieRequestDTO() {
        this.warnings = new ArrayList<>();
        this.creePar = "AI-Chatbot";
        this.duplicate = false;
        
        // Valeurs par défaut pour éviter les NPE
        this.tauxRemboursement = 0.0;
        this.plafondAnnuel = 0.0;
        this.plafondMensuel = 0.0;
        this.plafondParActe = 0.0;
        this.franchise = 0.0;
        this.coutMoyenParSinistre = 0.0;
        this.dureeMinContrat = 0;
        this.dureeMaxContrat = 0;
        this.resiliableAnnuellement = false;
        this.confianceExtraction = 0.0;
    }

    // Getters et setters
    public String getOriginalPrompt() { return originalPrompt; }
    public void setOriginalPrompt(String originalPrompt) { this.originalPrompt = originalPrompt; }

    public String getActionDetectee() { return actionDetectee; }
    public void setActionDetectee(String actionDetectee) { this.actionDetectee = actionDetectee; }

    public Double getConfianceExtraction() { return confianceExtraction; }
    public void setConfianceExtraction(Double confianceExtraction) { this.confianceExtraction = confianceExtraction; }

    public String getNomGarantie() { return nomGarantie; }
    public void setNomGarantie(String nomGarantie) { this.nomGarantie = nomGarantie; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Double getTauxRemboursement() { return tauxRemboursement; }
    public void setTauxRemboursement(Double tauxRemboursement) { this.tauxRemboursement = tauxRemboursement; }

    public String getTypeMontant() { return typeMontant; }
    public void setTypeMontant(String typeMontant) { this.typeMontant = typeMontant; }

    public Double getPlafondAnnuel() { return plafondAnnuel; }
    public void setPlafondAnnuel(Double plafondAnnuel) { this.plafondAnnuel = plafondAnnuel; }

    public Double getPlafondMensuel() { return plafondMensuel; }
    public void setPlafondMensuel(Double plafondMensuel) { this.plafondMensuel = plafondMensuel; }

    public Double getPlafondParActe() { return plafondParActe; }
    public void setPlafondParActe(Double plafondParActe) { this.plafondParActe = plafondParActe; }

    public Double getFranchise() { return franchise; }
    public void setFranchise(Double franchise) { this.franchise = franchise; }

    public Double getCoutMoyenParSinistre() { return coutMoyenParSinistre; }
    public void setCoutMoyenParSinistre(Double coutMoyenParSinistre) { this.coutMoyenParSinistre = coutMoyenParSinistre; }

    public Integer getDureeMinContrat() { return dureeMinContrat; }
    public void setDureeMinContrat(Integer dureeMinContrat) { this.dureeMinContrat = dureeMinContrat; }

    public Integer getDureeMaxContrat() { return dureeMaxContrat; }
    public void setDureeMaxContrat(Integer dureeMaxContrat) { this.dureeMaxContrat = dureeMaxContrat; }

    public Boolean getResiliableAnnuellement() { return resiliableAnnuellement; }
    public void setResiliableAnnuellement(Boolean resiliableAnnuellement) { this.resiliableAnnuellement = resiliableAnnuellement; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getCreePar() { return creePar; }
    public void setCreePar(String creePar) { this.creePar = creePar; }

    public String getBusinessHash() { return businessHash; }
    public void setBusinessHash(String businessHash) { this.businessHash = businessHash; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public boolean isDuplicate() { return duplicate; }
    public void setDuplicate(boolean duplicate) { this.duplicate = duplicate; }

    public String getDuplicateResolution() { return duplicateResolution; }
    public void setDuplicateResolution(String duplicateResolution) { this.duplicateResolution = duplicateResolution; }

    // Méthodes utilitaires
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public void applyDefaults() {
        if (this.tauxRemboursement == null) {
            this.tauxRemboursement = 0.8; // 80% par défaut
        }
        if (this.typeMontant == null) {
            this.typeMontant = "FRAIS_REELS";
        }
        if (this.coutMoyenParSinistre == null) {
            this.coutMoyenParSinistre = 100.0;
        }
        if (this.dureeMinContrat == null) {
            this.dureeMinContrat = 12;
        }
        if (this.dureeMaxContrat == null) {
            this.dureeMaxContrat = 60;
        }
        if (this.resiliableAnnuellement == null) {
            this.resiliableAnnuellement = true;
        }
        if (this.statut == null) {
            this.statut = "ACTIF";
        }
        if (this.franchise == null) {
            this.franchise = 0.0;
        }
        
        // Calculer les plafonds dérivés si nécessaire
        if (this.plafondAnnuel != null) {
            if (this.plafondMensuel == null) {
                this.plafondMensuel = this.plafondAnnuel / 12;
            }
            if (this.plafondParActe == null) {
                this.plafondParActe = this.plafondAnnuel / 24;
            }
        }
    }
    public void generateBusinessHash() {
        if (this.nomGarantie != null && this.type != null) {
            this.businessHash = (this.nomGarantie + "_" + this.type).toUpperCase().replaceAll("[^A-Z0-9_]", "_");
        }
    }

    @Override
    public String toString() {
        return String.format("ChatbotGarantieRequestDTO{nom='%s', type='%s', taux=%.2f, warnings=%d}", 
                           nomGarantie, type, tauxRemboursement, warnings.size());
    }
}
