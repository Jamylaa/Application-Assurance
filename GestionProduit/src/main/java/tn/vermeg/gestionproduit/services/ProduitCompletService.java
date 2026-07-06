package tn.vermeg.gestionproduit.services;

import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.dto.CreationProduitCompletRequest;
import tn.vermeg.gestionproduit.dto.GarantieAssociationRequest;
import tn.vermeg.gestionproduit.dto.PackAvecGarantiesRequest;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.entities.Pack;
import tn.vermeg.gestionproduit.entities.PackGarantie;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.repositories.GarantieRepository;
import tn.vermeg.gestionproduit.repositories.PackGarantieRepository;
import tn.vermeg.gestionproduit.repositories.PackUnifiedRepository;
import tn.vermeg.gestionproduit.repositories.ProduitRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Crée un produit complet (produit + packs + garanties par pack) en une seule opération.
 *
 * <p>MongoDB tourne ici en instance unique sans replica set : les transactions ACID
 * multi-documents ne sont pas disponibles. La création est donc séquentielle, avec
 * compensation manuelle (suppression des entités déjà créées) en cas d'échec à mi-parcours.
 */
@Service
public class ProduitCompletService {

    private final ProduitService produitService;
    private final PackUnifiedService packUnifiedService;
    private final GarantieService garantieService;
    private final HierarchicalService hierarchicalService;
    private final AssociationValidationService associationValidationService;
    private final ProduitRepository produitRepository;
    private final PackUnifiedRepository packUnifiedRepository;
    private final GarantieRepository garantieRepository;
    private final PackGarantieRepository packGarantieRepository;

    public ProduitCompletService(ProduitService produitService,
                                  PackUnifiedService packUnifiedService,
                                  GarantieService garantieService,
                                  HierarchicalService hierarchicalService,
                                  AssociationValidationService associationValidationService,
                                  ProduitRepository produitRepository,
                                  PackUnifiedRepository packUnifiedRepository,
                                  GarantieRepository garantieRepository,
                                  PackGarantieRepository packGarantieRepository) {
        this.produitService = produitService;
        this.packUnifiedService = packUnifiedService;
        this.garantieService = garantieService;
        this.hierarchicalService = hierarchicalService;
        this.associationValidationService = associationValidationService;
        this.produitRepository = produitRepository;
        this.packUnifiedRepository = packUnifiedRepository;
        this.garantieRepository = garantieRepository;
        this.packGarantieRepository = packGarantieRepository;
    }

    public Produit creerProduitComplet(CreationProduitCompletRequest request) {
        List<String> produitIdsCrees = new ArrayList<>();
        List<String> packIdsCrees = new ArrayList<>();
        List<String> garantieIdsCrees = new ArrayList<>();
        List<String> packGarantieIdsCrees = new ArrayList<>();

        try {
            Produit produit = produitService.createProduit(request.getProduit());
            produitIdsCrees.add(produit.getIdProduit());

            for (PackAvecGarantiesRequest packReq : orEmpty(request.getPacks())) {
                packReq.getPack().setProduitId(produit.getIdProduit());
                Pack pack = packUnifiedService.createPack(packReq.getPack());
                packIdsCrees.add(pack.getIdPack());

                for (GarantieAssociationRequest garantieReq : orEmpty(packReq.getGaranties())) {
                    String garantieId;
                    if (garantieReq.getGarantieIdExistante() != null && !garantieReq.getGarantieIdExistante().isBlank()) {
                        garantieId = associationValidationService
                                .validateGarantieExisteEtActive(garantieReq.getGarantieIdExistante())
                                .getIdGarantie();
                    } else {
                        Garantie garantie = garantieService.createGarantie(garantieReq.getGarantie());
                        garantieIdsCrees.add(garantie.getIdGarantie());
                        garantieId = garantie.getIdGarantie();
                    }

                    PackGarantie configuration = garantieReq.getConfiguration() != null
                            ? garantieReq.getConfiguration() : new PackGarantie();
                    PackGarantie association = packUnifiedService.ajouterGarantieAuPack(
                            pack.getIdPack(), garantieId, configuration);
                    packGarantieIdsCrees.add(association.getIdPackGarantie());
                }
            }

            return hierarchicalService.getProduitWithFullHierarchy(produit.getIdProduit());

        } catch (Exception e) {
            packGarantieIdsCrees.forEach(packGarantieRepository::deleteById);
            garantieIdsCrees.forEach(garantieRepository::deleteById);
            packIdsCrees.forEach(packUnifiedRepository::deleteById);
            produitIdsCrees.forEach(produitRepository::deleteById);
            throw new IllegalStateException(
                    "Échec de la création du produit complet, toutes les entités créées ont été annulées : "
                            + e.getMessage(), e);
        }
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }
}
