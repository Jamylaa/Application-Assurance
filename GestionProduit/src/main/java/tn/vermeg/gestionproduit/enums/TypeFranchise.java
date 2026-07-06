package tn.vermeg.gestionproduit.enums;

public enum TypeFranchise {

    AUCUNE("Aucune franchise — prise en charge à 100%"),
    FIXE("Franchise fixe en montant absolu (TND)"),
    POURCENTAGE("Franchise en pourcentage du montant sinistre"),
    RELATIVE("Franchise relative — disparaît si le sinistre dépasse le seuil"),
    ABSOLUE("Franchise absolue — toujours déduite quel que soit le montant");

    private final String libelle;

    TypeFranchise(String libelle) { this.libelle = libelle; }
    public String getLibelle() { return libelle; }
}
