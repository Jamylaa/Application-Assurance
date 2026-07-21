package tn.vermeg.gestionproduit.services;

import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.enums.StatutWorkflow;
import tn.vermeg.gestionproduit.enums.TypeProduit;
import tn.vermeg.gestionproduit.repositories.ProduitRepository;
import tn.vermeg.gestionproduit.util.ValidationUtils;

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
    // Mise à jour partielle : seuls les champs non nuls/non vides de produitDetails sont appliqués,
    // les autres champs existants du produit sont conservés tels quels (sémantique de fusion,
    // adaptée à un appelant qui n'envoie que les champs qu'il souhaite réellement modifier).
    public Produit updateProduit(String idProduit, Produit produitDetails) {
        Produit produit = getProduitById(idProduit);

        if (produitDetails.getNomProduit() != null && !produitDetails.getNomProduit().isBlank()) {
            boolean nomChange = !produit.getNomProduit().equalsIgnoreCase(produitDetails.getNomProduit());
            if (nomChange && produitRepository.existsByNomProduitIgnoreCase(produitDetails.getNomProduit())) {
                throw new IllegalArgumentException("Un produit avec ce nom existe déjà.");
            }
            produit.setNomProduit(produitDetails.getNomProduit());
        }
        if (produitDetails.getCodeProduit() != null && !produitDetails.getCodeProduit().isBlank()) {
            produit.setCodeProduit(produitDetails.getCodeProduit());
        }
        if (produitDetails.getNomCommercial() != null) produit.setNomCommercial(produitDetails.getNomCommercial());
        if (produitDetails.getDescription() != null) produit.setDescription(produitDetails.getDescription());
        if (produitDetails.getTypeProduit() != null) produit.setTypeProduit(produitDetails.getTypeProduit());
        if (produitDetails.getStatutWorkflow() != null) produit.setStatutWorkflow(produitDetails.getStatutWorkflow());
        if (produitDetails.getPrixBase() > 0) produit.setPrixBase(produitDetails.getPrixBase());
        if (produitDetails.getDevisePrix() != null) produit.setDevisePrix(produitDetails.getDevisePrix());
        if (produitDetails.getCouvertureGeographique() != null) {
            produit.setCouvertureGeographique(produitDetails.getCouvertureGeographique());
        }
        if (produitDetails.getTerritoiresExclus() != null) produit.setTerritoiresExclus(produitDetails.getTerritoiresExclus());
        if (produitDetails.getVersion() != null) produit.setVersion(produitDetails.getVersion());
        if (produitDetails.getVersionPrecedenteId() != null) {
            produit.setVersionPrecedenteId(produitDetails.getVersionPrecedenteId());
        }
        if (produitDetails.getDateEffet() != null) produit.setDateEffet(produitDetails.getDateEffet());
        if (produitDetails.getDateExpiration() != null) produit.setDateExpiration(produitDetails.getDateExpiration());
        if (produitDetails.getModifiePar() != null) produit.setModifiePar(produitDetails.getModifiePar());

        validateProduit(produit);
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
        if (produit.getStatutWorkflow() == StatutWorkflow.PUBLIE) {
            return produit;
        }
        if (produit.getStatutWorkflow() == StatutWorkflow.ARCHIVE || produit.getStatutWorkflow() == StatutWorkflow.REJETE) {
            throw new IllegalStateException(
                    "Un produit " + produit.getStatutWorkflow() + " ne peut pas être publié directement.");
        }
        produit.setStatutWorkflow(StatutWorkflow.PUBLIE);
        produit.setValidePar(utilisateur);
        produit.setDateValidation(Instant.now());
        return produitRepository.save(produit);
    }
    // VALIDATION MÉTIER
    private void validateProduit(Produit produit) {
        ValidationUtils.requireNonBlank(produit.getNomProduit(), "Le nom du produit est obligatoire.");
        if (produit.getTypeProduit() == null) {
            throw new IllegalArgumentException("Le type du produit est obligatoire.");
        }
    }
}