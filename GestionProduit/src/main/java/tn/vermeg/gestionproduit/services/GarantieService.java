package tn.vermeg.gestionproduit.services;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.enums.Statut;
import tn.vermeg.gestionproduit.enums.DomaineMedical;
import tn.vermeg.gestionproduit.exceptions.ResourceNotFoundException;
import tn.vermeg.gestionproduit.repositories.GarantieRepository;
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

    public List<Garantie> getGarantiesByStatut(Statut statut) {
        return garantieRepository.findByStatut(statut);
    }

    public List<Garantie> searchGaranties(String nom) {
        return garantieRepository.findByNomGarantieContainingIgnoreCase(nom);
    }

    public List<Garantie> getGarantiesByTauxRemboursementMin(double tauxMin) {

        if (tauxMin < 0 || tauxMin > 1) {
            throw new IllegalArgumentException("Le taux doit être entre 0 et 1.");
        }

        return garantieRepository.findByTauxRemboursementGreaterThanEqual(tauxMin);
    }

    public List<Garantie> getGarantiesByPlafondMin(double plafondMin) {

        if (plafondMin < 0) {
            throw new IllegalArgumentException("Le plafond ne peut pas être négatif.");
        }

        return garantieRepository.findByPlafondAnnuelGreaterThanEqual(plafondMin);
    }

    public List<Garantie> getGarantiesByDomaine(DomaineMedical domaine) {
        if (domaine == null) {
            throw new IllegalArgumentException("Le domaine ne peut pas être null");
        }
        return garantieRepository.findByDomaine(domaine);
    }

    public List<Garantie> getGarantiesByDomaineAndStatut(DomaineMedical domaine, Statut statut) {
        if (domaine == null || statut == null) {
            throw new IllegalArgumentException("Le domaine et le statut ne peuvent pas être null");
        }
        return garantieRepository.findByDomaineAndStatut(domaine, statut);
    }

    public List<Garantie> getActiveGarantiesByDomaine(DomaineMedical domaine) {
        if (domaine == null) {
            throw new IllegalArgumentException("Le domaine ne peut pas être null");
        }
        return garantieRepository.findByDomaineAndStatutAndDateDesactivationIsNull(domaine, Statut.ACTIF);
    }

    // UPDATE
    public Garantie updateGarantie(String idGarantie, Garantie details) {

        Garantie garantie = getGarantieById(idGarantie);

        validateGarantie(details);

        boolean nomChange = !garantie.getNomGarantie()
                .equalsIgnoreCase(details.getNomGarantie());

        if (nomChange && garantieRepository.existsByNomGarantieIgnoreCase(details.getNomGarantie())) {
            throw new IllegalArgumentException("Une garantie avec ce nom existe déjà.");
        }

        garantie.setNomGarantie(details.getNomGarantie());
        garantie.setDescription(details.getDescription());
        garantie.setDomaine(details.getDomaine());
        garantie.setTauxRemboursement(details.getTauxRemboursement());
        garantie.setTypeMontant(details.getTypeMontant());
        garantie.setTypePlafond(details.getTypePlafond());
        garantie.setPlafondAnnuel(details.getPlafondAnnuel());
        garantie.setPlafondMensuel(details.getPlafondMensuel());
        garantie.setPlafondParActe(details.getPlafondParActe());
        garantie.setFranchise(details.getFranchise());
        garantie.setCoutMoyenParSinistre(details.getCoutMoyenParSinistre());
        garantie.setDureeMinContrat(details.getDureeMinContrat());
        garantie.setDureeMaxContrat(details.getDureeMaxContrat());
        garantie.setResiliableAnnuellement(details.isResiliableAnnuellement());
        garantie.setCreePar(details.getCreePar());

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
        if (garantie.getStatut() == Statut.INACTIF) {
            throw new IllegalStateException("La garantie est déjà inactive.");}

        garantie.setStatut(Statut.INACTIF);
        garantie.setDateDesactivation(Instant.now());
        return garantieRepository.save(garantie);
    }

     private void validateGarantie(@Valid Garantie garantie) {
        if (garantie.getNomGarantie() == null || garantie.getNomGarantie().isBlank()) {
            throw new IllegalArgumentException("Le nom de la garantie est obligatoire.");
        }
        if (!garantie.estValide()) {
            throw new IllegalArgumentException("Paramètres financiers invalides.");
        }
    }

    public Garantie createGarantie(@Valid Garantie garantie) {
        validateGarantie(garantie);

        if (garantieRepository.existsByNomGarantieIgnoreCase(garantie.getNomGarantie())) {
            throw new IllegalArgumentException("Une garantie avec ce nom existe déjà: " + garantie.getNomGarantie());
        }

        garantie.setStatut(Statut.ACTIF);
        garantie.setCreePar("system");
        
        return garantieRepository.save(garantie);
    }
}