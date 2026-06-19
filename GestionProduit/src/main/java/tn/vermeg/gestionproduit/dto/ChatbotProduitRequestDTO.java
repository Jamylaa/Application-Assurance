package tn.vermeg.gestionproduit.dto;

import java.util.ArrayList;
import java.util.List;

public class ChatbotProduitRequestDTO {
    
    // Données extraites
    private String originalPrompt;
    private String actionDetectee;
    private Double confianceExtraction;
    
    // Données du produit
    private String nomProduit;
    private String description;
    private String typeProduit;
    private String statut;
    
    // Métadonnées
    private String businessHash;
    private List<String> warnings;
    private boolean duplicate;
    private String duplicateResolution;

    public ChatbotProduitRequestDTO() {
        this.warnings = new ArrayList<>();
        this.duplicate = false;
    }

    // Getters et setters
    public String getOriginalPrompt() { return originalPrompt; }
    public void setOriginalPrompt(String originalPrompt) { this.originalPrompt = originalPrompt; }
    public String getActionDetectee() { return actionDetectee; }
    public void setActionDetectee(String actionDetectee) { this.actionDetectee = actionDetectee; }
    public Double getConfianceExtraction() { return confianceExtraction; }
    public void setConfianceExtraction(Double confianceExtraction) { this.confianceExtraction = confianceExtraction; }
    public String getNomProduit() { return nomProduit; }
    public void setNomProduit(String nomProduit) { this.nomProduit = nomProduit; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTypeProduit() { return typeProduit; }
    public void setTypeProduit(String typeProduit) { this.typeProduit = typeProduit; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
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
        if (this.statut == null) {
            this.statut = "ACTIF";
        }
    }
    public void generateBusinessHash() {
        if (this.nomProduit != null && this.typeProduit != null) {
            this.businessHash = (this.nomProduit + "_" + this.typeProduit).toUpperCase().replaceAll("[^A-Z0-9_]", "_");
        }
    }
    @Override
    public String toString() {
        return String.format("ChatbotProduitRequestDTO{nom='%s', type='%s', statut='%s', warnings=%d}", 
                           nomProduit, typeProduit, statut, warnings.size());
    }
}