package tn.vermeg.gestionproduit.entities.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Value Object encapsulant la règle de calcul du remboursement d'une garantie.
 * Permet de définir une formule paramétrable sans hard-coding des algorithmes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegleCalcul {

    /**
     * Formule textuelle de calcul, ex:
     * "montant * tauxRemboursement / 100 - franchise"
     * "Math.min(montant * taux / 100, plafondParActe) - franchiseFixe"
     */
    private String formule;

    /** Explication humaine de la règle pour le juriste / l'assureur. */
    private String descriptionFormule;

    /** Noms des variables utilisées dans la formule (pour documentation). */
    private List<String> parametresFormule;

    /**
     * Valeurs par défaut des paramètres si non surchargés au niveau du pack.
     * Exemple : {"tauxRemboursement": 80.0, "franchiseFixe": 50.0}
     */
    private Map<String, Double> valeursDefaut;

    /** Ordre d'application lors du calcul cumulé (plusieurs garanties / même acte). */
    @Builder.Default
    private int prioriteCalcul = 1;

    /** Si true, le plafond est appliqué après calcul du taux. */
    @Builder.Default
    private boolean appliquerPlafondApresCalcul = true;

    /** Si true, la franchise est déduite avant l'application du plafond. */
    @Builder.Default
    private boolean deduireFranchiseAvantPlafond = false;

    /** Si true, la base de calcul est le tarif conventionné, pas le montant réel. */
    @Builder.Default
    private boolean baseConventionnee = false;

    /**
     * Calcul simplifié utilisé comme fallback applicatif.
     * (La vraie logique métier est dans le moteur de calcul.)
     */
    public double calculerRemboursement(double montant, double taux, double franchiseApplicable, double plafondApplicable) {
        double base = montant * taux / 100.0;
        if (deduireFranchiseAvantPlafond) {
            base = Math.max(0, base - franchiseApplicable);
            if (appliquerPlafondApresCalcul && plafondApplicable > 0) base = Math.min(base, plafondApplicable);
        } else {
            if (appliquerPlafondApresCalcul && plafondApplicable > 0) base = Math.min(base, plafondApplicable);
            base = Math.max(0, base - franchiseApplicable);
        }
        return base;
    }
}
