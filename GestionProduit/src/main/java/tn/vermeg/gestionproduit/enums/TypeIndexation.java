package tn.vermeg.gestionproduit.enums;

public enum TypeIndexation {

    AUCUNE("Pas d'indexation — prime fixe"),
    FIXE("Taux d'indexation fixe contractuellement défini"),
    INFLATION("Indexation sur l'indice de l'inflation nationale"),
    INDICE_SANTE("Indexation sur l'indice médical / soins de santé"),
    INDICE_AUTO("Indexation sur l'indice automobile"),
    INDICE_CONSTRUCTION("Indexation sur l'indice de la construction"),
    NEGOCIABLE("Taux négocié individuellement");

    private final String libelle;

    TypeIndexation(String libelle) { this.libelle = libelle; }
    public String getLibelle() { return libelle; }
}
