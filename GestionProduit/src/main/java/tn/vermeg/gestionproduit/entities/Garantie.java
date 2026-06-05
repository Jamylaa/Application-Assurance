package tn.vermeg.gestionproduit.entities;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;

@Document(collection = "garanties")
public class Garantie {

    @Id
    private String idGarantie;

    private String nomGarantie;
    private String description;

    @NotNull(message = "Le statut est obligatoire")
    private Statut statut;
    private Map<String, Object> customFields;

    @NotBlank(message = "Le type de garantie est obligatoire")
    @Size(min = 2, max = 50, message = "Le type doit contenir entre 2 et 50 caractères")
    @Field("type")
   // @JsonAlias({"typeGarantie", "type_garantie"})
    private String type;

    // PARAMÈTRES FINANCIERS

    private double tauxRemboursement; // 0 → 1 recommandé
    private TypeMontant typeMontant;
    private TypePlafond typePlafond;

    private double plafondAnnuel;
    private double plafondMensuel;
    private double plafondParActe;

    private double franchise;

    private double coutMoyenParSinistre;

    // PARAMÈTRES CONTRACTUELS
    private int dureeMinContrat;
    private int dureeMaxContrat;

    private boolean resiliableAnnuellement;

    // AUDIT
    private String creePar;
    @CreatedDate
    private Instant dateCreation;
    @LastModifiedDate
    private Instant dateModification;
    private Instant dateDesactivation;

    // MÉTHODES MÉTIER
    public boolean estValide() {
        return tauxRemboursement >= 0
                && tauxRemboursement <= 1
                && plafondAnnuel >= 0
                && franchise >= 0;
    }
    public boolean estActive() {
        return statut == Statut.ACTIF && dateDesactivation == null;
    }
    // CONSTRUCTEURS
    public Garantie() {}

    public Garantie(String idGarantie, String nomGarantie, String description, Statut statut, Map<String, Object> customFields, String type, double tauxRemboursement, TypeMontant typeMontant, TypePlafond typePlafond, double plafondAnnuel, double plafondMensuel, double plafondParActe, double franchise, double coutMoyenParSinistre, int dureeMinContrat, int dureeMaxContrat, boolean resiliableAnnuellement, String creePar, Instant dateCreation, Instant dateModification, Instant dateDesactivation) {
        this.idGarantie = idGarantie;
        this.nomGarantie = nomGarantie;
        this.description = description;
        this.statut = statut;
        this.customFields = customFields;
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
        this.creePar = creePar;
        this.dateCreation = dateCreation;
        this.dateModification = dateModification;
        this.dateDesactivation = dateDesactivation;
    }

    // GETTERS & SETTERS
    public String getIdGarantie() { return idGarantie; }
    public void setIdGarantie(String idGarantie) { this.idGarantie = idGarantie; }
    public String getNomGarantie() { return nomGarantie; }
    public void setNomGarantie(String nomGarantie) { this.nomGarantie = nomGarantie; }
    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }

    public String getType() { return type; }
    public void setType(String type) { 
        this.type = normalizeType(type); 
    }

    private String normalizeType(String type) {
        if (type == null) {
            return null;
        }
        return type.trim().toUpperCase();
    }
    public boolean hasValidType() {
        return type != null && !type.trim().isEmpty();
    }
    public double getTauxRemboursement() { return tauxRemboursement; }
    public void setTauxRemboursement(double tauxRemboursement) { this.tauxRemboursement = tauxRemboursement; }

    public TypeMontant getTypeMontant() { return typeMontant; }
    public void setTypeMontant(TypeMontant typeMontant) { this.typeMontant = typeMontant; }

    public TypePlafond getTypePlafond() { return typePlafond; }
    public void setTypePlafond(TypePlafond typePlafond) { this.typePlafond = typePlafond; }

    public double getPlafondAnnuel() { return plafondAnnuel; }
    public void setPlafondAnnuel(double plafondAnnuel) { this.plafondAnnuel = plafondAnnuel; }

    public double getPlafondMensuel() { return plafondMensuel; }
    public void setPlafondMensuel(double plafondMensuel) { this.plafondMensuel = plafondMensuel; }

    public double getPlafondParActe() { return plafondParActe; }
    public void setPlafondParActe(double plafondParActe) { this.plafondParActe = plafondParActe; }

    public double getFranchise() { return franchise; }
    public void setFranchise(double franchise) { this.franchise = franchise; }

    public double getCoutMoyenParSinistre() { return coutMoyenParSinistre; }
    public void setCoutMoyenParSinistre(double coutMoyenParSinistre) { this.coutMoyenParSinistre = coutMoyenParSinistre; }

    public int getDureeMinContrat() { return dureeMinContrat; }
    public void setDureeMinContrat(int dureeMinContrat) { this.dureeMinContrat = dureeMinContrat; }

    public int getDureeMaxContrat() { return dureeMaxContrat; }
    public void setDureeMaxContrat(int dureeMaxContrat) { this.dureeMaxContrat = dureeMaxContrat; }

    public boolean isResiliableAnnuellement() { return resiliableAnnuellement; }
    public void setResiliableAnnuellement(boolean resiliableAnnuellement) { this.resiliableAnnuellement = resiliableAnnuellement; }

    public String getCreePar() { return creePar; }
    public void setCreePar(String creePar) { this.creePar = creePar; }

    public Instant getDateCreation() { return dateCreation; }
    public void setDateCreation(Instant dateCreation) { this.dateCreation = dateCreation; }

    public Instant getDateModification() { return dateModification; }
    public void setDateModification(Instant dateModification) { this.dateModification = dateModification; }

    public Instant getDateDesactivation() { return dateDesactivation; }
    public void setDateDesactivation(Instant dateDesactivation) { this.dateDesactivation = dateDesactivation; }
}