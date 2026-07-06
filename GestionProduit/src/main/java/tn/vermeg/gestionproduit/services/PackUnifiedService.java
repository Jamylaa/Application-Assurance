package tn.vermeg.gestionproduit.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.entities.Pack;
import tn.vermeg.gestionproduit.entities.PackGarantie;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.enums.DomaineMedical;
import tn.vermeg.gestionproduit.enums.NiveauCouverture;
import tn.vermeg.gestionproduit.enums.StatutWorkflow;
import tn.vermeg.gestionproduit.enums.TypeMontant;
import tn.vermeg.gestionproduit.enums.TypePlafond;
import tn.vermeg.gestionproduit.enums.TypeProduit;
import tn.vermeg.gestionproduit.exceptions.ResourceNotFoundException;
import tn.vermeg.gestionproduit.repositories.GarantieRepository;
import tn.vermeg.gestionproduit.repositories.PackGarantieRepository;
import tn.vermeg.gestionproduit.repositories.PackUnifiedRepository;
import tn.vermeg.gestionproduit.repositories.ProduitRepository;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
@Service
public class PackUnifiedService {

    private final PackUnifiedRepository packRepository;
    private final PackGarantieRepository packGarantieRepository;
    private final GarantieRepository garantieRepository;
    private final ProduitRepository produitRepository;
    private final AssociationValidationService associationValidationService;

    public PackUnifiedService(PackUnifiedRepository packRepository,
                            PackGarantieRepository packGarantieRepository,
                            GarantieRepository garantieRepository,
                            ProduitRepository produitRepository,
                            AssociationValidationService associationValidationService) {
        this.packRepository = packRepository;
        this.packGarantieRepository = packGarantieRepository;
        this.garantieRepository = garantieRepository;
        this.produitRepository = produitRepository;
        this.associationValidationService = associationValidationService;
    }

    // ==================== GESTION DES PACKS ====================
     // Récupère tous les packs
    public List<Pack> getAllPacks() {
        return packRepository.findAll();
    }
    //Récupère un pack par son ID
    public Pack getPackById(String idPack) {
        return packRepository.findById(idPack)
                .orElseThrow(() -> new ResourceNotFoundException("Pack", idPack,
                    "Pack non trouvé avec l'ID: " + idPack));
    }
// Crée un nouveau pack
    public Pack createPack(@Valid Pack pack) {
        validatePack(pack);

        if (packRepository.existsByNomPackIgnoreCase(pack.getNomPack())) {
            throw new IllegalArgumentException("Un pack avec ce nom existe déjà.");
        }

        pack.setDateCreation(Instant.now());
        pack.setDateModification(Instant.now());

        return packRepository.save(pack);
    }
        //Met à jour un pack existant
      public Pack updatePack(String idPack, @Valid Pack details) {
        Pack existingPack = getPackById(idPack);

        validatePack(details);

        if (!existingPack.getNomPack().equals(details.getNomPack()) &&
            packRepository.existsByNomPackIgnoreCase(details.getNomPack())) {
            throw new IllegalArgumentException("Un pack avec ce nom existe déjà.");
        }

        // Mise à jour des champs
        existingPack.setNomPack(details.getNomPack());
        existingPack.setDescription(details.getDescription());
        existingPack.setPrixMensuel(details.getPrixMensuel());
        existingPack.setNiveauCouverture(details.getNiveauCouverture());
        existingPack.setVersionPack(details.getVersionPack());
        existingPack.setDateModification(Instant.now());

        return packRepository.save(existingPack);
    }
//Supprime un pack
    public void deletePack(String idPack) {
        Pack pack = getPackById(idPack);

        // Vérifier si des garanties sont associées
        List<PackGarantie> associations = packGarantieRepository.findByPackId(idPack);
        if (!associations.isEmpty()) {
            throw new IllegalStateException("Impossible de supprimer le pack: des garanties y sont associées");
        }

        packRepository.delete(pack);
    }

    // ==================== ASSOCIATION PACKS-PRODUITS ====================
// Associe un pack à un produit
    public Pack associatePackToProduit(String packId, String produitId) {
        Pack pack = getPackById(packId);
        if (!produitRepository.existsById(produitId)) {
            throw new ResourceNotFoundException("Produit", produitId,
                    "Produit non trouvé avec l'ID: " + produitId);
        }

        pack.setProduitId(produitId);
        pack.setDateModification(Instant.now());

        return packRepository.save(pack);
    }
//Dissocie un pack d'un produit
    public Pack dissociatePackFromProduit(String packId) {
        Pack pack = getPackById(packId);

        pack.setProduitId(null);
        pack.setDateModification(Instant.now());

        return packRepository.save(pack);
    }
//Récupère tous les packs associés à un produit
    public List<Pack> getPacksByProduitId(String produitId) {
        return packRepository.findByProduitId(produitId);
    }

    public List<Pack> searchPacksByNom(String nomPack) {
        if (nomPack == null || nomPack.isBlank()) {return List.of();}
        return packRepository.findByNomPackIgnoreCaseContaining(nomPack.trim());
    }

    public List<Pack> getPacksByNiveau(NiveauCouverture niveauCouverture) {
        return packRepository.findByNiveauCouverture(niveauCouverture);}

    public List<Pack> getPacksByPrixRange(double prixMin, double prixMax) {
        return packRepository.findByPrixMensuelBetween(prixMin, prixMax);}

    public List<Garantie> getGarantiesDisponiblesPourPack(String packId) {
        getPackById(packId);
        Set<String> linkedIds = packGarantieRepository.findByPackId(packId).stream()
                .map(PackGarantie::getGarantieId)
                .collect(Collectors.toSet());
        return garantieRepository.findAll().stream()
                .filter(g -> g.getIdGarantie() != null && !linkedIds.contains(g.getIdGarantie()))
                .collect(Collectors.toList());}

    public double calculerPrixTotalPack(String packId) {
        Pack pack = getPackById(packId);
        double supplements = packGarantieRepository.findByPackId(packId).stream()
                .mapToDouble(PackGarantie::getSupplementPrix)
                .sum();
        return pack.getPrixMensuel() + supplements;
    }

    // ==================== GESTION DES GARANTIES DE PACKS ====================
// Récupère toutes les associations pack-garantie
   public List<PackGarantie> getAllPackGaranties() {
        return packGarantieRepository.findAll();
    }

    //Récupère une association pack-garantie par son ID
    public PackGarantie getPackGarantieById(String idPackGarantie) {
        return packGarantieRepository.findById(idPackGarantie)
                .orElseThrow(() -> new ResourceNotFoundException("PackGarantie", idPackGarantie,
                    "Association Pack-Garantie non trouvée avec l'ID: " + idPackGarantie));
    }
//Récupère toutes les garanties d'un pack
      public List<PackGarantie> getGarantiesByPackId(String packId) {
        return packGarantieRepository.findByPackId(packId);
    }
   // Récupère tous les packs contenant une garantie
    public List<PackGarantie> getPacksByGarantieId(String garantieId) {
        return packGarantieRepository.findByGarantieId(garantieId);
    }

   //Ajoute une garantie à un pack
    public PackGarantie ajouterGarantieAuPack(String packId, String garantieId, @Valid PackGarantie packGarantie) {
        Pack pack = getPackById(packId);
        if (pack.getStatutWorkflow() != StatutWorkflow.PUBLIE) {
            throw new IllegalStateException(
                    "Impossible d'associer une garantie : le pack '" + pack.getNomPack()
                            + "' n'est pas PUBLIE (statut actuel = " + pack.getStatutWorkflow() + ").");
        }

        Garantie garantie = associationValidationService.validateGarantieExisteEtActive(garantieId);

        // Vérifier duplication
        if (packGarantieRepository.existsByPackIdAndGarantieId(packId, garantieId)) {
            throw new IllegalArgumentException("Garantie déjà ajoutée à ce pack");
        }
        // Initialisation propre
        packGarantie.setPackId(packId);
        packGarantie.setGarantieId(garantieId);
        packGarantie.setNomGarantie(garantie.getNomGarantie());
        packGarantie.setCodeGarantie(garantie.getCodeGarantie());
        packGarantie.setActif(true);
        packGarantie.setDateActivation(Instant.now());

        return packGarantieRepository.save(packGarantie);
    }
// Met à jour une association pack-garantie
    public PackGarantie updatePackGarantie(String idPackGarantie, @Valid PackGarantie details) {
        PackGarantie existing = getPackGarantieById(idPackGarantie);
        validatePackGarantie(details);

        // Mise à jour des champs
        existing.setTauxRemboursementSpecifique(details.getTauxRemboursementSpecifique());
        existing.setPlafondSpecifique(details.getPlafondSpecifique());
        existing.setFranchiseSpecifique(details.getFranchiseSpecifique());
        existing.setTypeMontant(details.getTypeMontant());
        existing.setOptionnelle(details.isOptionnelle());
        existing.setSupplementPrix(details.getSupplementPrix());

        return packGarantieRepository.save(existing);
    }
    //Supprime une garantie d'un pack
    public void supprimerGarantieDuPack(String idPackGarantie) {
        PackGarantie packGarantie = getPackGarantieById(idPackGarantie);
        packGarantieRepository.delete(packGarantie);
    }
//Active/désactive une garantie dans un pack
    public PackGarantie toggleGarantieActivation(String idPackGarantie, boolean active) {
        PackGarantie packGarantie = getPackGarantieById(idPackGarantie);
        packGarantie.setActif(active);
        if (active) {packGarantie.setDateActivation(Instant.now());
        } else {packGarantie.setDateDesactivation(Instant.now());}
        return packGarantieRepository.save(packGarantie);
    }
// Récupère les garanties optionnelles d'un pack
    public List<PackGarantie> getGarantiesOptionnellesByPackId(String packId) {
        return packGarantieRepository.findByPackIdAndOptionnelle(packId, true);}
    //Récupère les garanties obligatoires d'un pack
    public List<PackGarantie> getGarantiesObligatoiresByPackId(String packId) {
        return packGarantieRepository.findByPackIdAndOptionnelle(packId, false);}

    // ==================== MÉTHODES DE VALIDATION ====================
    private void validatePack(Pack pack) {
        if (pack.getNomPack() == null || pack.getNomPack().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du pack est obligatoire");
        }

        if (pack.getPrixMensuel() < 0) {
            throw new IllegalArgumentException("Le prix mensuel ne peut pas être négatif");}
    }

    private void validatePackGarantie(PackGarantie packGarantie) {
        if (packGarantie.getTauxRemboursementSpecifique() != null &&
                (packGarantie.getTauxRemboursementSpecifique() < 0 || packGarantie.getTauxRemboursementSpecifique() > 100)) {
            throw new IllegalArgumentException("Le taux de remboursement doit être entre 0 et 100");}

        if (packGarantie.getSupplementPrix() < 0) {
            throw new IllegalArgumentException("Le supplément de prix ne peut pas être négatif");}
    }
}
