package tn.vermeg.gestionproduit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.entities.PackGarantie;

/**
 * Garantie à associer à un pack lors de la création d'un produit complet.
 * Si {@code garantieIdExistante} est renseigné, la garantie existante (ACTIF) est réutilisée ;
 * sinon {@code garantie} est créée à la volée — à condition que {@code creationConfirmee} soit
 * explicitement à true, pour qu'aucune garantie ne puisse être créée silencieusement sans que
 * l'appelant (formulaire ou chatbot) ait effectivement présenté ce choix à l'utilisateur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GarantieAssociationRequest {
    private Garantie garantie;
    private String garantieIdExistante;
    private PackGarantie configuration;
    private boolean creationConfirmee;
}
