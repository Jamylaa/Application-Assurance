package tn.vermeg.gestionproduit.services.chatbot.orchestration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
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
import tn.vermeg.gestionproduit.repositories.ProduitRepository;
import tn.vermeg.gestionproduit.repositories.PackUnifiedRepository;
import tn.vermeg.gestionproduit.repositories.GarantieRepository;
import tn.vermeg.gestionproduit.services.PackUnifiedService;

import java.util.*;
@Service
public class BusinessRelationshipService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessRelationshipService.class);

    private final ProduitRepository produitRepository;
    private final PackUnifiedRepository packUnifiedRepository;
    private final GarantieRepository garantieRepository;
    private final PackUnifiedService packUnifiedService;

    public BusinessRelationshipService(
            ProduitRepository produitRepository,
            PackUnifiedRepository packUnifiedRepository,
            GarantieRepository garantieRepository,
            PackUnifiedService packUnifiedService) {
        this.produitRepository = produitRepository;
        this.packUnifiedRepository = packUnifiedRepository;
        this.garantieRepository = garantieRepository;
        this.packUnifiedService = packUnifiedService;
    }

    public static class AssociationResult {
        private boolean success;
        private String message;
        private Map<String, Object> associations = new HashMap<>();
        private List<String> warnings = new ArrayList<>();
        private List<String> errors = new ArrayList<>();

        public AssociationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Map<String, Object> getAssociations() { return associations; }
        public void setAssociations(Map<String, Object> associations) { this.associations = associations; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getErrors() { return errors; }
    }
// Associe automatiquement un pack à son produit parent
    public AssociationResult associatePackToProduit(String packId, String produitId) {
        logger.info("Association du pack {} au produit {}", packId, produitId);
        AssociationResult result = new AssociationResult(true, "Association pack-produit initiée");
        try {
            // Vérifier que le pack existe
            Optional<Pack> packOpt = packUnifiedRepository.findById(packId);
            if (packOpt.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("Pack non trouvé avec l'ID: " + packId);
                result.getErrors().add("Pack introuvable");
                return result;
            }
            Pack pack = packOpt.get();
            
            // Vérifier que le produit existe
            Optional<Produit> produitOpt = produitRepository.findById(produitId);
            if (produitOpt.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("Produit non trouvé avec l'ID: " + produitId);
                result.getErrors().add("Produit introuvable");
                return result;
            }
            Produit produit = produitOpt.get();
            // Vérifier si le pack est déjà associé à un produit
            if (pack.getProduitId() != null) {
                if (pack.getProduitId().equals(produitId)) {
                    result.getWarnings().add("Le pack est déjà associé à ce produit");
                } else {
                    result.getWarnings().add("Le pack était déjà associé à un autre produit: " + 
                        pack.getNomProduit());
                }
            }
            // Effectuer l'association en mettant à jour les champs dénormalisés
            pack.setProduitId(produit.getIdProduit());
            pack.setNomProduit(produit.getNomProduit());
            Pack updatedPack = packUnifiedRepository.save(pack);
            
            Map<String, Object> association = new HashMap<>();
            association.put("packId", updatedPack.getIdPack());
            association.put("packNom", updatedPack.getNomPack());
            association.put("produitId", produit.getIdProduit());
            association.put("produitNom", produit.getNomProduit());
            association.put("typeProduit", produit.getTypeProduit());
            result.getAssociations().put("packProduit", association);
            result.setMessage("Pack associé au produit avec succès");
            logger.info("Association réussie: Pack {} -> Produit {}", pack.getNomPack(), produit.getNomProduit());
        } catch (Exception e) {
            logger.error("Erreur lors de l'association pack-produit: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Erreur lors de l'association: " + e.getMessage());
            result.getErrors().add(e.getMessage());
        }
        
        return result;
    }
//Associe automatiquement des garanties à un pack
    public AssociationResult associateGarantiesToPack(String packId, List<String> garantieIds) {
        logger.info("Association de {} garanties au pack {}", garantieIds.size(), packId);
        AssociationResult result = new AssociationResult(true, "Association garanties-pack initiée");
        List<Map<String, Object>> associations = new ArrayList<>();
        try {
            // Vérifier que le pack existe
            Optional<Pack> packOpt = packUnifiedRepository.findById(packId);
            if (packOpt.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("Pack non trouvé avec l'ID: " + packId);
                result.getErrors().add("Pack introuvable");
                return result;
            }
            Pack pack = packOpt.get();
            // Traiter chaque garantie
            for (String garantieId : garantieIds) {
                Optional<Garantie> garantieOpt = garantieRepository.findById(garantieId);
                if (garantieOpt.isEmpty()) {
                    result.getWarnings().add("Garantie non trouvée avec l'ID: " + garantieId);
                    continue;
                }
                Garantie garantie = garantieOpt.get();
                // Vérifier si l'association existe déjà (à implémenter avec requête repository)
                // Créer l'association
                PackGarantie packGarantie = new PackGarantie();
                packGarantie.setPackId(pack.getIdPack());
                packGarantie.setGarantieId(garantie.getIdGarantie());
                packGarantie.setNomGarantie(garantie.getNomGarantie());
                packGarantie.setActif(true);
                PackGarantie saved = packUnifiedService.ajouterGarantieAuPack(packId, garantieId, packGarantie);
                Map<String, Object> association = new HashMap<>();
                association.put("packGarantieId", saved.getIdPackGarantie());
                association.put("packId", pack.getIdPack());
                association.put("packNom", pack.getNomPack());
                association.put("garantieId", garantie.getIdGarantie());
                association.put("garantieNom", garantie.getNomGarantie());
                association.put("domaineMedical", garantie.getDomaine());
                associations.add(association);
            }
            result.getAssociations().put("garantiesPack", associations);
            result.setMessage(associations.size() + " garantie(s) associée(s) au pack avec succès");
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'association garanties-pack: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Erreur lors de l'association: " + e.getMessage());
            result.getErrors().add(e.getMessage());}
        return result;
    }
// Associe automatiquement des garanties par nom à un pack
    public AssociationResult associateGarantiesByNameToPack(String packId, List<String> garantieNoms) {
        logger.info("Association de garanties par nom au pack {}", packId);
        AssociationResult result = new AssociationResult(true, "Association garanties par nom initiée");
        try {
            // Rechercher les IDs des garanties par nom
            List<String> garantieIds = new ArrayList<>();
            List<String> notFound = new ArrayList<>();
            for (String nom : garantieNoms) {
                List<Garantie> matchingGaranties = garantieRepository.findByNomGarantieContainingIgnoreCase(nom);
                if (matchingGaranties.isEmpty()) {
                    notFound.add(nom);
                } else {
                    // Prendre la première correspondance (ou la plus pertinente)
                    garantieIds.add(matchingGaranties.get(0).getIdGarantie());
                }
            }
            if (!notFound.isEmpty()) {
                result.getWarnings().add("Garanties non trouvées: " + String.join(", ", notFound));
            }
            if (garantieIds.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("Aucune garantie trouvée avec les noms fournis");
                result.getErrors().add("Aucune correspondance trouvée");
                return result;
            }
            // Associer les garanties trouvées
            return associateGarantiesToPack(packId, garantieIds);
        } catch (Exception e) {
            logger.error("Erreur lors de l'association garanties par nom: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Erreur lors de l'association: " + e.getMessage());
            result.getErrors().add(e.getMessage());
        }
        
        return result;
    }
// Valide la hiérarchie des entités avant création - Structure cible: PRODUIT -> PACK -> GARANTIES
    public AssociationResult validateEntityHierarchy(Map<String, Object> entityData) {
        logger.info("Validation de la hiérarchie des entités");
        AssociationResult result = new AssociationResult(true, "Hiérarchie validée");
        // Vérifier si un produit est spécifié ou doit être créé
        String produitId = (String) entityData.get("produitId");
        String produitNom = (String) entityData.get("produitNom");
        if (produitId == null && produitNom == null) {
            result.getWarnings().add("Aucun produit spécifié - un nouveau produit sera créé");
        } else if (produitId != null) {
            Optional<Produit> produitOpt = produitRepository.findById(produitId);
            if (produitOpt.isEmpty()) {
                result.getWarnings().add("Produit ID non trouvé - un nouveau produit sera créé");
            } else {
                Map<String, Object> produitInfo = new HashMap<>();
                produitInfo.put("id", produitOpt.get().getIdProduit());
                produitInfo.put("nom", produitOpt.get().getNomProduit());
                produitInfo.put("type", produitOpt.get().getTypeProduit());
                result.getAssociations().put("produitExistant", produitInfo);
            }
        } else if (produitNom != null) {
            List<Produit> matchingProduits = produitRepository.findByNomProduitContainingIgnoreCase(produitNom);
            if (!matchingProduits.isEmpty()) {
                Map<String, Object> produitInfo = new HashMap<>();
                produitInfo.put("id", matchingProduits.get(0).getIdProduit());
                produitInfo.put("nom", matchingProduits.get(0).getNomProduit());
                produitInfo.put("type", matchingProduits.get(0).getTypeProduit());
                result.getAssociations().put("produitTrouve", produitInfo);
                result.getWarnings().add("Produit existant trouvé avec le nom: " + produitNom);
            }
        }
        
        // Vérifier les garanties
        List<String> garantieNoms = (List<String>) entityData.get("garanties");
        if (garantieNoms != null && !garantieNoms.isEmpty()) {
            List<Map<String, Object>> garantiesTrouvees = new ArrayList<>();
            List<String> garantiesNonTrouvees = new ArrayList<>();
            
            for (String nom : garantieNoms) {
                List<Garantie> matchingGaranties = garantieRepository.findByNomGarantieContainingIgnoreCase(nom);
                if (!matchingGaranties.isEmpty()) {
                    Map<String, Object> garantieInfo = new HashMap<>();
                    garantieInfo.put("id", matchingGaranties.get(0).getIdGarantie());
                    garantieInfo.put("nom", matchingGaranties.get(0).getNomGarantie());
                    garantieInfo.put("domaine", matchingGaranties.get(0).getDomaine());
                    garantiesTrouvees.add(garantieInfo);
                } else {
                    garantiesNonTrouvees.add(nom);
                }
            }
            result.getAssociations().put("garantiesExistantes", garantiesTrouvees);
            if (!garantiesNonTrouvees.isEmpty()) {
                result.getWarnings().add("Garanties non trouvées (seront créées): " + 
                    String.join(", ", garantiesNonTrouvees));
            }
        }
        return result;
    }
//Crée la structure hiérarchique complète: PRODUIT -> PACK -> GARANTIES
      public AssociationResult createCompleteHierarchy(
            String produitNom,
            String packNom,
            List<String> garantieNoms,
            Map<String, Object> additionalData) {
        
        logger.info("Création de la hiérarchie complète: Produit {} -> Pack {} -> Garanties {}", 
            produitNom, packNom, garantieNoms);
        AssociationResult result = new AssociationResult(true, "Création de hiérarchie initiée");
        try {
            // Étape 1: Créer ou retrouver le produit
            Produit produit = createOrRetrieveProduit(produitNom, additionalData);
            Map<String, Object> produitMap = new HashMap<>();
            produitMap.put("id", produit.getIdProduit());
            produitMap.put("nom", produit.getNomProduit());
            produitMap.put("action", produitNom.equals(produit.getNomProduit()) ? "CRÉÉ" : "RÉCUPÉRÉ");
            result.getAssociations().put("produit", produitMap);
            // Étape 2: Créer le pack associé au produit
            Pack pack = createPackForProduit(packNom, produit.getIdProduit(), additionalData);
            Map<String, Object> packMap = new HashMap<>();
            packMap.put("id", pack.getIdPack());
            packMap.put("nom", pack.getNomPack());
            packMap.put("action", "CRÉÉ");
            result.getAssociations().put("pack", packMap);
            // Étape 3: Associer les garanties au pack
            if (garantieNoms != null && !garantieNoms.isEmpty()) {
                AssociationResult garantieResult = associateGarantiesByNameToPack(pack.getIdPack(), garantieNoms);
                result.getAssociations().put("garanties", garantieResult.getAssociations());
                result.getWarnings().addAll(garantieResult.getWarnings());
            }
            result.setMessage("Hiérarchie créée avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de la création de la hiérarchie: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Erreur lors de la création de la hiérarchie: " + e.getMessage());
            result.getErrors().add(e.getMessage());}
        return result;
    }
   // Crée ou récupère un produit
    private Produit createOrRetrieveProduit(String nom, Map<String, Object> data) {
        // Rechercher d'abord si le produit existe
        List<Produit> existingProduits = produitRepository.findByNomProduitContainingIgnoreCase(nom);
        if (!existingProduits.isEmpty()) {
            logger.info("Produit existant trouvé: {}", existingProduits.get(0).getNomProduit());
            return existingProduits.get(0);
        }
        // Créer un nouveau produit
        Produit produit = new Produit();
        produit.setNomProduit(nom);
        produit.setDescription((String) data.getOrDefault("description", "Produit créé par le chatbot"));
        produit.setStatut(Statut.ACTIF); // Actif par défaut
        // Type produit si fourni
        if (data.containsKey("typeProduit")) {
            // Conversion du type si nécessaire
            produit.setTypeProduit(null); // À adapter selon l'enum
        }
        return produitRepository.save(produit);
    }
// Crée un pack pour un produit
    private Pack createPackForProduit(String nom, String produitId, Map<String, Object> data) {
        Pack pack = new Pack();
        pack.setNomPack(nom);
        pack.setDescription((String) data.getOrDefault("description", "Pack créé par le chatbot"));
        pack.setPrixMensuel((Double) data.getOrDefault("prixMensuel", 0.0));
        pack.setStatut(Statut.ACTIF); // Actif par défaut
        
        // Associer au produit
        Optional<Produit> produitOpt = produitRepository.findById(produitId);
        if (produitOpt.isPresent()) {
            pack.setProduitId(produitOpt.get().getIdProduit());
            pack.setNomProduit(produitOpt.get().getNomProduit());}
        return packUnifiedRepository.save(pack);}
// Obtient les statistiques des associations
    public Map<String, Object> getAssociationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalProduits", produitRepository.count());
        stats.put("totalPacks", packUnifiedRepository.count());
        stats.put("totalGaranties", garantieRepository.count());
        
        // Packs sans produit
        long packsSansProduit = packUnifiedRepository.findAll().stream()
            .filter(p -> p.getProduitId() == null || p.getProduitId().isEmpty())
            .count();
        stats.put("packsSansProduit", packsSansProduit);
        return stats;
    }
}