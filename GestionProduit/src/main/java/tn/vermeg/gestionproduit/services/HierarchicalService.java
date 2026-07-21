package tn.vermeg.gestionproduit.services;

import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.dto.PackDetailDTO;
import tn.vermeg.gestionproduit.dto.ProduitDetailDTO;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.entities.Pack;
import tn.vermeg.gestionproduit.entities.PackGarantie;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.enums.DomaineMedical;
import tn.vermeg.gestionproduit.exceptions.ResourceNotFoundException;
import tn.vermeg.gestionproduit.repositories.GarantieRepository;
import tn.vermeg.gestionproduit.repositories.PackGarantieRepository;
import tn.vermeg.gestionproduit.repositories.PackUnifiedRepository;
import tn.vermeg.gestionproduit.repositories.ProduitRepository;

import java.util.List;
import java.util.Objects;
@Service
public class HierarchicalService {

    private final ProduitRepository produitRepository;
    private final PackUnifiedRepository packRepository;
    private final PackGarantieRepository packGarantieRepository;
    private final GarantieRepository garantieRepository;

    public HierarchicalService(ProduitRepository produitRepository,
                               PackUnifiedRepository packRepository,
                               PackGarantieRepository packGarantieRepository,
                               GarantieRepository garantieRepository) {
        this.produitRepository = produitRepository;
        this.packRepository = packRepository;
        this.packGarantieRepository = packGarantieRepository;
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
// Récupère tous les produits avec leurs packs associé
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
// Récupère un pack avec toutes ses garanties associées
// Retourne l'entité Pack avec la liste des garanties
    public Pack getPackWithGaranties(String packId) {
        Pack pack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack", packId,
                    "Pack non trouvé avec l'ID: " + packId));

        pack.setGaranties(packGarantieRepository.findByPackIdAndActifTrue(packId));
        return pack;
    }
//Récupère tous les packs avec leurs garanties associées
    public List<Pack> getAllPacksWithGaranties() {
        List<Pack> packs = packRepository.findAll();
        return packs.stream()
                .map(pack -> {
                    pack.setGaranties(packGarantieRepository.findByPackIdAndActifTrue(pack.getIdPack()));
                    return pack;
                })
                .toList();
    }
// Récupère tous les packs d'un produit avec leurs garanties
    public List<Pack> getPacksByProduitWithGaranties(String produitId) {
        List<Pack> packs = packRepository.findByProduitId(produitId);
        return packs.stream()
                .map(pack -> {
                    pack.setGaranties(packGarantieRepository.findByPackIdAndActifTrue(pack.getIdPack()));
                    return pack;
                })
                .toList();
    }
    // ==================== HIÉRARCHIE COMPLÈTE ====================
    // Récupère la hiérarchie complète : Produit → Packs → Garanties
     // Cette méthode est utile pour l'affichage dans le frontend
    public Produit getProduitWithFullHierarchy(String produitId) {
        Produit produit = getProduitWithPacks(produitId);
        if (produit.getPacks() != null) {
            produit.getPacks().forEach(pack ->
                    pack.setGaranties(packGarantieRepository.findByPackIdAndActifTrue(pack.getIdPack())));
        }
        return produit;
    }
    // ==================== DONNÉES DÉRIVÉES (calculées à la demande) ====================
    // Détail d'un produit avec {@code idPacks} calculé à la demande
     // (jamais stocké/dénormalisé sur l'entité Produit).
    public ProduitDetailDTO getProduitDetail(String produitId) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", produitId,
                        "Produit non trouvé avec l'ID: " + produitId));
        List<String> idPacks = packRepository.findByProduitId(produitId).stream()
                .map(Pack::getIdPack)
                .toList();
        return new ProduitDetailDTO(produit, idPacks);
    }
//Détail d'un pack avec {@code idGaranties} et {@code domainesMedicaux} calculés
//à la demande via PackGarantie → Garantie.domaine (jamais stockés sur l'entité Pack).
    public PackDetailDTO getPackDetail(String packId) {
        Pack pack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack", packId,
                        "Pack non trouvé avec l'ID: " + packId));

        List<PackGarantie> associations = packGarantieRepository.findByPackIdAndActifTrue(packId);
        List<String> idGaranties = associations.stream()
                .map(PackGarantie::getGarantieId)
                .toList();

        List<DomaineMedical> domainesMedicaux = garantieRepository.findAllById(idGaranties).stream()
                .map(Garantie::getDomaine)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return new PackDetailDTO(pack, idGaranties, domainesMedicaux);
    }
}