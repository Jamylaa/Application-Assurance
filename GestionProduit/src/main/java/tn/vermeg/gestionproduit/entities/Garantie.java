package tn.vermeg.gestionproduit.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.vermeg.gestionproduit.entities.embedded.FranchiseGarantie;
import tn.vermeg.gestionproduit.entities.embedded.PlafondGarantie;
import tn.vermeg.gestionproduit.entities.embedded.RegleCalcul;
import tn.vermeg.gestionproduit.enums.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "garanties")
public class Garantie {

    @Id
    private String idGarantie;

    // ─── IDENTITÉ TECHNIQUE ───────────────────────────────────────────────────

    /** Code métier unique (ex: "GAR-HOSP-001", "GAR-OPT-002"). */
    @NotBlank(message = "Le code garantie est obligatoire")
    @Indexed(unique = true)
    private String codeGarantie;

    /** Nom complet de la garantie. */
    @NotBlank(message = "Le nom de la garantie est obligatoire")
    private String nomGarantie;

    /**
     * Nom court pour les tableaux et comparatifs (max 30 caractères).
     * Ex: "Hospitalisation", "Optique", "Dentaire"
     */
    private String nomCourt;

    /** Description complète pour l'assuré. */
    @NotBlank(message = "La description est obligatoire")
    private String description;

    /** Description technique destinée aux actuaires et juristes. */
    private String descriptionTechnique;

    // ─── CLASSIFICATION ───────────────────────────────────────────────────────

    @NotNull(message = "Le domaine médical est obligatoire")
    private DomaineMedical domaine;

    /**
     * Indique si la garantie est par nature obligatoire dans tous les packs qui l'incluent
     * (peut être surchargé par PackGarantie.optionnelle).
     */
    @Builder.Default
    private boolean garantieObligatoireParDefaut = false;

    // ─── WORKFLOW ─────────────────────────────────────────────────────────────

    @Builder.Default
    private StatutWorkflow statutWorkflow = StatutWorkflow.BROUILLON;

    // ─── ÉVÉNEMENTS COUVERTS ──────────────────────────────────────────────────

    /**
     * Liste des actes, soins ou événements pris en charge par cette garantie.
     * Sert de base documentaire et au moteur de recommandation du chatbot.
     * Ex: ["Consultation généraliste", "Consultation spécialiste", "Urgences 24h/24"]
     */
    private List<String> evenementsCouvertsParDefaut;

    // ─── PLAFONDS (Value Object) ──────────────────────────────────────────────

    /**
     * Structure de plafonds remplaçant les 3 primitifs plats de la v1
     * (plafondAnnuel, plafondMensuel, plafondParActe).
     * Inclut plafondGlobal et plafondParSoins, plus la méthode appliquerPlafond().
     */
    private PlafondGarantie plafond;

    // ─── FRANCHISE (Value Object) ─────────────────────────────────────────────

    /**
     * Structure de franchise remplaçant le double plat de la v1.
     * Supporte les types FIXE, POURCENTAGE, RELATIVE, ABSOLUE.
     */
    private FranchiseGarantie franchise;

    // ─── REMBOURSEMENT ────────────────────────────────────────────────────────

    /** Mode de calcul du remboursement. */
    @Builder.Default
    private TypeRemboursement typeRemboursement = TypeRemboursement.FRAIS_REELS;

    /**
     * Taux de remboursement de base en % (0–100).
     * Ex: 80 = remboursement à 80% des frais réels.
     */
    @Min(0) @Max(100)
    @Builder.Default
    private double tauxRemboursementBase = 80.0;

    /** Taux minimal applicable (plancher, si le taux peut être modulé). */
    @Min(0) @Max(100)
    @Builder.Default
    private double tauxRemboursementMinimum = 0.0;

    /** Taux maximal applicable (plafond, pour les cas exceptionnels). */
    @Min(0) @Max(100)
    @Builder.Default
    private double tauxRemboursementMaximum = 100.0;

    // ─── RÈGLE DE CALCUL (Value Object) ──────────────────────────────────────

    /**
     * Règle paramétrée de calcul du remboursement.
     * Permet de définir une formule sans modifier le code du moteur de calcul.
     */
    private RegleCalcul regleCalcul;

    // ─── DÉPENDANCES ENTRE GARANTIES ──────────────────────────────────────────

    /**
     * IDs des garanties prérequises pour activer celle-ci.
     * Ex: une garantie "Chambre particulière" peut nécessiter "Hospitalisation de base".
     */
    private List<String> prerequisGarantieIds;

    // ─── PARAMÈTRES DYNAMIQUES ────────────────────────────────────────────────

    /**
     * Paramètres variables utilisés dans la formule de calcul.
     * Permet d'adapter la garantie sans recompiler le code.
     * Ex: {"tauxSpecialiste": 90.0, "plafondUrgences": 500.0}
     */
    private Map<String, Object> parametresDynamiques;

    // ─── DONNÉES ACTUARIELLES ─────────────────────────────────────────────────

    /**
     * Prime pure de base — contribution de cette garantie à la prime totale (TND/mois).
     * Calculée par les actuaires à partir des statistiques de sinistralité.
     */
    @Builder.Default
    private double primePureBase = 0.0;

    // ─── AUDIT ────────────────────────────────────────────────────────────────

    private String creePar;
    private String modifiePar;

    @CreatedDate
    private Instant dateCreation;

    @LastModifiedDate
    private Instant dateModification;

    private Instant dateDesactivation;

    // ─── MÉTHODES MÉTIER ──────────────────────────────────────────────────────

    public boolean estValide() {
        return codeGarantie != null && !codeGarantie.isBlank()
                && nomGarantie != null && !nomGarantie.isBlank()
                && domaine != null
                && tauxRemboursementBase >= 0
                && tauxRemboursementBase <= 100;
    }

    public boolean estActive() {
        return StatutWorkflow.PUBLIE.equals(statutWorkflow) && dateDesactivation == null;
    }

    /**
     * Calcule le remboursement en appliquant taux → plafond → franchise
     * selon la règle de calcul configurée.
     *
     * @param montantSinistre montant brut du sinistre (TND)
     * @return montant remboursé net (TND)
     */
    public double calculerRemboursement(double montantSinistre) {
        if (!estActive() || montantSinistre <= 0) return 0.0;

        double franchiseApplicable = (franchise != null)
                ? franchise.calculerFranchise(montantSinistre) : 0.0;
        double plafondApplicable = (plafond != null && plafond.getTypePrincipal() != null)
                ? plafond.appliquerPlafond(montantSinistre * tauxRemboursementBase / 100.0)
                : montantSinistre * tauxRemboursementBase / 100.0;

        if (regleCalcul != null) {
            return regleCalcul.calculerRemboursement(
                    montantSinistre, tauxRemboursementBase, franchiseApplicable, plafondApplicable);
        }

        // Calcul par défaut : taux → plafond → franchise
        double base = montantSinistre * tauxRemboursementBase / 100.0;
        if (plafond != null) base = plafond.appliquerPlafond(base);
        return Math.max(0, base - franchiseApplicable);
    }
}
