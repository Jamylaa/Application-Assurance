package tn.vermeg.gestionproduit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.vermeg.gestionproduit.entities.Produit;

import java.util.List;

/**
 * Requête de création d'un produit complet en une seule opération :
 * produit + packs + garanties par pack (créées ou réutilisées).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreationProduitCompletRequest {
    private Produit produit;
    private List<PackAvecGarantiesRequest> packs;
}
