package tn.vermeg.gestionproduit.enums;

public enum StatutWorkflow {

    BROUILLON("En cours de rédaction"),
    SOUMIS_VALIDATION("Soumis pour validation"),
    EN_COURS_VALIDATION("En cours de validation"),
    APPROUVE("Approuvé par le responsable"),
    REJETE("Rejeté — corrections requises"),
    PUBLIE("Publié et commercialisé"),
    ARCHIVE("Archivé — plus commercialisé"),
    SUSPENDU("Suspendu temporairement");

    private final String libelle;

    StatutWorkflow(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public boolean estActif() {
        return this == PUBLIE;
    }

    public boolean estEditable() {
        return this == BROUILLON || this == REJETE;
    }
}
