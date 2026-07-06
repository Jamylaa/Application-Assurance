package tn.vermeg.gestionproduit.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.enums.StatutWorkflow;
import tn.vermeg.gestionproduit.enums.DomaineMedical;

import java.util.List;
import java.util.Optional;

@Repository
public interface GarantieRepository extends MongoRepository<Garantie, String> {

    boolean existsByNomGarantieIgnoreCase(String nomGarantie);
    Optional<Garantie> findFirstByNomGarantieIgnoreCase(String nomGarantie);
    List<Garantie> findByNomGarantieContainingIgnoreCase(String nomGarantie);
    List<Garantie> findByTauxRemboursementBaseGreaterThanEqual(double tauxMin);
    List<Garantie> findByPlafond_PlafondAnnuelGreaterThanEqual(double plafondMin);
    List<Garantie> findByDomaine(DomaineMedical domaine);
    List<Garantie> findByDomaineAndStatutWorkflowAndDateDesactivationIsNull(DomaineMedical domaine, StatutWorkflow statutWorkflow);
    long countByDomaine(DomaineMedical domaine);
 boolean existsByDomaine(DomaineMedical domaine);
}
