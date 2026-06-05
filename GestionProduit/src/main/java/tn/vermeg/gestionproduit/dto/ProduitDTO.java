package tn.vermeg.gestionproduit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.vermeg.gestionproduit.entities.Statut;
import tn.vermeg.gestionproduit.entities.TypeProduit;

import java.time.Instant;
import java.util.Map;

public class ProduitDTO {

    private String idProduit;

    @NotBlank(message = "Le nom du produit est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String nomProduit;

    @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
    private String description;

    @NotNull(message = "Le type du produit est obligatoire")
    private TypeProduit typeProduit;

    private Statut statut;

    private Instant dateCreation;

    private Instant dateModification;

    private Map<String, Object> customFields;

    // CONSTRUCTEURS
    public ProduitDTO() {}

    public ProduitDTO(String idProduit, String nomProduit, String description,
                      TypeProduit typeProduit, Statut statut) {
        this.idProduit = idProduit;
        this.nomProduit = nomProduit;
        this.description = description;
        this.typeProduit = typeProduit;
        this.statut = statut;
    }

    // GETTERS & SETTERS
    public String getIdProduit() {
        return idProduit;
    }

    public void setIdProduit(String idProduit) {
        this.idProduit = idProduit;
    }

    public String getNomProduit() {
        return nomProduit;
    }

    public void setNomProduit(String nomProduit) {
        this.nomProduit = nomProduit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TypeProduit getTypeProduit() {
        return typeProduit;
    }

    public void setTypeProduit(TypeProduit typeProduit) {
        this.typeProduit = typeProduit;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
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

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }
}
