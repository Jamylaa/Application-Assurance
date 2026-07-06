package tn.vermeg.gestionproduit.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.vermeg.gestionproduit.entities.CritereRecommandation;
import tn.vermeg.gestionproduit.exceptions.ResourceNotFoundException;
import tn.vermeg.gestionproduit.services.CritereRecommandationService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/criteres-recommandation")
@CrossOrigin(origins = "http://localhost:4200")
public class CritereRecommandationController {

    @Autowired
    private CritereRecommandationService critereRecommandationService;

    @GetMapping
    public ResponseEntity<List<CritereRecommandation>> getAllCriteres() {
        return ResponseEntity.ok(critereRecommandationService.getAllCriteres());
    }

    @GetMapping("/{idCritere}")
    public ResponseEntity<CritereRecommandation> getCritereById(@PathVariable String idCritere) {
        try {
            return ResponseEntity.ok(critereRecommandationService.getCritereById(idCritere));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/produit/{produitId}")
    public ResponseEntity<List<CritereRecommandation>> getCriteresByProduitId(@PathVariable String produitId) {
        return ResponseEntity.ok(critereRecommandationService.getCriteresByProduitId(produitId));
    }

    @GetMapping("/pack/{packId}")
    public ResponseEntity<CritereRecommandation> getCritereByPackId(@PathVariable String packId) {
        try {
            return ResponseEntity.ok(critereRecommandationService.getCritereByPackId(packId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/actifs")
    public ResponseEntity<List<CritereRecommandation>> getCriteresActifs() {
        return ResponseEntity.ok(critereRecommandationService.getCriteresActifs());
    }

    @GetMapping("/search")
    public ResponseEntity<List<CritereRecommandation>> searchByMotsCles(@RequestParam List<String> motCle) {
        return ResponseEntity.ok(critereRecommandationService.searchByMotsCles(motCle));
    }

    /** Calcule le score de recommandation de ce critère pour un profil client donné. */
    @PostMapping("/{idCritere}/score")
    public ResponseEntity<Map<String, Double>> calculerScore(
            @PathVariable String idCritere,
            @RequestBody Map<String, Double> attributsProfil) {
        try {
            return ResponseEntity.ok(Map.of("score", critereRecommandationService.calculerScore(idCritere, attributsProfil)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<CritereRecommandation> createCritere(@Valid @RequestBody CritereRecommandation critere) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(critereRecommandationService.createCritere(critere));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{idCritere}")
    public ResponseEntity<CritereRecommandation> updateCritere(
            @PathVariable String idCritere,
            @Valid @RequestBody CritereRecommandation details) {
        try {
            return ResponseEntity.ok(critereRecommandationService.updateCritere(idCritere, details));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{idCritere}")
    public ResponseEntity<Void> deleteCritere(@PathVariable String idCritere) {
        try {
            critereRecommandationService.deleteCritere(idCritere);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
