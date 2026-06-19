package tn.vermeg.gestionproduit.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.entities.Pack;
import tn.vermeg.gestionproduit.entities.PackGarantie;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.enums.CouvertureGeographique;
import tn.vermeg.gestionproduit.enums.DomaineMedical;
import tn.vermeg.gestionproduit.enums.NiveauCouverture;
import tn.vermeg.gestionproduit.enums.Statut;
import tn.vermeg.gestionproduit.enums.TypeClient;
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
@Transactional
public class PackUnifiedService {

    private final PackUnifiedRepository packRepository;
    private final PackGarantieRepository packGarantieRepository;
    private final GarantieRepository garantieRepository;
    private final ProduitRepository produitRepository;

    public PackUnifiedService(PackUnifiedRepository packRepository,
                            PackGarantieRepository packGarantieRepository,
                            GarantieRepository garantieRepository,
                            ProduitRepository produitRepository) {
        this.packRepository = packRepository;
        this.packGarantieRepository = packGarantieRepository;
        this.garantieRepository = garantieRepository;
        this.produitRepository = produitRepository;
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

        pack.setStatut(Statut.ACTIF);
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
        existingPack.setAgeMinimum(details.getAgeMinimum());
        existingPack.setAgeMaximum(details.getAgeMaximum());
        existingPack.setTypeClients(details.getTypeClients());
        existingPack.setAncienneteContratMois(details.getAncienneteContratMois());
        existingPack.setCouvertureGeographique(details.getCouvertureGeographique());
        existingPack.setPrixMensuel(details.getPrixMensuel());
        existingPack.setDureeMinContrat(details.getDureeMinContrat());
        existingPack.setDureeMaxContrat(details.getDureeMaxContrat());
        existingPack.setNiveauCouverture(details.getNiveauCouverture());
        existingPack.setStatut(details.getStatut());
        existingPack.setDomainesMedicaux(details.getDomainesMedicaux());
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
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", produitId, 
                    "Produit non trouvé avec l'ID: " + produitId));

        pack.setProduitId(produitId);
        pack.setNomProduit(produit.getNomProduit());
        pack.setDateModification(Instant.now());

        return packRepository.save(pack);
    }
//Dissocie un pack d'un produit
    public Pack dissociatePackFromProduit(String packId) {
        Pack pack = getPackById(packId);
        
        pack.setProduitId(null);
        pack.setNomProduit(null);
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

    public List<Pack> getPacksByStatut(Statut statut) {
        return packRepository.findByStatut(statut);
    }

    public List<Pack> getPacksByNiveau(NiveauCouverture niveauCouverture) {
        return packRepository.findByNiveauCouverture(niveauCouverture);}

    public List<Pack> getPacksByTypeClient(TypeClient typeClient) {
        return packRepository.findByTypeClientsContaining(typeClient);}

    public List<Pack> getPacksByPrixRange(double prixMin, double prixMax) {
        return packRepository.findByPrixMensuelBetween(prixMin, prixMax);}

    public Pack desactiverPack(String idPack) {
        Pack pack = getPackById(idPack);
        pack.setStatut(Statut.INACTIF);
        pack.setDateModification(Instant.now());
        return packRepository.save(pack);}

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
        Garantie garantie = garantieRepository.findById(garantieId)
                .orElseThrow(() -> new ResourceNotFoundException("Garantie", garantieId, 
                    "Garantie non trouvée avec l'ID: " + garantieId));

        // Vérifier duplication
        if (packGarantieRepository.existsByPackIdAndGarantieId(packId, garantieId)) {
            throw new IllegalArgumentException("Garantie déjà ajoutée à ce pack");
        }
        // Initialisation propre
        packGarantie.setPackId(packId);
        packGarantie.setGarantieId(garantieId);
        packGarantie.setNomGarantie(garantie.getNomGarantie());
        packGarantie.setPriorite(packGarantie.getPriorite() <= 0 ? 1 : packGarantie.getPriorite());
        packGarantie.setActif(true);
        packGarantie.setDelaiCarence(Math.max(packGarantie.getDelaiCarence(), 0));
        packGarantie.setDateActivation(Instant.now());

        return packGarantieRepository.save(packGarantie);
    }
// Met à jour une association pack-garantie
    public PackGarantie updatePackGarantie(String idPackGarantie, @Valid PackGarantie details) {
        PackGarantie existing = getPackGarantieById(idPackGarantie);
        validatePackGarantie(details);

        // Mise à jour des champs
        existing.setTauxRemboursement(details.getTauxRemboursement());
        existing.setPlafond(details.getPlafond());
        existing.setFranchise(details.getFranchise());
        existing.setTypeMontant(details.getTypeMontant());
        existing.setDelaiCarence(details.getDelaiCarence());
        existing.setPriorite(details.getPriorite());
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

        if (pack.getAgeMinimum() != null && pack.getAgeMaximum() != null) {
            if (pack.getAgeMinimum() > pack.getAgeMaximum()) {
                throw new IllegalArgumentException("L'âge minimum ne peut pas être supérieur à l'âge maximum");}
            if (pack.getAgeMinimum() < 0 || pack.getAgeMaximum() > 120) {
                throw new IllegalArgumentException("Les âges doivent être compris entre 0 et 120 ans");}}

        if (pack.getPrixMensuel() < 0) {
            throw new IllegalArgumentException("Le prix mensuel ne peut pas être négatif");}

        if (pack.getDureeMinContrat() > pack.getDureeMaxContrat()) {
            throw new IllegalArgumentException("La durée minimale ne peut pas être supérieure à la durée maximale");}

        if (pack.getAncienneteContratMois() < 0) {
            throw new IllegalArgumentException("L'ancienneté de contrat ne peut pas être négative");}
    }

    private void validatePackGarantie(PackGarantie packGarantie) {
        if (packGarantie.getTauxRemboursement() < 0 || packGarantie.getTauxRemboursement() > 1) {
            throw new IllegalArgumentException("Le taux de remboursement doit être entre 0 et 1");}

        if (packGarantie.getPlafond() < 0) {
            throw new IllegalArgumentException("Le plafond ne peut pas être négatif");}

        if (packGarantie.getFranchise() < 0) {
            throw new IllegalArgumentException("La franchise ne peut pas être négative");}

        if (packGarantie.getDelaiCarence() < 0) {
            throw new IllegalArgumentException("Le délai de carence ne peut pas être négatif");}

        if (packGarantie.getPriorite() < 1) {
            throw new IllegalArgumentException("La priorité doit être supérieure ou égale à 1");}

        if (packGarantie.getSupplementPrix() < 0) {
            throw new IllegalArgumentException("Le supplément de prix ne peut pas être négatif");}
    }
}