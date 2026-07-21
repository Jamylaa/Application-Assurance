package tn.vermeg.gestionproduit.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.vermeg.gestionproduit.enums.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "produits")
public class Produit {
    @Id
    private String idProduit;
    // ─── IDENTITÉ TECHNIQUE ───────────────────────────────────────────────────
    /**
     * Code métier unique du produit (ex: "SANTE-COMP-001").
     * Utilisé pour référencer le produit dans les systèmes tiers (contrats, CRM).
     */
    @NotBlank(message = "Le code produit est obligatoire")
    @Indexed(unique = true)
    private String codeProduit;
    /** Nom technique interne, utilisé dans les systèmes de gestion. */
    @NotBlank(message = "Le nom du produit est obligatoire")
    private String nomProduit;
    /** Nom commercial affiché aux clients et agents (peut différer du nom technique). */
    private String nomCommercial;
    /** Description détaillée pour la fiche produit (admin et juriste). */
    @NotBlank(message = "La description est obligatoire")
    private String description;
    // ─── CLASSIFICATION ───────────────────────────────────────────────────────
    @NotNull(message = "Le type de produit est obligatoire")
    private TypeProduit typeProduit;
    // ─── WORKFLOW ─────────────────────────────────────────────────────────────
    @Builder.Default
    private StatutWorkflow statutWorkflow = StatutWorkflow.BROUILLON;
    /** Identifiant de l'utilisateur ayant validé le produit. */
    private String validePar;
    private Instant dateValidation;
    // ─── PARAMÈTRES FINANCIERS ────────────────────────────────────────────────
    /**
     * Prime de référence (base tarifaire).
     * Les packs précisent leur propre tarification ; ce champ sert d'estimation
     * pour les simulateurs et comparateurs.
     */
    @Positive(message = "Le prix de base doit être positif")
    @Builder.Default
    private double prixBase = 0.0;
    @Builder.Default
    private String devisePrix = "TND";
    // ─── COUVERTURE GÉOGRAPHIQUE ──────────────────────────────────────────────
    @Builder.Default
    private CouvertureGeographique couvertureGeographique = CouvertureGeographique.NATIONAL;
    /** Territoires ou zones exclus de la couverture. */
    private List<String> territoiresExclus;
    // ─── VERSIONING ───────────────────────────────────────────────────────────
    /** Numéro de version du produit (ex: "1.0", "2.1"). */
    @Builder.Default
    private String version = "1.0";
    /** ID de la version précédente (pour la traçabilité réglementaire). */
    private String versionPrecedenteId;
    /** Date d'entrée en vigueur de cette version. */
    private Instant dateEffet;
    /** Date de fin de commercialisation (null = produit toujours commercialisé). */
    private Instant dateExpiration;
    // ─── RELATIONS (CHARGÉES DYNAMIQUEMENT) ──────────────────────────────────
    @Transient
    private List<Pack> packs;

    // ─── AUDIT ────────────────────────────────────────────────────────────────
    private String creePar;
    private String modifiePar;
    @CreatedDate
    private Instant dateCreation;
    @LastModifiedDate
    private Instant dateModification;

    // ─── MÉTHODES MÉTIER ──────────────────────────────────────────────────────

    public boolean estActif() {
        return StatutWorkflow.PUBLIE.equals(statutWorkflow);
    }

    public boolean estCommercialisable() {
        return estActif()
                && (dateExpiration == null || Instant.now().isBefore(dateExpiration));}

    public boolean estValide() {
        return codeProduit != null && !codeProduit.isBlank()
                && nomProduit != null && !nomProduit.isBlank()
                && typeProduit != null;
    }
}
