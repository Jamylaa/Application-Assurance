package tn.vermeg.gestionproduit.entities.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.vermeg.gestionproduit.enums.TypePlafond;

/**
 * Value Object représentant la structure de plafond d'une garantie.
 * Un plafond de 0 signifie « illimité » pour ce type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlafondGarantie {
    /** Plafond remboursable par acte médical (0 = illimité). */
    private double plafondParActe;
    /** Plafond remboursable par mois (0 = illimité). */
    private double plafondMensuel;
    /** Plafond remboursable sur l'année de contrat (0 = illimité). */
    private double plafondAnnuel;
    /** Plafond global sur la durée totale du contrat (0 = illimité). */
    private double plafondGlobal;
    /** Plafond par soins (regroupement de plusieurs actes pour un même épisode). */
    private double plafondParSoins;
    /** Type de plafond dominant utilisé dans les calculs de remboursement. */
    private TypePlafond typePrincipal;
    /** Description lisible par l'assuré, ex: "3 000 TND/an — 500 TND/acte". */
    private String description;
    /** Devise des montants (TND, EUR…). */
    @Builder.Default
    private String devise = "TND";
    public boolean estIllimite() {
        return plafondParActe == 0 && plafondMensuel == 0 && plafondAnnuel == 0
                && plafondGlobal == 0 && plafondParSoins == 0;
    }
    public double appliquerPlafond(double montantCalcule) {
        if (typePrincipal == null) return montantCalcule;
        return switch (typePrincipal) {
            case PAR_ACTE -> plafondParActe > 0 ? Math.min(montantCalcule, plafondParActe) : montantCalcule;
            case MENSUEL  -> plafondMensuel > 0 ? Math.min(montantCalcule, plafondMensuel) : montantCalcule;
            case ANNUEL   -> plafondAnnuel > 0  ? Math.min(montantCalcule, plafondAnnuel)  : montantCalcule;
            case GLOBAL   -> plafondGlobal > 0  ? Math.min(montantCalcule, plafondGlobal)  : montantCalcule;
            case PAR_SOINS -> plafondParSoins > 0 ? Math.min(montantCalcule, plafondParSoins) : montantCalcule;
        };
    }
}