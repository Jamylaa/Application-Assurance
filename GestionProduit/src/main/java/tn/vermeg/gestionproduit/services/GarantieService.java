package tn.vermeg.gestionproduit.services;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.enums.StatutWorkflow;
import tn.vermeg.gestionproduit.enums.DomaineMedical;
import tn.vermeg.gestionproduit.exceptions.ResourceNotFoundException;
import tn.vermeg.gestionproduit.repositories.GarantieRepository;
import tn.vermeg.gestionproduit.util.ValidationUtils;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;

@Service
@Validated
public class GarantieService {

    private final GarantieRepository garantieRepository;

    public GarantieService(GarantieRepository garantieRepository) {
        this.garantieRepository = garantieRepository;
    }
    public List<Garantie> getAllGaranties() {
        return garantieRepository.findAll();
    }
    public Garantie getGarantieById(String idGarantie) {
        return garantieRepository.findById(idGarantie)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Garantie", idGarantie, "Garantie non trouvée avec l'ID: " + idGarantie));
    }

    public List<Garantie> searchGaranties(String nom) {
        return garantieRepository.findByNomGarantieContainingIgnoreCase(nom);
    }
    public List<Garantie> getGarantiesByTauxRemboursementMin(double tauxMin) {

        if (tauxMin < 0 || tauxMin > 100) {
            throw new IllegalArgumentException("Le taux doit être entre 0 et 100.");
        }
        return garantieRepository.findByTauxRemboursementBaseGreaterThanEqual(tauxMin);
    }
    public List<Garantie> getGarantiesByPlafondMin(double plafondMin) {
        if (plafondMin < 0) {
            throw new IllegalArgumentException("Le plafond ne peut pas être négatif.");
        }
        return garantieRepository.findByPlafond_PlafondAnnuelGreaterThanEqual(plafondMin);
    }
    public List<Garantie> getGarantiesByDomaine(DomaineMedical domaine) {
        if (domaine == null) {
            throw new IllegalArgumentException("Le domaine ne peut pas être null");
        }
        return garantieRepository.findByDomaine(domaine);
    }
    public List<Garantie> getActiveGarantiesByDomaine(DomaineMedical domaine) {
        if (domaine == null) {
            throw new IllegalArgumentException("Le domaine ne peut pas être null");
        }
        return garantieRepository.findByDomaineAndStatutWorkflowAndDateDesactivationIsNull(domaine, StatutWorkflow.PUBLIE);
    }
    // UPDATE
    // Mise à jour partielle : seuls les champs fournis dans "details" sont appliqués sur la
    // garantie existante, les autres champs sont conservés (sémantique de fusion).
    public Garantie updateGarantie(String idGarantie, Garantie details) {
        Garantie garantie = getGarantieById(idGarantie);

        if (details.getNomGarantie() != null && !details.getNomGarantie().isBlank()) {
            boolean nomChange = !garantie.getNomGarantie().equalsIgnoreCase(details.getNomGarantie());
            if (nomChange && garantieRepository.existsByNomGarantieIgnoreCase(details.getNomGarantie())) {
                throw new IllegalArgumentException("Une garantie avec ce nom existe déjà.");
            }
            garantie.setNomGarantie(details.getNomGarantie());
        }
        if (details.getCodeGarantie() != null && !details.getCodeGarantie().isBlank()) {
            garantie.setCodeGarantie(details.getCodeGarantie());
        }
        if (details.getNomCourt() != null) garantie.setNomCourt(details.getNomCourt());
        if (details.getDescription() != null) garantie.setDescription(details.getDescription());
        if (details.getDescriptionTechnique() != null) garantie.setDescriptionTechnique(details.getDescriptionTechnique());
        if (details.getDomaine() != null) garantie.setDomaine(details.getDomaine());
        if (details.getStatutWorkflow() != null) garantie.setStatutWorkflow(details.getStatutWorkflow());
        if (details.getEvenementsCouvertsParDefaut() != null) {
            garantie.setEvenementsCouvertsParDefaut(details.getEvenementsCouvertsParDefaut());
        }
        if (details.getTauxRemboursementBase() > 0) garantie.setTauxRemboursementBase(details.getTauxRemboursementBase());
        if (details.getTauxRemboursementMinimum() > 0) garantie.setTauxRemboursementMinimum(details.getTauxRemboursementMinimum());
        if (details.getTauxRemboursementMaximum() > 0) garantie.setTauxRemboursementMaximum(details.getTauxRemboursementMaximum());
        if (details.getPlafond() != null) garantie.setPlafond(details.getPlafond());
        if (details.getFranchise() != null) garantie.setFranchise(details.getFranchise());
        if (details.getTypeRemboursement() != null) garantie.setTypeRemboursement(details.getTypeRemboursement());
        if (details.getRegleCalcul() != null) garantie.setRegleCalcul(details.getRegleCalcul());
        if (details.getPrerequisGarantieIds() != null) garantie.setPrerequisGarantieIds(details.getPrerequisGarantieIds());
        if (details.getPrimePureBase() > 0) garantie.setPrimePureBase(details.getPrimePureBase());
        if (details.getModifiePar() != null) garantie.setModifiePar(details.getModifiePar());

        validateGarantie(garantie);
        return garantieRepository.save(garantie);
    }
    // DELETE / DESACTIVATION
    public void deleteGarantie(String idGarantie) {
        if (!garantieRepository.existsById(idGarantie)) {
            throw new IllegalArgumentException("Garantie non trouvée avec l'ID: " + idGarantie);}
        garantieRepository.deleteById(idGarantie);
    }
    public Garantie desactiverGarantie(String idGarantie) {
        Garantie garantie = getGarantieById(idGarantie);
        if (garantie.getDateDesactivation() != null) {
            throw new IllegalStateException("La garantie est déjà inactive.");}
        garantie.setDateDesactivation(Instant.now());
        return garantieRepository.save(garantie);
    }
     private void validateGarantie(@Valid Garantie garantie) {
        ValidationUtils.requireNonBlank(garantie.getNomGarantie(), "Le nom de la garantie est obligatoire.");
        if (!garantie.estValide()) {
            throw new IllegalArgumentException("Paramètres financiers invalides.");
        }
    }
    public Garantie createGarantie(@Valid Garantie garantie) {
        validateGarantie(garantie);
        if (garantieRepository.existsByNomGarantieIgnoreCase(garantie.getNomGarantie())) {
            throw new IllegalArgumentException("Une garantie avec ce nom existe déjà: " + garantie.getNomGarantie());
        }
        garantie.setCreePar("system");
        return garantieRepository.save(garantie);
    }
}
