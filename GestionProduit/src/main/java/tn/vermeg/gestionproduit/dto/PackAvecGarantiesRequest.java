package tn.vermeg.gestionproduit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.vermeg.gestionproduit.entities.Pack;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackAvecGarantiesRequest {
    private Pack pack;
    private List<GarantieAssociationRequest> garanties;
}
