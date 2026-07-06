package tn.vermeg.gestionproduit.entities.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.vermeg.gestionproduit.enums.TypeFranchise;

/**
 * Value Object représentant la structure de franchise d'une garantie.
 * La franchise est la part restant à la charge de l'assuré.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FranchiseGarantie {

    @Builder.Default
    private TypeFranchise type = TypeFranchise.AUCUNE;

    /** Montant fixe de franchise (TND), utilisé si type = FIXE ou ABSOLUE. */
    @Builder.Default
    private double montantFixe = 0.0;

    /** Pourcentage de franchise (0–100), utilisé si type = POURCENTAGE. */
    @Builder.Default
    private double pourcentage = 0.0;

    /** Montant minimum de franchise si calculée en %. */
    @Builder.Default
    private double montantMinimum = 0.0;

    /** Montant maximum de franchise si calculée en % (0 = sans plafond). */
    @Builder.Default
    private double montantMaximum = 0.0;

    /** Description lisible, ex: "10% du montant sinistre, min 50 TND". */
    private String description;

    /** Devise (TND, EUR…). */
    @Builder.Default
    private String devise = "TND";

    /**
     * Calcule la franchise applicable sur un montant de sinistre donné.
     *
     * @param montantSinistre montant total du sinistre
     * @return montant de franchise à déduire
     */
    public double calculerFranchise(double montantSinistre) {
        return switch (type) {
            case AUCUNE -> 0.0;
            case FIXE, ABSOLUE -> montantFixe;
            case POURCENTAGE -> {
                double franchiseCalculee = montantSinistre * pourcentage / 100.0;
                if (montantMinimum > 0) franchiseCalculee = Math.max(franchiseCalculee, montantMinimum);
                if (montantMaximum > 0) franchiseCalculee = Math.min(franchiseCalculee, montantMaximum);
                yield franchiseCalculee;
            }
            case RELATIVE ->
                // Franchise relative : s'annule si sinistre > seuil (montantMaximum = seuil)
                (montantMaximum > 0 && montantSinistre >= montantMaximum) ? 0.0 : montantFixe;
        };
    }
}
