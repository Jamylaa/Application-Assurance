package tn.vermeg.gestionproduit.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import tn.vermeg.gestionproduit.entities.CritereRecommandation;

import java.util.List;
import java.util.Optional;

@Repository
public interface CritereRecommandationRepository extends MongoRepository<CritereRecommandation, String> {

    List<CritereRecommandation> findByProduitId(String produitId);

    Optional<CritereRecommandation> findByPackId(String packId);

    List<CritereRecommandation> findByProduitIdAndActifTrue(String produitId);

    List<CritereRecommandation> findByActifTrue();

    List<CritereRecommandation> findByMotsClesContaining(String motCle);

    List<CritereRecommandation> findByCasUsagesContaining(String casUsage);

    @Query("{ 'actif': true, 'motsCles': { $in: ?0 } }")
    List<CritereRecommandation> findByMotsClesIn(List<String> motsCles);

    @Query("{ 'actif': true, $or: [ { 'motsCles': { $in: ?0 } }, { 'synonymes': { $in: ?0 } } ] }")
    List<CritereRecommandation> findByMotsClesOrSynonymesIn(List<String> termes);

    boolean existsByPackId(String packId);

    long countByActifTrue();
}
