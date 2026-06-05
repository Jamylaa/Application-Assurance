package tn.vermeg.gestionproduit.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.entities.Statut;

import java.util.List;
import java.util.Optional;
@Repository
public interface GarantieRepository extends MongoRepository<Garantie, String> {
    List<Garantie> findByType(String type);
    
    //Recherche par type avec insensibilité à la casse
    List<Garantie> findByTypeIgnoreCase(String type);

    List<Garantie> findByStatut(Statut statut);
    boolean existsByNomGarantieIgnoreCase(String nomGarantie);

    Optional<Garantie> findFirstByNomGarantieIgnoreCase(String nomGarantie);

    List<Garantie> findByNomGarantieContainingIgnoreCase(String nomGarantie);
    List<Garantie> findByTauxRemboursementGreaterThanEqual(double tauxMin);
    List<Garantie> findByPlafondAnnuelGreaterThanEqual(double plafondMin);
    
    // Recherche des garanties actives par type
    List<Garantie> findByTypeAndStatut(String type, Statut statut);
    
    // Recherche textuelle sur le nom et le type
    List<Garantie> findByNomGarantieContainingIgnoreCaseOrTypeContainingIgnoreCase(String nom, String type);
    
    // Compte les garanties par type
    long countByType(String type);
    
    // Vérifie l'existence par type
    boolean existsByType(String type);
}