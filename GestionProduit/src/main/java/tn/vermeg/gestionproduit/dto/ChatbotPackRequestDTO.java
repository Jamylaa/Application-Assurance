package tn.vermeg.gestionproduit.dto;

import tn.vermeg.gestionproduit.entities.TypeClient;
import java.util.ArrayList;
import java.util.List;

public class ChatbotPackRequestDTO {
    
    // Données extraites
    private String originalPrompt;
    private String actionDetectee;
    private Double confianceExtraction;
    
    // Données du pack
    private String nomPack;
    private String description;
    private Integer ageMinimum;
    private Integer ageMaximum;
    private List<TypeClient> typeClients;
    private Integer ancienneteContratMois;
    private String couvertureGeographique;
    private Double prixMensuel;
    private Integer dureeMinContrat;
    private Integer dureeMaxContrat;
    private String niveauCouverture;
    private String statut;
    
    // Liaison avec produit
    private String produitId;
    private String nomProduit;
    
    // Métadonnées
    private String businessHash;
    private List<String> warnings;
    private boolean duplicate;
    private String duplicateResolution;

    public ChatbotPackRequestDTO() {
        this.warnings = new ArrayList<>();
        this.typeClients = new ArrayList<>();
        this.duplicate = false;
    }

    // Getters et setters
    public String getOriginalPrompt() { return originalPrompt; }
    public void setOriginalPrompt(String originalPrompt) { this.originalPrompt = originalPrompt; }

    public String getActionDetectee() { return actionDetectee; }
    public void setActionDetectee(String actionDetectee) { this.actionDetectee = actionDetectee; }

    public Double getConfianceExtraction() { return confianceExtraction; }
    public void setConfianceExtraction(Double confianceExtraction) { this.confianceExtraction = confianceExtraction; }

    public String getNomPack() { return nomPack; }
    public void setNomPack(String nomPack) { this.nomPack = nomPack; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getAgeMinimum() { return ageMinimum; }
    public void setAgeMinimum(Integer ageMinimum) { this.ageMinimum = ageMinimum; }

    public Integer getAgeMaximum() { return ageMaximum; }
    public void setAgeMaximum(Integer ageMaximum) { this.ageMaximum = ageMaximum; }

    public List<TypeClient> getTypeClients() { return typeClients; }
    public void setTypeClients(List<TypeClient> typeClients) { this.typeClients = typeClients; }

    public Integer getAncienneteContratMois() { return ancienneteContratMois; }
    public void setAncienneteContratMois(Integer ancienneteContratMois) { this.ancienneteContratMois = ancienneteContratMois; }

    public String getCouvertureGeographique() { return couvertureGeographique; }
    public void setCouvertureGeographique(String couvertureGeographique) { this.couvertureGeographique = couvertureGeographique; }

    public Double getPrixMensuel() { return prixMensuel; }
    public void setPrixMensuel(Double prixMensuel) { this.prixMensuel = prixMensuel; }

    public Integer getDureeMinContrat() { return dureeMinContrat; }
    public void setDureeMinContrat(Integer dureeMinContrat) { this.dureeMinContrat = dureeMinContrat; }

    public Integer getDureeMaxContrat() { return dureeMaxContrat; }
    public void setDureeMaxContrat(Integer dureeMaxContrat) { this.dureeMaxContrat = dureeMaxContrat; }

    public String getNiveauCouverture() { return niveauCouverture; }
    public void setNiveauCouverture(String niveauCouverture) { this.niveauCouverture = niveauCouverture; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getProduitId() { return produitId; }
    public void setProduitId(String produitId) { this.produitId = produitId; }

    public String getNomProduit() { return nomProduit; }
    public void setNomProduit(String nomProduit) { this.nomProduit = nomProduit; }

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
        if (this.ageMinimum == null) {
            this.ageMinimum = 0;
        }
        if (this.ageMaximum == null) {
            this.ageMaximum = 120;
        }
        if (this.typeClients == null || this.typeClients.isEmpty()) {
            this.typeClients = List.of(TypeClient.INDIVIDUEL);
        }
        if (this.ancienneteContratMois == null) {
            this.ancienneteContratMois = 0;
        }
        if (this.couvertureGeographique == null) {
            this.couvertureGeographique = "NATIONAL";
        }
        if (this.dureeMinContrat == null) {
            this.dureeMinContrat = 12;
        }
        if (this.dureeMaxContrat == null) {
            this.dureeMaxContrat = 60;
        }
        if (this.niveauCouverture == null) {
            this.niveauCouverture = "STANDARD";
        }
        if (this.statut == null) {
            this.statut = "ACTIF";
        }
    }
    public void generateBusinessHash() {
        if (this.nomPack != null && this.nomProduit != null) {
            this.businessHash = (this.nomPack + "_" + this.nomProduit).toUpperCase().replaceAll("[^A-Z0-9_]", "_");
        }
    }
    @Override
    public String toString() {
        return String.format("ChatbotPackRequestDTO{nom='%s', produit='%s', prix=%.2f, warnings=%d}", 
                           nomPack, nomProduit, prixMensuel, warnings.size());
    }
}
