package tn.vermeg.gestionproduit.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.vermeg.gestionproduit.dto.CreationProduitCompletRequest;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.services.ProduitCompletService;

@RestController
@RequestMapping("/api/produits-complets")
@CrossOrigin(origins = "http://localhost:4200")
public class ProduitCompletController {

    @Autowired
    private ProduitCompletService produitCompletService;

    /**
     * Crée un produit complet (produit + packs + garanties) en une seule opération.
     * En cas d'échec à mi-parcours, toutes les entités déjà créées sont supprimées.
     */
    @PostMapping
    public ResponseEntity<Produit> creerProduitComplet(@RequestBody CreationProduitCompletRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(produitCompletService.creerProduitComplet(request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
