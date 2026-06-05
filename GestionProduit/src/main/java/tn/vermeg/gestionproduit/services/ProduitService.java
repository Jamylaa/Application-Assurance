package tn.vermeg.gestionproduit.services;

import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.entities.Statut;
import tn.vermeg.gestionproduit.entities.TypeProduit;
import tn.vermeg.gestionproduit.repositories.ProduitRepository;

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

    public List<Produit> getProduitsByStatut(Statut statut) {
        return produitRepository.findByStatut(statut);
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

        produit.setStatut(Statut.ACTIF);

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
        produit.setStatut(produitDetails.getStatut());
        if (produitDetails.getCustomFields() != null) {
            produit.setCustomFields(produitDetails.getCustomFields());
        }

        return produitRepository.save(produit);
    }

    // DELETE
    public void deleteProduit(String idProduit) {

        if (!produitRepository.existsById(idProduit)) {
            throw new IllegalArgumentException("Produit non trouvé avec l'ID: " + idProduit);
        }

        produitRepository.deleteById(idProduit);
    }

    // DÉSACTIVATION
    public Produit desactiverProduit(String idProduit) {

        Produit produit = getProduitById(idProduit);

        if (produit.getStatut() == Statut.INACTIF) {
            throw new IllegalStateException("Le produit est déjà inactif.");
        }

        produit.setStatut(Statut.INACTIF);

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