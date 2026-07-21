package tn.vermeg.gestionproduit.repositories;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.vermeg.gestionproduit.entities.Pack;
import java.util.List;
@Repository
public interface PackUnifiedRepository extends MongoRepository<Pack, String> {
    boolean existsByNomPackIgnoreCase(String nomPack);
    List<Pack> findByNomPackIgnoreCaseContaining(String nomPack);
    List<Pack> findByNiveauCouverture(tn.vermeg.gestionproduit.enums.NiveauCouverture niveauCouverture);
    List<Pack> findByProduitId(String produitId);
    List<Pack> findByPrixMensuelBetween(double prixMin, double prixMax);
}
