package tn.vermeg.gestionproduit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.vermeg.gestionproduit.entities.Pack;
import tn.vermeg.gestionproduit.enums.DomaineMedical;

import java.util.List;

/**
 * Vue enrichie d'un Pack pour le frontend : ajoute {@code idGaranties} et
 * {@code domainesMedicaux}, tous deux calculés à la demande via PackGarantie
 * (jamais stockés/dénormalisés sur l'entité).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackDetailDTO {
    private Pack pack;
    private List<String> idGaranties;
    private List<DomaineMedical> domainesMedicaux;
}
