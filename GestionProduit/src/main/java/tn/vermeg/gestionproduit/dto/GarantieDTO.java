package tn.vermeg.gestionproduit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import tn.vermeg.gestionproduit.entities.Statut;
import tn.vermeg.gestionproduit.entities.TypeMontant;
import tn.vermeg.gestionproduit.entities.TypePlafond;

import java.time.Instant;
import java.util.Map;

public class GarantieDTO {

    private String idGarantie;

    @NotBlank(message = "Le nom de la garantie est obligatoire")
    private String nomGarantie;

    private String description;

    @NotNull(message = "Le statut est obligatoire")
    private Statut statut;

    @NotBlank(message = "Le type de garantie est obligatoire")
    private String type;

    // PARAMÈTRES FINANCIERS
    @Min(value = 0, message = "Le taux de remboursement doit être entre 0 et 1")
    @Max(value = 1, message = "Le taux de remboursement doit être entre 0 et 1")
    private double tauxRemboursement;

    private TypeMontant typeMontant;
    private TypePlafond typePlafond;

    @PositiveOrZero(message = "Le plafond annuel ne peut pas être négatif")
    private double plafondAnnuel;

    @PositiveOrZero(message = "Le plafond mensuel ne peut pas être négatif")
    private double plafondMensuel;

    @PositiveOrZero(message = "Le plafond par acte ne peut pas être négatif")
    private double plafondParActe;

    @PositiveOrZero(message = "La franchise ne peut pas être négative")
    private double franchise;

    @PositiveOrZero(message = "Le coût moyen par sinistre ne peut pas être négatif")
    private double coutMoyenParSinistre;

    // PARAMÈTRES CONTRACTUELS
    private int dureeMinContrat;
    private int dureeMaxContrat;
    private boolean resiliableAnnuellement;

    // AUDIT
    private String creePar;
    private Instant dateCreation;
    private Instant dateModification;
    private Instant dateDesactivation;

    private Map<String, Object> customFields;

    // CONSTRUCTEURS
    public GarantieDTO() {}

    public GarantieDTO(String idGarantie, String nomGarantie, String description,
                       Statut statut, String type, double tauxRemboursement,
                       TypeMontant typeMontant, TypePlafond typePlafond,
                       double plafondAnnuel, double plafondMensuel, double plafondParActe,
                       double franchise, double coutMoyenParSinistre,
                       int dureeMinContrat, int dureeMaxContrat, boolean resiliableAnnuellement) {
        this.idGarantie = idGarantie;
        this.nomGarantie = nomGarantie;
        this.description = description;
        this.statut = statut;
        this.type = type;
        this.tauxRemboursement = tauxRemboursement;
        this.typeMontant = typeMontant;
        this.typePlafond = typePlafond;
        this.plafondAnnuel = plafondAnnuel;
        this.plafondMensuel = plafondMensuel;
        this.plafondParActe = plafondParActe;
        this.franchise = franchise;
        this.coutMoyenParSinistre = coutMoyenParSinistre;
        this.dureeMinContrat = dureeMinContrat;
        this.dureeMaxContrat = dureeMaxContrat;
        this.resiliableAnnuellement = resiliableAnnuellement;
    }

    // GETTERS & SETTERS
    public String getIdGarantie() {
        return idGarantie;
    }

    public void setIdGarantie(String idGarantie) {
        this.idGarantie = idGarantie;
    }

    public String getNomGarantie() {
        return nomGarantie;
    }

    public void setNomGarantie(String nomGarantie) {
        this.nomGarantie = nomGarantie;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getTauxRemboursement() {
        return tauxRemboursement;
    }

    public void setTauxRemboursement(double tauxRemboursement) {
        this.tauxRemboursement = tauxRemboursement;
    }

    public TypeMontant getTypeMontant() {
        return typeMontant;
    }

    public void setTypeMontant(TypeMontant typeMontant) {
        this.typeMontant = typeMontant;
    }

    public TypePlafond getTypePlafond() {
        return typePlafond;
    }

    public void setTypePlafond(TypePlafond typePlafond) {
        this.typePlafond = typePlafond;
    }

    public double getPlafondAnnuel() {
        return plafondAnnuel;
    }

    public void setPlafondAnnuel(double plafondAnnuel) {
        this.plafondAnnuel = plafondAnnuel;
    }

    public double getPlafondMensuel() {
        return plafondMensuel;
    }

    public void setPlafondMensuel(double plafondMensuel) {
        this.plafondMensuel = plafondMensuel;
    }

    public double getPlafondParActe() {
        return plafondParActe;
    }

    public void setPlafondParActe(double plafondParActe) {
        this.plafondParActe = plafondParActe;
    }

    public double getFranchise() {
        return franchise;
    }

    public void setFranchise(double franchise) {
        this.franchise = franchise;
    }

    public double getCoutMoyenParSinistre() {
        return coutMoyenParSinistre;
    }

    public void setCoutMoyenParSinistre(double coutMoyenParSinistre) {
        this.coutMoyenParSinistre = coutMoyenParSinistre;
    }

    public int getDureeMinContrat() {
        return dureeMinContrat;
    }

    public void setDureeMinContrat(int dureeMinContrat) {
        this.dureeMinContrat = dureeMinContrat;
    }

    public int getDureeMaxContrat() {
        return dureeMaxContrat;
    }

    public void setDureeMaxContrat(int dureeMaxContrat) {
        this.dureeMaxContrat = dureeMaxContrat;
    }

    public boolean isResiliableAnnuellement() {
        return resiliableAnnuellement;
    }

    public void setResiliableAnnuellement(boolean resiliableAnnuellement) {
        this.resiliableAnnuellement = resiliableAnnuellement;
    }

    public String getCreePar() {
        return creePar;
    }

    public void setCreePar(String creePar) {
        this.creePar = creePar;
    }

    public Instant getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Instant dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Instant getDateModification() {
        return dateModification;
    }

    public void setDateModification(Instant dateModification) {
        this.dateModification = dateModification;
    }

    public Instant getDateDesactivation() {
        return dateDesactivation;
    }

    public void setDateDesactivation(Instant dateDesactivation) {
        this.dateDesactivation = dateDesactivation;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }
}
