package tn.vermeg.gestionproduit.repositories;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.vermeg.gestionproduit.entities.Pack;
import java.util.List;
import java.util.Optional;
@Repository
public interface PackUnifiedRepository extends MongoRepository<Pack, String> {
    boolean existsByNomPackIgnoreCase(String nomPack);

    Optional<Pack> findFirstByNomPackIgnoreCase(String nomPack);
    List<Pack> findByNomPackIgnoreCaseContaining(String nomPack);
    List<Pack> findByStatut(tn.vermeg.gestionproduit.enums.Statut statut);
    List<Pack> findByNiveauCouverture(tn.vermeg.gestionproduit.enums.NiveauCouverture niveauCouverture);
    List<Pack> findByTypeClientsContaining(tn.vermeg.gestionproduit.enums.TypeClient typeClient);
    List<Pack> findByProduitId(String produitId);
    List<Pack> findByPrixMensuelBetween(double prixMin, double prixMax);
    List<Pack> findByAgeMinimumLessThanEqualAndAgeMaximumGreaterThanEqual(int ageMax, int ageMin);
    long countByStatut(tn.vermeg.gestionproduit.enums.Statut statut);
    List<Pack> findByStatutAndPrixMensuelLessThanOrderByPrixMensuelAsc(tn.vermeg.gestionproduit.enums.Statut statut, double prixMax);
    List<Pack> findByCouvertureGeographique(tn.vermeg.gestionproduit.enums.CouvertureGeographique couvertureGeographique);
}
