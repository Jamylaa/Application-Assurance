package tn.vermeg.gestionproduit.services;

import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.enums.StatutWorkflow;
import tn.vermeg.gestionproduit.exceptions.ResourceNotFoundException;
import tn.vermeg.gestionproduit.repositories.GarantieRepository;

/**
 * Centralise les règles de validation appliquées avant toute association
 * pack-garantie : la garantie doit exister et être PUBLIE.
 */
@Service
public class AssociationValidationService {

    private final GarantieRepository garantieRepository;

    public AssociationValidationService(GarantieRepository garantieRepository) {
        this.garantieRepository = garantieRepository;
    }
    /**
     * Vérifie que la garantie existe et qu'elle est PUBLIE.
     * @throws ResourceNotFoundException si la garantie n'existe pas
     * @throws IllegalStateException si la garantie n'est pas PUBLIE
     */
    public Garantie validateGarantieExisteEtActive(String garantieId) {
        Garantie garantie = garantieRepository.findById(garantieId)
                .orElseThrow(() -> new ResourceNotFoundException("Garantie", garantieId,
                        "Impossible d'associer : la garantie " + garantieId + " n'existe pas."));

        if (garantie.getStatutWorkflow() != StatutWorkflow.PUBLIE) {
            throw new IllegalStateException(
                    "Impossible d'associer la garantie '" + garantie.getNomGarantie()
                            + "' : statut actuel = " + garantie.getStatutWorkflow() + " (PUBLIE requis).");
        }

        return garantie;
    }
}