package tn.vermeg.gestionproduit.services;

import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.entities.Pack;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.exceptions.ResourceNotFoundException;
import tn.vermeg.gestionproduit.repositories.GarantieRepository;
import tn.vermeg.gestionproduit.repositories.PackUnifiedRepository;
import tn.vermeg.gestionproduit.repositories.ProduitRepository;

import java.util.List;

/**
 * Service pour gérer les relations hiérarchiques entre Produit → Pack → Garantie
 * Utilise directement les entités MongoDB sans DTOs
 */
@Service
public class HierarchicalService {

    private final ProduitRepository produitRepository;
    private final PackUnifiedRepository packRepository;
    private final GarantieRepository garantieRepository;

    public HierarchicalService(ProduitRepository produitRepository,
                               PackUnifiedRepository packRepository,
                               GarantieRepository garantieRepository) {
        this.produitRepository = produitRepository;
        this.packRepository = packRepository;
        this.garantieRepository = garantieRepository;
    }

    // ==================== PRODUIT AVEC PACKS ====================

    /**
     * Récupère un produit avec tous ses packs associés
     * Retourne l'entité Produit avec la liste des packs
     */
    public Produit getProduitWithPacks(String produitId) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", produitId,
                    "Produit non trouvé avec l'ID: " + produitId));

        List<Pack> packs = packRepository.findByProduitId(produitId);
        produit.setPacks(packs);
        return produit;
    }

    /**
     * Récupère tous les produits avec leurs packs associés
     */
    public List<Produit> getAllProduitsWithPacks() {
        List<Produit> produits = produitRepository.findAll();
        return produits.stream()
                .map(produit -> {
                    List<Pack> packs = packRepository.findByProduitId(produit.getIdProduit());
                    produit.setPacks(packs);
                    return produit;
                })
                .toList();
    }

    // ==================== PACK AVEC GARANTIES ====================

    /**
     * Récupère un pack avec toutes ses garanties associées
     * Retourne l'entité Pack avec la liste des garanties
     */
    public Pack getPackWithGaranties(String packId) {
        Pack pack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack", packId,
                    "Pack non trouvé avec l'ID: " + packId));

        List<Garantie> garanties = garantieRepository.findByPackId(packId);
        pack.setGaranties(garanties);
        return pack;
    }

    /**
     * Récupère tous les packs avec leurs garanties associées
     */
    public List<Pack> getAllPacksWithGaranties() {
        List<Pack> packs = packRepository.findAll();
        return packs.stream()
                .map(pack -> {
                    List<Garantie> garanties = garantieRepository.findByPackId(pack.getIdPack());
                    pack.setGaranties(garanties);
                    return pack;
                })
                .toList();
    }

    /**
     * Récupère tous les packs d'un produit avec leurs garanties
     */
    public List<Pack> getPacksByProduitWithGaranties(String produitId) {
        List<Pack> packs = packRepository.findByProduitId(produitId);
        return packs.stream()
                .map(pack -> {
                    List<Garantie> garanties = garantieRepository.findByPackId(pack.getIdPack());
                    pack.setGaranties(garanties);
                    return pack;
                })
                .toList();
    }

    // ==================== HIÉRARCHIE COMPLÈTE ====================

    /**
     * Récupère la hiérarchie complète : Produit → Packs → Garanties
     * Cette méthode est utile pour l'affichage dans le frontend
     */
    public Produit getProduitWithFullHierarchy(String produitId) {
        Produit produit = getProduitWithPacks(produitId);
        if (produit.getPacks() != null) {
            produit.getPacks().forEach(pack -> {
                List<Garantie> garanties = garantieRepository.findByPackId(pack.getIdPack());
                pack.setGaranties(garanties);
            });
        }
        return produit;
    }

    /**
     * Met à jour la référence packId dans une garantie lors de l'association
     */
    public Garantie updateGarantiePackReference(String garantieId, String packId) {
        Garantie garantie = garantieRepository.findById(garantieId)
                .orElseThrow(() -> new ResourceNotFoundException("Garantie", garantieId,
                    "Garantie non trouvée avec l'ID: " + garantieId));

        garantie.setPackId(packId);
        return garantieRepository.save(garantie);
    }

    /**
     * Supprime la référence packId d'une garantie lors de la dissociation
     */
    public Garantie removeGarantiePackReference(String garantieId) {
        Garantie garantie = garantieRepository.findById(garantieId)
                .orElseThrow(() -> new ResourceNotFoundException("Garantie", garantieId,
                    "Garantie non trouvée avec l'ID: " + garantieId));

        garantie.setPackId(null);
        return garantieRepository.save(garantie);
    }
}
