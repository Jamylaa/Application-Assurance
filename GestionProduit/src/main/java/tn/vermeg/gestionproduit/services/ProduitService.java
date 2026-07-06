package tn.vermeg.gestionproduit.services;

import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.enums.StatutWorkflow;
import tn.vermeg.gestionproduit.enums.TypeProduit;
import tn.vermeg.gestionproduit.repositories.ProduitRepository;

import java.time.Instant;
import java.util.List;

@Service
public class ProduitService {

    private final ProduitRepository produitRepository;

    public ProduitService(ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
    }

    // READ
    public List<Produit> getAllProduits() {
        return produitRepository.findAll();
    }

    public Produit getProduitById(String idProduit) {
        return produitRepository.findById(idProduit)
                .orElseThrow(() ->
                        new IllegalArgumentException("Produit non trouvé avec l'ID: " + idProduit));
    }

    public List<Produit> getProduitsByType(TypeProduit typeProduit) {
        return produitRepository.findByTypeProduit(typeProduit);
    }

    public List<Produit> searchProduits(String nomProduit) {
        return produitRepository.findByNomProduitContainingIgnoreCase(nomProduit);
    }

    // CREATE
    public Produit createProduit(Produit produit) {

        validateProduit(produit);

        if (produitRepository.existsByNomProduitIgnoreCase(produit.getNomProduit())) {
            throw new IllegalArgumentException("Un produit avec ce nom existe déjà.");
        }

        return produitRepository.save(produit);
    }

    // UPDATE
    public Produit updateProduit(String idProduit, Produit produitDetails) {

        Produit produit = getProduitById(idProduit);

        validateProduit(produitDetails);

        boolean nomChange = !produit.getNomProduit()
                .equalsIgnoreCase(produitDetails.getNomProduit());

        if (nomChange &&
                produitRepository.existsByNomProduitIgnoreCase(produitDetails.getNomProduit())) {
            throw new IllegalArgumentException("Un produit avec ce nom existe déjà.");
        }

        produit.setNomProduit(produitDetails.getNomProduit());
        produit.setDescription(produitDetails.getDescription());
        produit.setTypeProduit(produitDetails.getTypeProduit());

        return produitRepository.save(produit);
    }

    // DELETE
    public void deleteProduit(String idProduit) {

        if (!produitRepository.existsById(idProduit)) {
            throw new IllegalArgumentException("Produit non trouvé avec l'ID: " + idProduit);
        }

        produitRepository.deleteById(idProduit);
    }

    // PUBLICATION
    public Produit publierProduit(String idProduit, String utilisateur) {

        Produit produit = getProduitById(idProduit);

        if (produit.getStatutWorkflow() != StatutWorkflow.APPROUVE) {
            throw new IllegalStateException(
                    "Le produit doit être au statut APPROUVE avant publication (statut actuel: "
                            + produit.getStatutWorkflow() + ").");
        }

        produit.setStatutWorkflow(StatutWorkflow.PUBLIE);
        produit.setValidePar(utilisateur);
        produit.setDateValidation(Instant.now());

        return produitRepository.save(produit);
    }

    // VALIDATION MÉTIER
    private void validateProduit(Produit produit) {

        if (produit.getNomProduit() == null || produit.getNomProduit().isBlank()) {
            throw new IllegalArgumentException("Le nom du produit est obligatoire.");
        }

        if (produit.getTypeProduit() == null) {
            throw new IllegalArgumentException("Le type du produit est obligatoire.");
        }
    }
}
