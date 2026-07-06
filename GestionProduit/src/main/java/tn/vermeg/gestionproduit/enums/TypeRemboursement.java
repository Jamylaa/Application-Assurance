package tn.vermeg.gestionproduit.enums;

public enum TypeRemboursement {

    FRAIS_REELS("Remboursement des frais réels engagés"),
    FORFAIT("Remboursement forfaitaire fixe"),
    TARIF_CONVENTIONNE("Remboursement sur la base du tarif conventionné"),
    CAPITAL_DECES("Versement d'un capital en cas de décès"),
    INDEMNITE_JOURNALIERE("Indemnité journalière en cas d'incapacité"),
    RENTE_VIAGERE("Rente mensuelle viagère"),
    RENTE_EDUCATION("Rente pour les enfants"),
    VALEUR_A_NEUF("Remplacement à neuf sans vétusté"),
    VALEUR_VENALE("Remboursement sur la valeur vénale marchande");

    private final String libelle;

    TypeRemboursement(String libelle) { this.libelle = libelle; }
    public String getLibelle() { return libelle; }

    public boolean estRemboursementVariable() {
        return this == FRAIS_REELS || this == TARIF_CONVENTIONNE || this == VALEUR_VENALE;
    }
}
