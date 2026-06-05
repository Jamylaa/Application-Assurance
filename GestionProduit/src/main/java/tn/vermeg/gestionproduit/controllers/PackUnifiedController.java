package tn.vermeg.gestionproduit.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.vermeg.gestionproduit.entities.*;
import tn.vermeg.gestionproduit.dto.PackDTO;
import tn.vermeg.gestionproduit.dto.PackGarantieDTO;
import tn.vermeg.gestionproduit.services.PackUnifiedService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/packs")
@CrossOrigin(origins = {"http://localhost:4200"})
public class PackUnifiedController {

    private final PackUnifiedService packUnifiedService;

    public PackUnifiedController(PackUnifiedService packUnifiedService) {
        this.packUnifiedService = packUnifiedService;
    }

    @GetMapping
    public ResponseEntity<List<Pack>> getAllPacks() {
        return ResponseEntity.ok(packUnifiedService.getAllPacks());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Pack>> searchPacks(@RequestParam String nomPack) {
        return ResponseEntity.ok(packUnifiedService.searchPacksByNom(nomPack));
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<Pack>> getPacksByStatut(@PathVariable Statut statut) {
        return ResponseEntity.ok(packUnifiedService.getPacksByStatut(statut));
    }

    @GetMapping("/niveau/{niveauCouverture}")
    public ResponseEntity<List<Pack>> getPacksByNiveau(@PathVariable NiveauCouverture niveauCouverture) {
        return ResponseEntity.ok(packUnifiedService.getPacksByNiveau(niveauCouverture));
    }

    @GetMapping("/type-client/{typeClient}")
    public ResponseEntity<List<Pack>> getPacksByTypeClient(@PathVariable TypeClient typeClient) {
        return ResponseEntity.ok(packUnifiedService.getPacksByTypeClient(typeClient));
    }

    @GetMapping("/prix-range")
    public ResponseEntity<List<Pack>> getPacksByPrixRange(
            @RequestParam double prixMin,
            @RequestParam double prixMax) {
        return ResponseEntity.ok(packUnifiedService.getPacksByPrixRange(prixMin, prixMax));
    }

    @GetMapping("/by-produit/{produitId}")
    public ResponseEntity<List<Pack>> getPacksByProduitId(@PathVariable String produitId) {
        return ResponseEntity.ok(packUnifiedService.getPacksByProduitId(produitId));
    }

    @GetMapping("/produit/{produitId}")
    public ResponseEntity<List<Pack>> getPacksByProduitIdAlias(@PathVariable String produitId) {
        return ResponseEntity.ok(packUnifiedService.getPacksByProduitId(produitId));
    }

    @GetMapping("/associations")
    public ResponseEntity<List<PackGarantie>> getAllPackGaranties() {
        return ResponseEntity.ok(packUnifiedService.getAllPackGaranties());
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getPackStatistics() {
        Map<String, Object> stats = Map.of(
            "totalPacks", 0,
            "packsActifs", 0,
            "packsInactifs", 0,
            "message", "Statistiques à implémenter dans le service"
        );
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = Map.of(
            "service", "Pack Unified Service",
            "status", "UP",
            "description", "Service unifié pour la gestion complète des packs, configurations et associations",
            "version", "1.0.0",
            "architecture", "Centralisée et Unifiée",
            "timestamp", System.currentTimeMillis(),
            "features", Map.of(
                "packManagement", "Gestion complète des packs (CRUD)",
                "productAssociation", "Association packs-produits",
                "garantieConfiguration", "Configuration des garanties dans les packs",
                "advancedSearch", "Recherches et filtres avancés",
                "statistics", "Statistiques et rapports",
                "validation", "Validation robuste des données"
            ),
            "endpoints", Map.of(
                "packs", "/api/packs",
                "associations", "/api/packs/associations",
                "search", "/api/packs/search",
                "statistics", "/api/packs/statistics",
                "health", "/api/packs/health"
            )
        );
        return ResponseEntity.ok(status);
    }

    @PostMapping
    public ResponseEntity<Pack> createPack(@Valid @RequestBody PackDTO dto) {
        Pack pack = new Pack();
        pack.setNomPack(dto.getNomPack());
        pack.setDescription(dto.getDescription());
        pack.setProduitId(dto.getProduitId());
        pack.setNomProduit(dto.getNomProduit());
        pack.setCustomFields(dto.getCustomFields());
        pack.setAgeMinimum(dto.getAgeMinimum());
        pack.setAgeMaximum(dto.getAgeMaximum());
        pack.setTypeClients(dto.getTypeClients());
        pack.setAncienneteContratMois(dto.getAncienneteContratMois());
        pack.setCouvertureGeographique(dto.getCouvertureGeographique());
        pack.setPrixMensuel(dto.getPrixMensuel());
        pack.setDureeMinContrat(dto.getDureeMinContrat());
        pack.setDureeMaxContrat(dto.getDureeMaxContrat());
        pack.setNiveauCouverture(dto.getNiveauCouverture());
        pack.setStatut(dto.getStatut());
        Pack createdPack = packUnifiedService.createPack(pack);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPack);
    }

    //GARANTIES D'UN PACK (avant /{id} mono-segment)
    @GetMapping("/{packId}/garanties")
    public ResponseEntity<List<PackGarantie>> getGarantiesByPackId(@PathVariable String packId) {
        return ResponseEntity.ok(packUnifiedService.getGarantiesByPackId(packId));
    }

    @GetMapping("/{packId}/garanties/optionnelles")
    public ResponseEntity<List<PackGarantie>> getGarantiesOptionnelles(@PathVariable String packId) {
        return ResponseEntity.ok(packUnifiedService.getGarantiesOptionnellesByPackId(packId));
    }

    @GetMapping("/{packId}/garanties/obligatoires")
    public ResponseEntity<List<PackGarantie>> getGarantiesObligatoires(@PathVariable String packId) {
        return ResponseEntity.ok(packUnifiedService.getGarantiesObligatoiresByPackId(packId));
    }

    @GetMapping("/{packId}/garanties/incluses")
    public ResponseEntity<List<PackGarantie>> getGarantiesIncluses(@PathVariable String packId) {
        return getGarantiesObligatoires(packId);
    }

    @GetMapping("/{packId}/garanties-disponibles")
    public ResponseEntity<List<Garantie>> getGarantiesDisponiblesPourPack(@PathVariable String packId) {
        return ResponseEntity.ok(packUnifiedService.getGarantiesDisponiblesPourPack(packId));
    }

    @GetMapping("/{packId}/prix-total")
    public ResponseEntity<Double> calculerPrixTotalPack(@PathVariable String packId) {
        return ResponseEntity.ok(packUnifiedService.calculerPrixTotalPack(packId));
    }

    @PostMapping("/{packId}/garanties/{garantieId}")
    public ResponseEntity<PackGarantie> ajouterGarantieAuPack(
            @PathVariable String packId,
            @PathVariable String garantieId,
            @Valid @RequestBody PackGarantieDTO dto) {
        PackGarantie packGarantie = new PackGarantie();
        packGarantie.setTauxRemboursement(dto.getTauxRemboursement());
        packGarantie.setPlafond(dto.getPlafond());
        packGarantie.setFranchise(dto.getFranchise());
        packGarantie.setTypeMontant(dto.getTypeMontant());
        packGarantie.setDelaiCarence(dto.getDelaiCarence());
        packGarantie.setPriorite(dto.getPriorite());
        packGarantie.setActif(dto.isActif());
        packGarantie.setOptionnelle(dto.isOptionnelle());
        packGarantie.setSupplementPrix(dto.getSupplementPrix());
        packGarantie.setCustomFields(dto.getCustomFields());
        PackGarantie association = packUnifiedService.ajouterGarantieAuPack(packId, garantieId, packGarantie);
        return ResponseEntity.status(HttpStatus.CREATED).body(association);
    }

    @PostMapping("/{packId}/associate-produit/{produitId}")
    public ResponseEntity<Pack> associatePackToProduit(@PathVariable String packId, @PathVariable String produitId) {
        return ResponseEntity.ok(packUnifiedService.associatePackToProduit(packId, produitId));
    }

    @DeleteMapping("/{packId}/dissociate-produit")
    public ResponseEntity<Pack> dissociatePackFromProduit(@PathVariable String packId) {
        return ResponseEntity.ok(packUnifiedService.dissociatePackFromProduit(packId));
    }

    @PutMapping("/{idPack}")
    public ResponseEntity<Pack> updatePack(@PathVariable String idPack, @Valid @RequestBody PackDTO dto) {
        Pack pack = new Pack();
        pack.setNomPack(dto.getNomPack());
        pack.setDescription(dto.getDescription());
        pack.setProduitId(dto.getProduitId());
        pack.setNomProduit(dto.getNomProduit());
        pack.setCustomFields(dto.getCustomFields());
        pack.setAgeMinimum(dto.getAgeMinimum());
        pack.setAgeMaximum(dto.getAgeMaximum());
        pack.setTypeClients(dto.getTypeClients());
        pack.setAncienneteContratMois(dto.getAncienneteContratMois());
        pack.setCouvertureGeographique(dto.getCouvertureGeographique());
        pack.setPrixMensuel(dto.getPrixMensuel());
        pack.setDureeMinContrat(dto.getDureeMinContrat());
        pack.setDureeMaxContrat(dto.getDureeMaxContrat());
        pack.setNiveauCouverture(dto.getNiveauCouverture());
        pack.setStatut(dto.getStatut());
        return ResponseEntity.ok(packUnifiedService.updatePack(idPack, pack));
    }

    @DeleteMapping("/{idPack}")
    public ResponseEntity<Void> deletePack(@PathVariable String idPack) {
        packUnifiedService.deletePack(idPack);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idPack}/desactiver")
    public ResponseEntity<Pack> desactiverPack(@PathVariable String idPack) {
        return ResponseEntity.ok(packUnifiedService.desactiverPack(idPack));
    }

    @PutMapping("/associations/{id}")
    public ResponseEntity<PackGarantie> updatePackGarantie(@PathVariable String id, @Valid @RequestBody PackGarantie packGarantie) {
        return ResponseEntity.ok(packUnifiedService.updatePackGarantie(id, packGarantie));
    }

    @DeleteMapping("/associations/{id}")
    public ResponseEntity<Void> supprimerGarantieDuPack(@PathVariable String id) {
        packUnifiedService.supprimerGarantieDuPack(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/associations/{id}/activation")
    public ResponseEntity<PackGarantie> toggleGarantieActivation(@PathVariable String id, @RequestParam boolean active) {
        return ResponseEntity.ok(packUnifiedService.toggleGarantieActivation(id, active));
    }
    @GetMapping("/{id}")
    public ResponseEntity<Pack> getPackById(@PathVariable String id) {
        return ResponseEntity.ok(packUnifiedService.getPackById(id));
    }

    @GetMapping("/{idPack}/details")
    public ResponseEntity<java.util.Map<String, Object>> getPackDetails(@PathVariable String idPack) {
        Pack pack = packUnifiedService.getPackById(idPack);
        java.util.List<PackGarantie> garanties = packUnifiedService.getGarantiesByPackId(idPack);
        double prixTotal = packUnifiedService.calculerPrixTotalPack(idPack);
        return ResponseEntity.ok(java.util.Map.of(
                "pack", pack,
                "garanties", garanties,
                "prixTotal", prixTotal
        ));
    }
}