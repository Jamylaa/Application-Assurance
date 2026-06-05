package tn.vermeg.gestionproduit.dto;

public class BeneficiaireDTO {
    
    private String idBeneficiaire;
    private String prenom;
    private String nom;
    private Integer age;
    private String sexe;  // M, F, Autre
    private String lien;  // Enfant, Conjoint, Parent, Autre
    private String etatSante; // Ex: Bon, Moyen, Mauvais
    private boolean handicape;
    private String remarques;
    
    // Constructeurs
    public BeneficiaireDTO() {}
    
    public BeneficiaireDTO(String prenom, String nom, Integer age, String lien) {
        this.prenom = prenom;
        this.nom = nom;
        this.age = age;
        this.lien = lien;
    }
    
    // Getters & Setters
    public String getIdBeneficiaire() { return idBeneficiaire; }
    public void setIdBeneficiaire(String idBeneficiaire) { this.idBeneficiaire = idBeneficiaire; }
    
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    
    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }
    
    public String getLien() { return lien; }
    public void setLien(String lien) { this.lien = lien; }
    
    public String getEtatSante() { return etatSante; }
    public void setEtatSante(String etatSante) { this.etatSante = etatSante; }
    
    public boolean isHandicape() { return handicape; }
    public void setHandicape(boolean handicape) { this.handicape = handicape; }
    
    public String getRemarques() { return remarques; }
    public void setRemarques(String remarques) { this.remarques = remarques; }
    
    @Override
    public String toString() {
        return String.format("Beneficiaire{nom='%s %s', age=%d, lien='%s'}", prenom, nom, age, lien);
    }
}

