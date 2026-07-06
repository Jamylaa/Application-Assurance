package tn.vermeg.gestionproduit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.vermeg.gestionproduit.entities.Produit;

import java.util.List;

/**
 * Vue enrichie d'un Produit pour le frontend : ajoute {@code idPacks},
 * calculé à la demande (jamais stocké/dénormalisé sur l'entité).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProduitDetailDTO {
    private Produit produit;
    private List<String> idPacks;
}
