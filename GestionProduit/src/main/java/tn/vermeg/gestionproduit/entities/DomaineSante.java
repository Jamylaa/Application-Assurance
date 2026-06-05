package tn.vermeg.gestionproduit.entities;

/**
 * Domaine médical regroupant les types de garanties santé.
 * Permet de catégoriser les garanties par grande famille de soins.
 */
public enum DomaineSante {
    SOINS_COURANTS("Soins courants"),
    HOSPITALISATION("Hospitalisation"),
    OPTIQUE("Optique"),
    DENTAIRE("Dentaire"),
    PHARMACIE("Pharmacie"),
    ASSISTANCE("Assistance et international");

    private final String libelle;

    DomaineSante(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
