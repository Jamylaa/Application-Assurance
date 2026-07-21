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
import tn.vermeg.gestionproduit.util.ValidationUtils;

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
        //Met à jour un pack existant (fusion partielle : seuls les champs fournis dans "details"
        //sont appliqués, les autres champs existants sont conservés)
      public Pack updatePack(String idPack, Pack details) {
        Pack existingPack = getPackById(idPack);

        if (details.getNomPack() != null && !details.getNomPack().isBlank()) {
            if (!existingPack.getNomPack().equalsIgnoreCase(details.getNomPack()) &&
                packRepository.existsByNomPackIgnoreCase(details.getNomPack())) {
                throw new IllegalArgumentException("Un pack avec ce nom existe déjà.");
            }
            existingPack.setNomPack(details.getNomPack());
        }
        if (details.getCodePack() != null && !details.getCodePack().isBlank()) {
            existingPack.setCodePack(details.getCodePack());
        }
        if (details.getNomCommercial() != null) existingPack.setNomCommercial(details.getNomCommercial());
        if (details.getDescription() != null) existingPack.setDescription(details.getDescription());
        if (details.getDescriptionCourte() != null) existingPack.setDescriptionCourte(details.getDescriptionCourte());
        if (details.getPrixMensuel() > 0) existingPack.setPrixMensuel(details.getPrixMensuel());
        if (details.getPrixAnnuel() > 0) existingPack.setPrixAnnuel(details.getPrixAnnuel());
        if (details.getTauxRemiseAnnuelle() > 0) existingPack.setTauxRemiseAnnuelle(details.getTauxRemiseAnnuelle());
        if (details.getDevisePrix() != null) existingPack.setDevisePrix(details.getDevisePrix());
        if (details.getNiveauCouverture() != null) existingPack.setNiveauCouverture(details.getNiveauCouverture());
        if (details.getStatutWorkflow() != null) existingPack.setStatutWorkflow(details.getStatutWorkflow());
        if (details.getColorTheme() != null) existingPack.setColorTheme(details.getColorTheme());
        if (details.getVersionPack() != null) existingPack.setVersionPack(details.getVersionPack());
        if (details.getOptionsPackIds() != null) existingPack.setOptionsPackIds(details.getOptionsPackIds());
        if (details.getPacksCompatibles() != null) existingPack.setPacksCompatibles(details.getPacksCompatibles());
        if (details.getPacksIncompatibles() != null) existingPack.setPacksIncompatibles(details.getPacksIncompatibles());
        if (details.getDateEffet() != null) existingPack.setDateEffet(details.getDateEffet());
        if (details.getDateExpiration() != null) existingPack.setDateExpiration(details.getDateExpiration());
        if (details.getModifiePar() != null) existingPack.setModifiePar(details.getModifiePar());
        existingPack.setDateModification(Instant.now());

        validatePack(existingPack);
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
    // Publie un pack (le rend actif/commercialisable et éligible aux associations de garanties)
    public Pack publierPack(String idPack, String utilisateur) {
        Pack pack = getPackById(idPack);

        if (pack.getStatutWorkflow() == StatutWorkflow.PUBLIE) {
            return pack;
        }
        if (pack.getStatutWorkflow() == StatutWorkflow.ARCHIVE || pack.getStatutWorkflow() == StatutWorkflow.REJETE) {
            throw new IllegalStateException(
                    "Un pack " + pack.getStatutWorkflow() + " ne peut pas être publié directement.");
        }
        pack.setStatutWorkflow(StatutWorkflow.PUBLIE);
        pack.setModifiePar(utilisateur);
        pack.setDateModification(Instant.now());

        return packRepository.save(pack);
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
        ValidationUtils.requireNonBlank(pack.getNomPack(), "Le nom du pack est obligatoire");

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
