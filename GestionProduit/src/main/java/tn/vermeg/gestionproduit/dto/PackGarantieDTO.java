package tn.vermeg.gestionproduit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import tn.vermeg.gestionproduit.entities.TypeMontant;

import java.time.Instant;
import java.util.Map;

public class PackGarantieDTO {

    private String idPackGarantie;

    @NotBlank(message = "L'ID du pack est obligatoire")
    private String packId;

    @NotBlank(message = "L'ID de la garantie est obligatoire")
    private String garantieId;

    private String nomGarantie;

    // PARAMÈTRES FINANCIERS SPÉCIFIQUES AU PACK
    @Min(value = 0, message = "Le taux de remboursement doit être entre 0 et 1")
    private double tauxRemboursement;

    @PositiveOrZero(message = "Le plafond ne peut pas être négatif")
    private double plafond;

    @PositiveOrZero(message = "La franchise ne peut pas être négative")
    private double franchise;

    private TypeMontant typeMontant;

    // PARAMÈTRES CONTRACTUELS
    private int delaiCarence; // en jours/mois
    private int priorite;

    private boolean actif;

    private Instant dateActivation;
    private Instant dateDesactivation;

    private boolean optionnelle;
    private double supplementPrix;
    private String condition;
    private int ordreAffichage;

    private Map<String, Object> customFields;

    // CONSTRUCTEURS
    public PackGarantieDTO() {}

    public PackGarantieDTO(String idPackGarantie, String packId, String garantieId,
                          String nomGarantie, double tauxRemboursement, double plafond,
                          double franchise, TypeMontant typeMontant,
                          int delaiCarence, int priorite, String condition, int ordreAffichage, boolean actif,
                          Instant dateActivation, Instant dateDesactivation,
                          boolean optionnelle, double supplementPrix) {
        this.idPackGarantie = idPackGarantie;
        this.packId = packId;
        this.garantieId = garantieId;
        this.nomGarantie = nomGarantie;
        this.tauxRemboursement = tauxRemboursement;
        this.plafond = plafond;
        this.franchise = franchise;
        this.typeMontant = typeMontant;
        this.delaiCarence = delaiCarence;
        this.priorite = priorite;
        this.condition = condition;
        this.ordreAffichage = ordreAffichage;
        this.actif = actif;
        this.dateActivation = dateActivation;
        this.dateDesactivation = dateDesactivation;
        this.optionnelle = optionnelle;
        this.supplementPrix = supplementPrix;
    }

    // GETTERS & SETTERS
    public String getIdPackGarantie() {
        return idPackGarantie;
    }

    public void setIdPackGarantie(String idPackGarantie) {
        this.idPackGarantie = idPackGarantie;
    }

    public String getPackId() {
        return packId;
    }

    public void setPackId(String packId) {
        this.packId = packId;
    }

    public String getGarantieId() {
        return garantieId;
    }

    public void setGarantieId(String garantieId) {
        this.garantieId = garantieId;
    }

    public String getNomGarantie() {
        return nomGarantie;
    }

    public void setNomGarantie(String nomGarantie) {
        this.nomGarantie = nomGarantie;
    }

    public double getTauxRemboursement() {
        return tauxRemboursement;
    }

    public void setTauxRemboursement(double tauxRemboursement) {
        this.tauxRemboursement = tauxRemboursement;
    }

    public double getPlafond() {
        return plafond;
    }

    public void setPlafond(double plafond) {
        this.plafond = plafond;
    }

    public double getFranchise() {
        return franchise;
    }

    public void setFranchise(double franchise) {
        this.franchise = franchise;
    }

    public TypeMontant getTypeMontant() {
        return typeMontant;
    }

    public void setTypeMontant(TypeMontant typeMontant) {
        this.typeMontant = typeMontant;
    }

    public int getDelaiCarence() {
        return delaiCarence;
    }

    public void setDelaiCarence(int delaiCarence) {
        this.delaiCarence = delaiCarence;
    }

    public int getPriorite() {
        return priorite;
    }

    public void setPriorite(int priorite) {
        this.priorite = priorite;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public int getOrdreAffichage() {
        return ordreAffichage;
    }

    public void setOrdreAffichage(int ordreAffichage) {
        this.ordreAffichage = ordreAffichage;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public Instant getDateActivation() {
        return dateActivation;
    }

    public void setDateActivation(Instant dateActivation) {
        this.dateActivation = dateActivation;
    }

    public Instant getDateDesactivation() {
        return dateDesactivation;
    }

    public void setDateDesactivation(Instant dateDesactivation) {
        this.dateDesactivation = dateDesactivation;
    }

    public boolean isOptionnelle() {
        return optionnelle;
    }

    public void setOptionnelle(boolean optionnelle) {
        this.optionnelle = optionnelle;
    }

    public double getSupplementPrix() {
        return supplementPrix;
    }

    public void setSupplementPrix(double supplementPrix) {
        this.supplementPrix = supplementPrix;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }
}
