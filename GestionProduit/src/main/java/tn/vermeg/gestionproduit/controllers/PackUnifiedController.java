package tn.vermeg.gestionproduit.controllers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.logging.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.entities.Pack;
import tn.vermeg.gestionproduit.entities.PackGarantie;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.enums.CouvertureGeographique;
import tn.vermeg.gestionproduit.enums.NiveauCouverture;
import tn.vermeg.gestionproduit.enums.Statut;
import tn.vermeg.gestionproduit.enums.TypeClient;
import tn.vermeg.gestionproduit.enums.TypeMontant;
import tn.vermeg.gestionproduit.enums.TypePlafond;
import tn.vermeg.gestionproduit.enums.TypeProduit;
import tn.vermeg.gestionproduit.services.HierarchicalService;
import tn.vermeg.gestionproduit.services.PackUnifiedService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/packs")
@CrossOrigin(origins = {"http://localhost:4200"})
@Tag(name = "Gestion des Packs", description = "API pour la gestion complète des packs d'assurance")
public class PackUnifiedController {

    private static final Logger logger = Logger.getLogger(PackUnifiedController.class.getName());

    private final PackUnifiedService packUnifiedService;
    private final HierarchicalService hierarchicalService;

    public PackUnifiedController(PackUnifiedService packUnifiedService,
                                HierarchicalService hierarchicalService) {
        this.packUnifiedService = packUnifiedService;
        this.hierarchicalService = hierarchicalService;
    }

    @GetMapping
    @Operation(summary = "Récupérer tous les packs", description = "Retourne la liste complète de tous les packs d'assurance disponibles")
    public ResponseEntity<List<Pack>> getAllPacks() {
        return ResponseEntity.ok(packUnifiedService.getAllPacks());}

    @GetMapping("/search")
    @Operation(summary = "Rechercher des packs par nom", description = "Recherche des packs dont le nom contient la chaîne spécifiée")
    public ResponseEntity<List<Pack>> searchPacks(
            @Parameter(description = "Nom ou partie du nom du pack à rechercher", required = true)
            @RequestParam String nomPack) {
        return ResponseEntity.ok(packUnifiedService.searchPacksByNom(nomPack));
    }

    @GetMapping("/statut/{statut}")
    @Operation(summary = "Filtrer les packs par statut", description = "Récupère tous les packs ayant le statut spécifié (ACTIF, INACTIF)")
    public ResponseEntity<List<Pack>> getPacksByStatut(
            @Parameter(description = "Statut des packs (ACTIF, INACTIF)", required = true)
            @PathVariable Statut statut) {
        return ResponseEntity.ok(packUnifiedService.getPacksByStatut(statut));
    }

    @GetMapping("/niveau/{niveauCouverture}")
    @Operation(summary = "Filtrer les packs par niveau de couverture", description = "Récupère tous les packs ayant le niveau de couverture spécifié")
    public ResponseEntity<List<Pack>> getPacksByNiveau(
            @Parameter(description = "Niveau de couverture (BASIC, PREMIUM, GOLD)", required = true)
            @PathVariable NiveauCouverture niveauCouverture) {
        return ResponseEntity.ok(packUnifiedService.getPacksByNiveau(niveauCouverture));
    }

    @GetMapping("/type-client/{typeClient}")
    @Operation(summary = "Filtrer les packs par type de client", description = "Récupère tous les packs destinés au type de client spécifié")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Packs filtrés récupérés avec succès")
    })
    public ResponseEntity<List<Pack>> getPacksByTypeClient(
            @Parameter(description = "Type de client (INDIVIDUEL, FAMILIAL, SENIOR, ETUDIANT, PROFESSIONNEL)", required = true)
            @PathVariable TypeClient typeClient) {
        return ResponseEntity.ok(packUnifiedService.getPacksByTypeClient(typeClient));
    }

    @GetMapping("/prix-range")
    @Operation(summary = "Filtrer les packs par plage de prix", description = "Récupère tous les packs dont le prix mensuel est dans la plage spécifiée")
    public ResponseEntity<List<Pack>> getPacksByPrixRange(
            @Parameter(description = "Prix minimum mensuel", required = true)
            @RequestParam double prixMin,
            @Parameter(description = "Prix maximum mensuel", required = true)
            @RequestParam double prixMax) {
        return ResponseEntity.ok(packUnifiedService.getPacksByPrixRange(prixMin, prixMax));
    }

    @GetMapping("/by-produit/{produitId}")
    @Operation(summary = "Récupérer les packs par produit", description = "Récupère tous les packs associés à un produit spécifique")
    public ResponseEntity<List<Pack>> getPacksByProduitId(
            @Parameter(description = "ID du produit", required = true)
            @PathVariable String produitId) {
        return ResponseEntity.ok(packUnifiedService.getPacksByProduitId(produitId));
    }

    @GetMapping("/produit/{produitId}")
    @Operation(summary = "Récupérer les packs par produit (alias)", description = "Alias pour l'endpoint /by-produit/{produitId}")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Packs récupérés avec succès")
    })
    public ResponseEntity<List<Pack>> getPacksByProduitIdAlias(
            @Parameter(description = "ID du produit", required = true)
            @PathVariable String produitId) {
        return ResponseEntity.ok(packUnifiedService.getPacksByProduitId(produitId));
    }

    @GetMapping("/associations")
    @Operation(summary = "Récupérer toutes les associations pack-garantie", description = "Retourne la liste complète de toutes les associations entre packs et garanties")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Associations récupérées avec succès")
    })
    public ResponseEntity<List<PackGarantie>> getAllPackGaranties() {
        return ResponseEntity.ok(packUnifiedService.getAllPackGaranties());
    }

    @GetMapping("/statistics")
    @Operation(summary = "Récupérer les statistiques des packs", description = "Retourne des statistiques sur les packs (total, actifs, inactifs)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistiques récupérées avec succès")
    })
    public ResponseEntity<Map<String, Object>> getPackStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPacks", 0);
        stats.put("packsActifs", 0);
        stats.put("packsInactifs", 0);
        stats.put("message", "Statistiques à implémenter dans le service");
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check du service packs", description = "Vérifie l'état de santé du service de gestion des packs")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service opérationnel")
    })
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "Pack Unified Service");
        status.put("status", "UP");
        status.put("description", "Service unifié pour la gestion complète des packs, configurations et associations");
        status.put("version", "1.0.0");
        status.put("architecture", "Centralisée et Unifiée");
        status.put("timestamp", System.currentTimeMillis());
        
        Map<String, String> features = new HashMap<>();
        features.put("packManagement", "Gestion complète des packs (CRUD)");
        features.put("productAssociation", "Association packs-produits");
        features.put("garantieConfiguration", "Configuration des garanties dans les packs");
        features.put("advancedSearch", "Recherches et filtres avancés");
        features.put("statistics", "Statistiques et rapports");
        features.put("validation", "Validation robuste des données");
        status.put("features", features);
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("packs", "/api/packs");
        endpoints.put("associations", "/api/packs/associations");
        endpoints.put("search", "/api/packs/search");
        endpoints.put("statistics", "/api/packs/statistics");
        endpoints.put("health", "/api/packs/health");
        status.put("endpoints", endpoints);
        return ResponseEntity.ok(status);
    }

    @PostMapping
    @Operation(summary = "Créer un nouveau pack", description = "Crée un nouveau pack d'assurance avec les informations fournies")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Pack créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides ou conflit"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    public ResponseEntity<Pack> createPack(
            @Parameter(description = "Informations du pack à créer", required = true)
            @Valid @RequestBody Pack pack) {
        logger.info("POST /api/packs - Creating pack: " + pack.getNomPack());
        Pack createdPack = packUnifiedService.createPack(pack);
        logger.info("POST /api/packs - Pack created successfully with ID: " + createdPack.getIdPack());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPack);
    }

    //GARANTIES D'UN PACK
    @GetMapping("/{packId}/garanties")
    @Operation(summary = "Récupérer les garanties d'un pack", description = "Retourne toutes les garanties associées à un pack spécifique")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Garanties récupérées avec succès"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<List<PackGarantie>> getGarantiesByPackId(
            @Parameter(description = "ID du pack", required = true)
            @PathVariable String packId) {
        return ResponseEntity.ok(packUnifiedService.getGarantiesByPackId(packId));
    }

    @GetMapping("/{packId}/garanties/optionnelles")
    @Operation(summary = "Récupérer les garanties optionnelles d'un pack", description = "Retourne uniquement les garanties optionnelles d'un pack")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Garanties optionnelles récupérées avec succès"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<List<PackGarantie>> getGarantiesOptionnelles(
            @Parameter(description = "ID du pack", required = true)
            @PathVariable String packId) {
        return ResponseEntity.ok(packUnifiedService.getGarantiesOptionnellesByPackId(packId));
    }

    @GetMapping("/{packId}/garanties/obligatoires")
    @Operation(summary = "Récupérer les garanties obligatoires d'un pack", description = "Retourne uniquement les garanties obligatoires d'un pack")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Garanties obligatoires récupérées avec succès"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<List<PackGarantie>> getGarantiesObligatoires(
            @Parameter(description = "ID du pack", required = true)
            @PathVariable String packId) {
        return ResponseEntity.ok(packUnifiedService.getGarantiesObligatoiresByPackId(packId));
    }

    @GetMapping("/{packId}/garanties/incluses")
    @Operation(summary = "Récupérer les garanties incluses d'un pack (alias)", description = "Alias pour l'endpoint des garanties obligatoires")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Garanties incluses récupérées avec succès")
    })
    public ResponseEntity<List<PackGarantie>> getGarantiesIncluses(
            @Parameter(description = "ID du pack", required = true)
            @PathVariable String packId) {
        return getGarantiesObligatoires(packId);
    }

    @GetMapping("/{packId}/garanties-disponibles")
    @Operation(summary = "Récupérer les garanties disponibles pour un pack", description = "Retourne toutes les garanties qui peuvent être ajoutées à un pack")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Garanties disponibles récupérées avec succès"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<List<Garantie>> getGarantiesDisponiblesPourPack(
            @Parameter(description = "ID du pack", required = true)
            @PathVariable String packId) {
        return ResponseEntity.ok(packUnifiedService.getGarantiesDisponiblesPourPack(packId));
    }

    @GetMapping("/{packId}/prix-total")
    @Operation(summary = "Calculer le prix total d'un pack", description = "Calcule le prix total d'un pack en incluant toutes ses garanties")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prix total calculé avec succès"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<Double> calculerPrixTotalPack(
            @Parameter(description = "ID du pack", required = true)
            @PathVariable String packId) {
        return ResponseEntity.ok(packUnifiedService.calculerPrixTotalPack(packId));
    }

    @PostMapping("/{packId}/garanties/{garantieId}")
    @Operation(summary = "Ajouter une garantie à un pack", description = "Associe une garantie à un pack avec les paramètres spécifiés")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Garantie ajoutée avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "404", description = "Pack ou garantie non trouvé")
    })
    public ResponseEntity<PackGarantie> ajouterGarantieAuPack(
            @Parameter(description = "ID du pack", required = true)
            @PathVariable String packId,
            @Parameter(description = "ID de la garantie", required = true)
            @PathVariable String garantieId,
            @Parameter(description = "Configuration de l'association pack-garantie", required = true)
            @Valid @RequestBody PackGarantie packGarantie) {
        PackGarantie association = packUnifiedService.ajouterGarantieAuPack(packId, garantieId, packGarantie);
        return ResponseEntity.status(HttpStatus.CREATED).body(association);
    }

    @PostMapping("/{packId}/associate-produit/{produitId}")
    @Operation(summary = "Associer un pack à un produit", description = "Associe un pack à un produit d'assurance")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Association créée avec succès"),
        @ApiResponse(responseCode = "404", description = "Pack ou produit non trouvé")
    })
    public ResponseEntity<Pack> associatePackToProduit(
            @Parameter(description = "ID du pack", required = true)
            @PathVariable String packId,
            @Parameter(description = "ID du produit", required = true)
            @PathVariable String produitId) {
        return ResponseEntity.ok(packUnifiedService.associatePackToProduit(packId, produitId));
    }

    @DeleteMapping("/{packId}/dissociate-produit")
    @Operation(summary = "Dissocier un pack d'un produit", description = "Supprime l'association entre un pack et son produit")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Association supprimée avec succès"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<Pack> dissociatePackFromProduit(
            @Parameter(description = "ID du pack", required = true)
            @PathVariable String packId) {
        return ResponseEntity.ok(packUnifiedService.dissociatePackFromProduit(packId));
    }

    @PutMapping("/{idPack}")
    @Operation(summary = "Mettre à jour un pack", description = "Met à jour les informations d'un pack existant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pack mis à jour avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides ou conflit"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<Pack> updatePack(
            @Parameter(description = "ID du pack à mettre à jour", required = true)
            @PathVariable String idPack,
            @Parameter(description = "Nouvelles informations du pack", required = true)
            @Valid @RequestBody Pack pack) {
        return ResponseEntity.ok(packUnifiedService.updatePack(idPack, pack));
    }

    @DeleteMapping("/{idPack}")
    @Operation(summary = "Supprimer un pack", description = "Supprime un pack d'assurance")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Pack supprimé avec succès"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé"),
        @ApiResponse(responseCode = "400", description = "Impossible de supprimer (garanties associées)")
    })
    public ResponseEntity<Void> deletePack(
            @Parameter(description = "ID du pack à supprimer", required = true)
            @PathVariable String idPack) {
        packUnifiedService.deletePack(idPack);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idPack}/desactiver")
    @Operation(summary = "Désactiver un pack", description = "Désactive un pack sans le supprimer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pack désactivé avec succès"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<Pack> desactiverPack(
            @Parameter(description = "ID du pack à désactiver", required = true)
            @PathVariable String idPack) {
        return ResponseEntity.ok(packUnifiedService.desactiverPack(idPack));
    }

    @PutMapping("/associations/{id}")
    @Operation(summary = "Mettre à jour une association pack-garantie", description = "Met à jour les paramètres d'une association pack-garantie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Association mise à jour avec succès"),
        @ApiResponse(responseCode = "404", description = "Association non trouvée")
    })
    public ResponseEntity<PackGarantie> updatePackGarantie(
            @Parameter(description = "ID de l'association", required = true)
            @PathVariable String id,
            @Parameter(description = "Nouveaux paramètres de l'association", required = true)
            @Valid @RequestBody PackGarantie packGarantie) {
        return ResponseEntity.ok(packUnifiedService.updatePackGarantie(id, packGarantie));
    }

    @DeleteMapping("/associations/{id}")
    @Operation(summary = "Supprimer une association pack-garantie", description = "Supprime l'association entre un pack et une garantie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Association supprimée avec succès"),
        @ApiResponse(responseCode = "404", description = "Association non trouvée")
    })
    public ResponseEntity<Void> supprimerGarantieDuPack(
            @Parameter(description = "ID de l'association", required = true)
            @PathVariable String id) {
        packUnifiedService.supprimerGarantieDuPack(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/associations/{id}/activation")
    @Operation(summary = "Activer/désactiver une garantie dans un pack", description = "Change le statut d'activation d'une garantie dans un pack")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statut d'activation mis à jour avec succès"),
        @ApiResponse(responseCode = "404", description = "Association non trouvée")
    })
    public ResponseEntity<PackGarantie> toggleGarantieActivation(
            @Parameter(description = "ID de l'association", required = true)
            @PathVariable String id,
            @Parameter(description = "Nouveau statut d'activation", required = true)
            @RequestParam boolean active) {
        return ResponseEntity.ok(packUnifiedService.toggleGarantieActivation(id, active));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un pack par ID", description = "Retourne les détails d'un pack spécifique")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pack récupéré avec succès"),
        @ApiResponse(responseCode = "404", description = "Pack non trouvé")
    })
    public ResponseEntity<Pack> getPackById(
            @Parameter(description = "ID du pack", required = true)
            @PathVariable String id) {
        return ResponseEntity.ok(packUnifiedService.getPackById(id));
    }

    // ==================== ENDPOINTS HIÉRARCHIQUES ====================

    /**
     * Récupère un pack avec toutes ses garanties associées
     * GET /api/packs/{id}/with-garanties
     */
    @GetMapping("/{id}/with-garanties")
    @Operation(summary = "Récupérer un pack avec ses garanties", description = "Retourne un pack avec toutes ses garanties associées")
    public ResponseEntity<Pack> getPackWithGaranties(
            @Parameter(description = "ID du pack", required = true)
            @PathVariable String id) {
        try {
            Pack pack = hierarchicalService.getPackWithGaranties(id);
            return ResponseEntity.ok(pack);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Récupère tous les packs avec leurs garanties associées
     * GET /api/packs/with-garanties
     */
    @GetMapping("/with-garanties")
    @Operation(summary = "Récupérer tous les packs avec leurs garanties", description = "Retourne tous les packs avec leurs garanties associées")
    public ResponseEntity<List<Pack>> getAllPacksWithGaranties() {
        List<Pack> packs = hierarchicalService.getAllPacksWithGaranties();
        return ResponseEntity.ok(packs);
    }
}