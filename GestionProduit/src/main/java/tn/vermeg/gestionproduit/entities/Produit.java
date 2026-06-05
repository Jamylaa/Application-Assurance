package tn.vermeg.gestionproduit.entities;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "produits")
public class Produit {

    @Id
    private String idProduit;

    private String nomProduit;
    private String description;

    private TypeProduit typeProduit;
    private Statut statut;

    @CreatedDate
    private Instant dateCreation;

    private Map<String, Object> customFields;
    @LastModifiedDate
    private Instant dateModification;

    // CONSTRUCTEURS


    public Produit(String idProduit, String nomProduit, String description, TypeProduit typeProduit, Statut statut, Instant dateCreation, Map<String, Object> customFields, Instant dateModification) {
        this.idProduit = idProduit;
        this.nomProduit = nomProduit;
        this.description = description;
        this.typeProduit = typeProduit;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.customFields = customFields;
        this.dateModification = dateModification;
    }

    public Produit() {}

    // MÉTHODES MÉTIER
    public boolean estActif() {
        return statut == Statut.ACTIF;
    }

    public boolean estValide() {
        return nomProduit != null && !nomProduit.isBlank()
                && typeProduit != null
                && statut != null;
    }
    // GETTERS & SETTERS
    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }
    public String getIdProduit() { return idProduit; }
    public void setIdProduit(String idProduit) { this.idProduit = idProduit; }

    public String getNomProduit() { return nomProduit; }
    public void setNomProduit(String nomProduit) { this.nomProduit = nomProduit; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TypeProduit getTypeProduit() { return typeProduit; }
    public void setTypeProduit(TypeProduit typeProduit) { this.typeProduit = typeProduit; }

    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }

    public Instant getDateCreation() { return dateCreation; }
    public void setDateCreation(Instant dateCreation) { this.dateCreation = dateCreation; }

    public Instant getDateModification() { return dateModification; }
    public void setDateModification(Instant dateModification) { this.dateModification = dateModification; }
}