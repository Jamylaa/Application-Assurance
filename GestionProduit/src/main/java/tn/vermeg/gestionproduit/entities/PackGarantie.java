package tn.vermeg.gestionproduit.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.vermeg.gestionproduit.entities.embedded.FranchiseGarantie;
import tn.vermeg.gestionproduit.entities.embedded.PlafondGarantie;
import tn.vermeg.gestionproduit.enums.TypeMontant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pack_garanties")
@CompoundIndexes({
    @CompoundIndex(name = "idx_pack_garantie_unique", def = "{'packId': 1, 'garantieId': 1}", unique = true)
})
public class PackGarantie {

    @Id
    private String idPackGarantie;

    // ─── CLÉS ÉTRANGÈRES ──────────────────────────────────────────────────────
    @NotBlank(message = "L'identifiant du pack est obligatoire")
    private String packId;

    @NotBlank(message = "L'identifiant de la garantie est obligatoire")
    private String garantieId;

    // ─── DÉNORMALISATION (pour les lectures sans jointure) ────────────────────

    /** Nom de la garantie (copié depuis Garantie.nomGarantie). */
    private String nomGarantie;

    /** Code de la garantie (copié depuis Garantie.codeGarantie). */
    private String codeGarantie;

    // ─── CONFIGURATION SPÉCIFIQUE AU PACK ────────────────────────────────────

    /**
     * Taux de remboursement spécifique pour ce pack (0–100%).
     * Surcharge le taux de base défini dans la Garantie.
     * Ex: 100.0 pour un pack premium, 60.0 pour un pack basic.
     * Si null → utiliser Garantie.tauxRemboursementBase.
     */
    @Min(0) @Max(100)
    private Double tauxRemboursementSpecifique;

    /**
     * Plafonds spécifiques pour ce pack.
     * Surcharge le PlafondGarantie défini dans la Garantie.
     * Si null → utiliser Garantie.plafond.
     */
    private PlafondGarantie plafondSpecifique;

    /**
     * Franchise spécifique pour ce pack.
     * Surcharge la FranchiseGarantie définie dans la Garantie.
     * Si null → utiliser Garantie.franchise.
     */
    private FranchiseGarantie franchiseSpecifique;

    /** Type de montant de remboursement applicable dans ce pack. */
    private TypeMontant typeMontant;

    // ─── OPTIONS ──────────────────────────────────────────────────────────────

    /**
     * Si true, la garantie est optionnelle dans ce pack — le souscripteur
     * peut choisir de l'inclure ou non (avec supplementPrix si incluse).
     * Si false, la garantie est systématiquement incluse.
     */
    @Builder.Default
    private boolean optionnelle = false;

    /**
     * Surcoût mensuel (TND) si la garantie est optionnelle et souscrite.
     * Ignoré si optionnelle = false.
     */
    @Builder.Default
    private double supplementPrix = 0.0;

    @Builder.Default
    private boolean actif = true;

    // ─── AUDIT ────────────────────────────────────────────────────────────────

    private String configurePar;

    @CreatedDate
    private Instant dateActivation;
    private Instant dateDesactivation;
    @LastModifiedDate
    private Instant dateModification;

    // ─── MÉTHODES MÉTIER ──────────────────────────────────────────────────────

    public boolean estValide() {
        return packId != null && !packId.isBlank()
                && garantieId != null && !garantieId.isBlank()
                && (tauxRemboursementSpecifique == null
                        || (tauxRemboursementSpecifique >= 0 && tauxRemboursementSpecifique <= 100));
    }

    public boolean estActif() {
        return actif && dateDesactivation == null;
    }

    /**
     * Calcule le remboursement en utilisant les paramètres spécifiques au pack
     * (si définis), sinon les paramètres de base passés en argument.
     *
     * @param montantSinistre  montant brut du sinistre
     * @param tauxBase         taux de base de la garantie (fallback si tauxSpecifique null)
     * @param plafondParActeBase plafond par acte de la garantie (fallback)
     * @param franchiseBase    montant de franchise de la garantie (fallback)
     * @return montant remboursé net
     */
    public double calculerRemboursement(double montantSinistre,
                                        double tauxBase,
                                        double plafondParActeBase,
                                        double franchiseBase) {
        double taux = (tauxRemboursementSpecifique != null) ? tauxRemboursementSpecifique : tauxBase;

        double franchiseApplicable = (franchiseSpecifique != null)
                ? franchiseSpecifique.calculerFranchise(montantSinistre) : franchiseBase;

        double base = montantSinistre * taux / 100.0;

        double plafondActe = (plafondSpecifique != null && plafondSpecifique.getPlafondParActe() > 0)
                ? plafondSpecifique.getPlafondParActe() : plafondParActeBase;
        if (plafondActe > 0) base = Math.min(base, plafondActe);

        return Math.max(0, base - franchiseApplicable);
    }
}
