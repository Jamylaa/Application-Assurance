package tn.vermeg.gestionproduit.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.enums.Statut;
import tn.vermeg.gestionproduit.enums.DomaineMedical;

import java.util.List;
import java.util.Optional;

@Repository
public interface GarantieRepository extends MongoRepository<Garantie, String> {

    List<Garantie> findByStatut(Statut statut);
    boolean existsByNomGarantieIgnoreCase(String nomGarantie);
    Optional<Garantie> findFirstByNomGarantieIgnoreCase(String nomGarantie);
    List<Garantie> findByNomGarantieContainingIgnoreCase(String nomGarantie);
    List<Garantie> findByTauxRemboursementGreaterThanEqual(double tauxMin);
    List<Garantie> findByPlafondAnnuelGreaterThanEqual(double plafondMin);
    List<Garantie> findByDomaineAndStatut(DomaineMedical domaine, Statut statut);
    List<Garantie> findByDomaine(DomaineMedical domaine);
    List<Garantie> findByDomaineAndStatutAndDateDesactivationIsNull(DomaineMedical domaine, Statut statut);
    long countByDomaine(DomaineMedical domaine);
 boolean existsByDomaine(DomaineMedical domaine);
}