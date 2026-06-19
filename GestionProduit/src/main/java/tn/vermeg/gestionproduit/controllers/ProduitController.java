package tn.vermeg.gestionproduit.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.enums.Statut;
import tn.vermeg.gestionproduit.enums.TypeProduit;
import tn.vermeg.gestionproduit.services.ProduitService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/produits")
@CrossOrigin(origins = "http://localhost:4200")
public class ProduitController {

    @Autowired
    private ProduitService produitService;

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

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<Produit>> getProduitsByStatut(@PathVariable Statut statut) {
        return ResponseEntity.ok(produitService.getProduitsByStatut(statut));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Produit>> searchProduits(@RequestParam String nom) {
        return ResponseEntity.ok(produitService.searchProduits(nom));
    }

    // CREATE
    @PostMapping
    public ResponseEntity<Produit> createProduit(@RequestBody Produit produit) {
        try {
            Produit newProduit = produitService.createProduit(produit);
            return ResponseEntity.status(HttpStatus.CREATED).body(newProduit);
        } catch (IllegalArgumentException e) {
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

    @PatchMapping("/{idProduit}/desactiver")
    public ResponseEntity<Produit> desactiverProduit(@PathVariable String idProduit) {
        try {
            return ResponseEntity.ok(produitService.desactiverProduit(idProduit));
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
}