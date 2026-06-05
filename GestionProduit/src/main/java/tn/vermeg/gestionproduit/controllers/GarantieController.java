package tn.vermeg.gestionproduit.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.entities.Statut;
import tn.vermeg.gestionproduit.exceptions.ApiException;
import tn.vermeg.gestionproduit.exceptions.ResourceException;
import tn.vermeg.gestionproduit.services.GarantieService;
import tn.vermeg.gestionproduit.dto.GarantieDTO;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/garanties")
@CrossOrigin(origins = "http://localhost:4200")
public class GarantieController {

    @Autowired
    private GarantieService garantieService;
    // READ
    @GetMapping
    public ResponseEntity<List<Garantie>> getAllGaranties() {
        return ResponseEntity.ok(garantieService.getAllGaranties());
    }

    @GetMapping("/{idGarantie}")
    public ResponseEntity<Garantie> getGarantieById(@PathVariable String idGarantie) {
        return ResponseEntity.ok(garantieService.getGarantieById(idGarantie));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Garantie>> getGarantiesByType(
            @PathVariable String type) {
        return ResponseEntity.ok(garantieService.getGarantiesByType(type));
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<Garantie>> getGarantiesByStatut(@PathVariable Statut statut) {
        return ResponseEntity.ok(garantieService.getGarantiesByStatut(statut));
    }

    @GetMapping("/domain")
    public ResponseEntity<java.util.Map<String, List<Garantie>>> getGarantiesByDomain() {
        return ResponseEntity.ok(garantieService.getGarantiesByDomain());
    }

    @GetMapping("/domain/{domaine}")
    public ResponseEntity<List<Garantie>> getGarantiesByDomainName(@PathVariable String domaine) {
        return ResponseEntity.ok(garantieService.getGarantiesByDomainName(domaine));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Garantie>> searchGaranties(@RequestParam String nomGarantie) {
        return ResponseEntity.ok(garantieService.searchGaranties(nomGarantie));
    }

    // Reference: types de garanties sante disponibles avec leur domaine medical
    @GetMapping("/types")
    public ResponseEntity<List<java.util.Map<String, String>>> getTypesGarantie() {
        List<java.util.Map<String, String>> types = java.util.Arrays
                .stream(tn.vermeg.gestionproduit.entities.TypeGarantie.values())
                .map(t -> java.util.Map.of(
                        "code", t.name(),
                        "libelle", t.getLibelle(),
                        "domaine", t.getDomaine().name(),
                        "domaineLibelle", t.getDomaine().getLibelle()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(types);
    }

    @GetMapping("/taux-min/{tauxMin}")
    public ResponseEntity<List<Garantie>> getGarantiesByTauxMin(
            @PathVariable double tauxMin) {
        try {
            return ResponseEntity.ok(
                    garantieService.getGarantiesByTauxRemboursementMin(tauxMin));
        } catch (ResourceException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/plafond-min/{plafondMin}")
    public ResponseEntity<List<Garantie>> getGarantiesByPlafondMin(
            @PathVariable double plafondMin) {
        try {
            return ResponseEntity.ok(
                    garantieService.getGarantiesByPlafondMin(plafondMin));
        } catch (ResourceException e) {
            return ResponseEntity.badRequest().build();
        }
    }

     // Crée une nouvelle garantie avec validation robuste
    @PostMapping
    public ResponseEntity<Garantie> createGarantie(@Valid @RequestBody GarantieDTO dto) {
        try {
            Garantie g = new Garantie();
            g.setNomGarantie(dto.getNomGarantie());
            g.setDescription(dto.getDescription());
            g.setStatut(dto.getStatut());
            g.setType(dto.getType());
            g.setTauxRemboursement(dto.getTauxRemboursement());
            g.setTypeMontant(dto.getTypeMontant());
            g.setTypePlafond(dto.getTypePlafond());
            g.setPlafondAnnuel(dto.getPlafondAnnuel());
            g.setPlafondMensuel(dto.getPlafondMensuel());
            g.setPlafondParActe(dto.getPlafondParActe());
            g.setFranchise(dto.getFranchise());
            g.setCoutMoyenParSinistre(dto.getCoutMoyenParSinistre());
            g.setDureeMinContrat(dto.getDureeMinContrat());
            g.setDureeMaxContrat(dto.getDureeMaxContrat());
            g.setResiliableAnnuellement(dto.isResiliableAnnuellement());
            if (dto.getCustomFields() != null) {
                g.setCustomFields(dto.getCustomFields());
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(garantieService.createGarantie(g));
        } catch (ResourceException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // UPDATE avec validation
    @PutMapping("/{idGarantie}")
    public ResponseEntity<Garantie> updateGarantie(
            @PathVariable String idGarantie,
            @Valid @RequestBody GarantieDTO dto) {
        try {
            Garantie details = new Garantie();
            details.setNomGarantie(dto.getNomGarantie());
            details.setDescription(dto.getDescription());
            details.setType(dto.getType());
            details.setStatut(dto.getStatut());
            details.setTauxRemboursement(dto.getTauxRemboursement());
            details.setTypeMontant(dto.getTypeMontant());
            details.setTypePlafond(dto.getTypePlafond());
            details.setPlafondAnnuel(dto.getPlafondAnnuel());
            details.setPlafondMensuel(dto.getPlafondMensuel());
            details.setPlafondParActe(dto.getPlafondParActe());
            details.setFranchise(dto.getFranchise());
            details.setCoutMoyenParSinistre(dto.getCoutMoyenParSinistre());
            details.setDureeMinContrat(dto.getDureeMinContrat());
            details.setDureeMaxContrat(dto.getDureeMaxContrat());
            details.setResiliableAnnuellement(dto.isResiliableAnnuellement());
            details.setCustomFields(dto.getCustomFields());
            return ResponseEntity.ok(
                    garantieService.updateGarantie(idGarantie, details));
        } catch (ResourceException e) {
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
        } catch (ResourceException e) {
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
        } catch (ResourceException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
