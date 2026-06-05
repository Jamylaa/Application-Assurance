package tn.vermeg.gestionproduit.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.entities.Statut;
import tn.vermeg.gestionproduit.entities.TypeProduit;

import java.util.List;

@Repository
public interface ProduitRepository extends MongoRepository<Produit, String> {

    List<Produit> findByTypeProduit(TypeProduit typeProduit);
    List<Produit> findByStatut(Statut statut);
    List<Produit> findByNomProduitContainingIgnoreCase(String nomProduit);
    boolean existsByNomProduitIgnoreCase(String nomProduit);
}