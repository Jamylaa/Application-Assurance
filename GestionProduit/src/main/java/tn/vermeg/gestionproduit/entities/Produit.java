package tn.vermeg.gestionproduit.entities;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.vermeg.gestionproduit.enums.Statut;
import tn.vermeg.gestionproduit.enums.TypeProduit;

import java.time.Instant;
import java.util.List;

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
    @LastModifiedDate
    private Instant dateModification;

    // Liste des packs associés (non persistée en MongoDB, construite dynamiquement)
    private transient List<Pack> packs;

    // CONSTRUCTEURS

    public Produit() {}
    public Produit(String idProduit, String nomProduit, String description,
                   TypeProduit typeProduit, Statut statut,
                   Instant dateCreation, Instant dateModification) {
        this.idProduit = idProduit;
        this.nomProduit = nomProduit;
        this.description = description;
        this.typeProduit = typeProduit;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.dateModification = dateModification;
    }

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

    public List<Pack> getPacks() { return packs; }
    public void setPacks(List<Pack> packs) { this.packs = packs; }
}