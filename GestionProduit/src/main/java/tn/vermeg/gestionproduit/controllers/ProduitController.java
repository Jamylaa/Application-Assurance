package tn.vermeg.gestionproduit.controllers;

import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.vermeg.gestionproduit.dto.ProduitDetailDTO;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.enums.TypeProduit;
import tn.vermeg.gestionproduit.exceptions.ResourceNotFoundException;
import tn.vermeg.gestionproduit.services.HierarchicalService;
import tn.vermeg.gestionproduit.services.ProduitService;

import java.util.List;

@RestController
@RequestMapping("/api/produits")
@CrossOrigin(origins = "http://localhost:4200")
public class ProduitController {

    private static final Logger logger = Logger.getLogger(ProduitController.class.getName());

    @Autowired
    private ProduitService produitService;

    @Autowired
    private HierarchicalService hierarchicalService;

    // READ
    @GetMapping
    public ResponseEntity<List<Produit>> getAllProduits() {
        return ResponseEntity.ok(produitService.getAllProduits());
    }

    @GetMapping("/{idProduit}")
    public ResponseEntity<Produit> getProduitById(@PathVariable String idProduit) {
        try {
            return ResponseEntity.ok(produitService.getProduitById(idProduit));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/type/{typeProduit}")
    public ResponseEntity<List<Produit>> getProduitsByType(@PathVariable TypeProduit typeProduit) {
        return ResponseEntity.ok(produitService.getProduitsByType(typeProduit));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Produit>> searchProduits(@RequestParam String nom) {
        return ResponseEntity.ok(produitService.searchProduits(nom));
    }

    // CREATE
    @PostMapping
    public ResponseEntity<Produit> createProduit(@RequestBody Produit produit) {
        logger.info("POST /api/produits - Creating product: " + produit.getNomProduit());
        try {
            Produit newProduit = produitService.createProduit(produit);
            logger.info("POST /api/produits - Product created successfully with ID: " + newProduit.getIdProduit());
            return ResponseEntity.status(HttpStatus.CREATED).body(newProduit);
        } catch (IllegalArgumentException e) {
            logger.severe("POST /api/produits - Failed to create product: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // UPDATE
    @PutMapping("/{idProduit}")
    public ResponseEntity<Produit> updateProduit(
            @PathVariable String idProduit,
            @RequestBody Produit produitDetails) {
        try {
            return ResponseEntity.ok(produitService.updateProduit(idProduit, produitDetails));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Publie un produit (transition APPROUVE → PUBLIE).
     */
    @PostMapping("/{idProduit}/publier")
    public ResponseEntity<Produit> publierProduit(
            @PathVariable String idProduit,
            @RequestParam(defaultValue = "system") String utilisateur) {
        try {
            return ResponseEntity.ok(produitService.publierProduit(idProduit, utilisateur));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // DELETE
    @DeleteMapping("/{idProduit}")
    public ResponseEntity<Void> deleteProduit(@PathVariable String idProduit) {
        try {
            produitService.deleteProduit(idProduit);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== ENDPOINTS HIÉRARCHIQUES ====================

    /**
     * Récupère un produit avec tous ses packs associés
     * GET /api/produits/{idProduit}/with-packs
     */
    @GetMapping("/{idProduit}/with-packs")
    public ResponseEntity<Produit> getProduitWithPacks(@PathVariable String idProduit) {
        try {
            Produit produit = hierarchicalService.getProduitWithPacks(idProduit);
            return ResponseEntity.ok(produit);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Récupère un produit avec la hiérarchie complète (Produit → Packs → Garanties)
     * GET /api/produits/{idProduit}/full-hierarchy
     */
    @GetMapping("/{idProduit}/full-hierarchy")
    public ResponseEntity<Produit> getProduitWithFullHierarchy(@PathVariable String idProduit) {
        try {
            Produit produit = hierarchicalService.getProduitWithFullHierarchy(idProduit);
            return ResponseEntity.ok(produit);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Détail d'un produit avec idPacks calculé à la demande.
     * GET /api/produits/{idProduit}/detail
     */
    @GetMapping("/{idProduit}/detail")
    public ResponseEntity<ProduitDetailDTO> getProduitDetail(@PathVariable String idProduit) {
        try {
            return ResponseEntity.ok(hierarchicalService.getProduitDetail(idProduit));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}