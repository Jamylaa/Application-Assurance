package tn.vermeg.gestionproduit.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.vermeg.gestionproduit.entities.Pack;

import java.util.List;
import java.util.Optional;

@Repository
public interface PackUnifiedRepository extends MongoRepository<Pack, String> {

    // Vérifie l'existence d'un pack par son nom (insensible à la casse)
    boolean existsByNomPackIgnoreCase(String nomPack);

    Optional<Pack> findFirstByNomPackIgnoreCase(String nomPack);

    // Recherche des packs par nom (insensible à la casse)
    List<Pack> findByNomPackIgnoreCaseContaining(String nomPack);

  // Recherche des packs par statut
    List<Pack> findByStatut(tn.vermeg.gestionproduit.entities.Statut statut);
// Recherche des packs par niveau de couverture
    List<Pack> findByNiveauCouverture(tn.vermeg.gestionproduit.entities.NiveauCouverture niveauCouverture);
// Recherche des packs par type de client
    List<Pack> findByTypeClientsContaining(tn.vermeg.gestionproduit.entities.TypeClient typeClient);
//Recherche des packs par produit associé
    List<Pack> findByProduitId(String produitId);
//Recherche des packs par plage de prix
    List<Pack> findByPrixMensuelBetween(double prixMin, double prixMax);
// Recherche des packs par plage d'âge
    List<Pack> findByAgeMinimumLessThanEqualAndAgeMaximumGreaterThanEqual(int ageMax, int ageMin);
// Compte les packs par statut
    long countByStatut(tn.vermeg.gestionproduit.entities.Statut statut);
// Recherche des packs actifs avec prix mensuel inférieur à une valeur
    List<Pack> findByStatutAndPrixMensuelLessThanOrderByPrixMensuelAsc(
        tn.vermeg.gestionproduit.entities.Statut statut, double prixMax);
// Recherche des packs par couverture géographique
    List<Pack> findByCouvertureGeographique(tn.vermeg.gestionproduit.entities.CouvertureGeographique couvertureGeographique);
}
