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
@Document(collection = "packs")
public class Pack {

    @Id
    private String idPack;

    // ─── IDENTITÉ TECHNIQUE ───────────────────────────────────────────────────
    /** Code métier unique (ex: "SANTE-COMP-BASIC-001"). */
    @NotBlank(message = "Le code pack est obligatoire")
    @Indexed(unique = true)
    private String codePack;

    /** Nom interne technique. */
    @NotBlank(message = "Le nom du pack est obligatoire")
    private String nomPack;

    /** Nom affiché aux clients (ex: "Formule Essentielle", "Formule Premium"). */
    private String nomCommercial;

    /** Description détaillée pour la fiche produit. */
    @NotBlank(message = "La description est obligatoire")
    private String description;

    /** Résumé court (max 280 caractères) pour les listes. */
    private String descriptionCourte;

    // ─── RELATION PRODUIT ─────────────────────────────────────────────────────
    @NotBlank(message = "L'identifiant du produit parent est obligatoire")
    @Indexed
    private String produitId;

    // ─── CLASSIFICATION ───────────────────────────────────────────────────────

    @NotNull(message = "Le niveau de couverture est obligatoire")
    private NiveauCouverture niveauCouverture;

    // ─── WORKFLOW ─────────────────────────────────────────────────────────────
    @Builder.Default
    private StatutWorkflow statutWorkflow = StatutWorkflow.BROUILLON;

    // ─── MISE EN AVANT COMMERCIALE ────────────────────────────────────────────

    /** Badge "Recommandé" — formule la plus adaptée selon le profil moyen. */
    @Builder.Default
    private boolean packRecommande = false;

    /** Couleur thème du badge (hex), ex: "#0d7377" pour teal. */
    @Builder.Default
    private String colorTheme = "#0f4c81";

    // ─── TARIFICATION ─────────────────────────────────────────────────────────

    /** Prime mensuelle (TND). */
    @Positive(message = "Le prix mensuel doit être positif")
    private double prixMensuel;

    /**
     * Prime annuelle (TND).
     * Peut être différente de prixMensuel × 12 si une remise annuelle est appliquée.
     */
    private double prixAnnuel;

    /** Taux de remise pour paiement annuel (ex: 5.0 pour 5%). */
    @Builder.Default
    private double tauxRemiseAnnuelle = 0.0;

    @Builder.Default
    private String devisePrix = "TND";

    /** Numéro de version commerciale du pack (ex: "1.0", "1.1"). */
    @Builder.Default
    private String versionPack = "1.0";

    // ─── OPTIONS ──────────────────────────────────────────────────────────────
    /** Ce pack propose-t-il des garanties optionnelles ajoutables ? */
    @Builder.Default
    private boolean optionsDisponibles = false;

    /**
     * IDs des packs représentant des "extensions optionnelles" de cette formule.
     * Permet d'associer des modules complémentaires (ex: "Module Optique Premium").
     */
    private List<String> optionsPackIds;
    // ─── COMPATIBILITÉ MULTI-CONTRATS ─────────────────────────────────────────

    /**
     * IDs des packs d'autres produits pouvant être combinés avec celui-ci
     * dans un contrat multi-risques.
     */
    private List<String> packsCompatibles;

    /**
     * IDs des packs exclusifs — souscrire l'un exclut l'autre
     * (ex: deux niveaux du même produit).
     */
    private List<String> packsIncompatibles;

    // ─── VALIDITÉ TEMPORELLE ──────────────────────────────────────────────────

    /** Date d'entrée en commercialisation. */
    private Instant dateEffet;
    /** Date de fin de commercialisation (null = toujours actif). */
    private Instant dateExpiration;

    // ─── RELATIONS (CHARGÉES DYNAMIQUEMENT) ──────────────────────────────────

    @Transient
    private List<PackGarantie> garanties;

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
                && (dateExpiration == null || Instant.now().isBefore(dateExpiration));
    }

    /**
     * Calcule le prix annuel avec remise si non défini.
     */
    public double getPrixAnnuelEffectif() {
        if (prixAnnuel > 0) return prixAnnuel;
        return prixMensuel * 12 * (1 - tauxRemiseAnnuelle / 100.0);
    }

    public boolean estValide() {
        return codePack != null && !codePack.isBlank()
                && nomPack != null && !nomPack.isBlank()
                && produitId != null && !produitId.isBlank()
                && niveauCouverture != null
                && prixMensuel > 0;
    }
}
