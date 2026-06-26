package tn.vermeg.gestionproduit.controllers;

import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.enums.Statut;
import tn.vermeg.gestionproduit.enums.DomaineMedical;
import tn.vermeg.gestionproduit.services.GarantieService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/garanties")
@CrossOrigin(origins = "http://localhost:4200")
public class GarantieController {

    private static final Logger logger = Logger.getLogger(GarantieController.class.getName());

    @Autowired
    private GarantieService garantieService;
    // READ
    @GetMapping
    public ResponseEntity<List<Garantie>> getAllGaranties() {
        return ResponseEntity.ok(garantieService.getAllGaranties());}
    @GetMapping("/{idGarantie}")
    public ResponseEntity<Garantie> getGarantieById(@PathVariable String idGarantie) {
        try {return ResponseEntity.ok(garantieService.getGarantieById(idGarantie));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {return ResponseEntity.notFound().build();}
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<Garantie>> getGarantiesByStatut(@PathVariable Statut statut) {
        return ResponseEntity.ok(garantieService.getGarantiesByStatut(statut));}

    @GetMapping("/search")
    public ResponseEntity<List<Garantie>> searchGaranties(@RequestParam String nomGarantie) {
        return ResponseEntity.ok(garantieService.searchGaranties(nomGarantie));
    }

    @GetMapping("/taux-min/{tauxMin}")
    public ResponseEntity<List<Garantie>> getGarantiesByTauxMin(
            @PathVariable double tauxMin) {
        try {return ResponseEntity.ok(
                    garantieService.getGarantiesByTauxRemboursementMin(tauxMin));
        } catch (IllegalArgumentException e) {return ResponseEntity.badRequest().build();}
    }

    @GetMapping("/plafond-min/{plafondMin}")
    public ResponseEntity<List<Garantie>> getGarantiesByPlafondMin(
            @PathVariable double plafondMin) {
        try {
            return ResponseEntity.ok(
                    garantieService.getGarantiesByPlafondMin(plafondMin));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    @GetMapping("/domaine/{domaine}")
    public ResponseEntity<List<Garantie>> getGarantiesByDomaine(
            @PathVariable String domaine) {
        try {
            DomaineMedical domaineEnum = DomaineMedical.fromString(domaine);
            if (domaineEnum == null) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(garantieService.getGarantiesByDomaine(domaineEnum));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<Garantie> createGarantie(@Valid @RequestBody Garantie garantie) {
        logger.info("POST /api/garanties - Creating guarantee: " + garantie.getNomGarantie());
        try {
            Garantie createdGarantie = garantieService.createGarantie(garantie);
            logger.info("POST /api/garanties - Guarantee created successfully with ID: " + createdGarantie.getIdGarantie());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(createdGarantie);
        } catch (IllegalArgumentException e) {
            logger.severe("POST /api/garanties - Failed to create guarantee: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.severe("POST /api/garanties - Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // UPDATE avec validation
    @PutMapping("/{idGarantie}")
    public ResponseEntity<Garantie> updateGarantie(
            @PathVariable String idGarantie,
            @Valid @RequestBody Garantie garantieDetails) {
        try {
            return ResponseEntity.ok(
                    garantieService.updateGarantie(idGarantie, garantieDetails));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

  //   Désactive une garantie
    @PatchMapping("/{idGarantie}/desactiver")
    public ResponseEntity<Garantie> desactiverGarantie(@PathVariable String idGarantie) {
        try {
            return ResponseEntity.ok(garantieService.desactiverGarantie(idGarantie));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE
    @DeleteMapping("/{idGarantie}")
    public ResponseEntity<Void> deleteGarantie(@PathVariable String idGarantie) {
        try {
            garantieService.deleteGarantie(idGarantie);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {return ResponseEntity.notFound().build();
        }
    }
}

//@GetMapping("/domaine/{domaine}/statut/{statut}")
//public ResponseEntity<List<Garantie>> getGarantiesByDomaineAndStatut(
//        @PathVariable String domaine,
//        @PathVariable Statut statut) {
//    try {
//        DomaineMedical domaineEnum = DomaineMedical.fromString(domaine);
//        if (domaineEnum == null) {
//            return ResponseEntity.badRequest().build();
//        }
//        return ResponseEntity.ok(garantieService.getGarantiesByDomaineAndStatut(domaineEnum, statut));
//    } catch (IllegalArgumentException e) {
//        return ResponseEntity.badRequest().build();
 //   }
//}
//@GetMapping("/domaine/{domaine}/actives")
//    public ResponseEntity<List<Garantie>> getActiveGarantiesByDomaine(
//            @PathVariable DomaineMedical domain) {
//        try {DomaineMedical domaine = DomaineMedical.fromString(domaine);
//            if (domain == null) {
//                return ResponseEntity.badRequest().build();
//            }
//            return ResponseEntity.ok(garantieService.getActiveGarantiesByDomaine(domain));
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }