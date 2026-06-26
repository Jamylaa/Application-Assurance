package tn.vermeg.gestionproduit.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.vermeg.gestionproduit.enums.CouvertureGeographique;
import tn.vermeg.gestionproduit.enums.NiveauCouverture;
import tn.vermeg.gestionproduit.enums.Statut;
import tn.vermeg.gestionproduit.enums.TypeClient;

import java.time.Instant;
import java.util.List;

@Document(collection = "packs")
public class Pack {

    @Id
    private String idPack;
    private String nomPack;
    private String description;
    private String produitId;
    private String nomProduit;

    // Conditions d'éligibilité
    private Integer ageMinimum;
    private Integer ageMaximum;
    private List<TypeClient> typeClients;
    private int ancienneteContratMois;
    private CouvertureGeographique couvertureGeographique;

    private double prixMensuel;
    private int dureeMinContrat;
    private int dureeMaxContrat;

    private NiveauCouverture niveauCouverture;
    private Statut statut;
    
    // Domaines médicaux couverts par le pack
    private List<String> domainesMedicaux;

    @CreatedDate
    private Instant dateCreation;
    @LastModifiedDate
    private Instant dateModification;

    // NOTE: Les garanties associées ne sont pas stockées directement dans le document MongoDB
    // pour éviter les problèmes de performance. Elles sont récupérées via le service
    // et construites dynamiquement dans les entités.
    private transient List<Garantie> garanties;

    public Pack() {}
    public Pack(String idPack, String nomPack, String description, String produitId, String nomProduit,
                Integer ageMinimum, Integer ageMaximum, List<TypeClient> typeClients,
                int ancienneteContratMois, CouvertureGeographique couvertureGeographique,
                double prixMensuel, int dureeMinContrat, int dureeMaxContrat,
                NiveauCouverture niveauCouverture, Statut statut,
                Instant dateCreation, Instant dateModification) {

        this.idPack = idPack;
        this.nomPack = nomPack;
        this.description = description;
        this.produitId = produitId;
        this.nomProduit = nomProduit;
        this.ageMinimum = ageMinimum;
        this.ageMaximum = ageMaximum;
        this.typeClients = typeClients;
        this.ancienneteContratMois = ancienneteContratMois;
        this.couvertureGeographique = couvertureGeographique;
        this.prixMensuel = prixMensuel;
        this.dureeMinContrat = dureeMinContrat;
        this.dureeMaxContrat = dureeMaxContrat;
        this.niveauCouverture = niveauCouverture;
        this.statut = statut;
        this.domainesMedicaux = new java.util.ArrayList<>();
        this.dateCreation = dateCreation;
        this.dateModification = dateModification;
    }

    //   Getters & Setters

    public String getIdPack() { return idPack; }
    public void setIdPack(String idPack) { this.idPack = idPack; }

    public String getNomPack() { return nomPack; }
    public void setNomPack(String nomPack) { this.nomPack = nomPack; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getProduitId() { return produitId; }
    public void setProduitId(String produitId) { this.produitId = produitId; }

    public String getNomProduit() { return nomProduit; }
    public void setNomProduit(String nomProduit) { this.nomProduit = nomProduit; }

    public Integer getAgeMinimum() { return ageMinimum; }
    public void setAgeMinimum(Integer ageMinimum) { this.ageMinimum = ageMinimum; }

    public Integer getAgeMaximum() { return ageMaximum; }
    public void setAgeMaximum(Integer ageMaximum) { this.ageMaximum = ageMaximum; }

    public List<TypeClient> getTypeClients() { return typeClients; }
    public void setTypeClients(List<TypeClient> typeClients) { this.typeClients = typeClients; }

    public int getAncienneteContratMois() { return ancienneteContratMois; }
    public void setAncienneteContratMois(int ancienneteContratMois) { this.ancienneteContratMois = ancienneteContratMois; }

    public CouvertureGeographique getCouvertureGeographique() { return couvertureGeographique; }
    public void setCouvertureGeographique(CouvertureGeographique couvertureGeographique) { this.couvertureGeographique = couvertureGeographique; }

    public double getPrixMensuel() { return prixMensuel; }
    public void setPrixMensuel(double prixMensuel) { this.prixMensuel = prixMensuel; }

    public int getDureeMinContrat() { return dureeMinContrat; }
    public void setDureeMinContrat(int dureeMinContrat) { this.dureeMinContrat = dureeMinContrat; }

    public int getDureeMaxContrat() { return dureeMaxContrat; }
    public void setDureeMaxContrat(int dureeMaxContrat) { this.dureeMaxContrat = dureeMaxContrat; }

    public NiveauCouverture getNiveauCouverture() { return niveauCouverture; }
    public void setNiveauCouverture(NiveauCouverture niveauCouverture) { this.niveauCouverture = niveauCouverture; }

    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }

    public Instant getDateCreation() { return dateCreation; }
    public void setDateCreation(Instant dateCreation) { this.dateCreation = dateCreation; }

    public Instant getDateModification() { return dateModification; }
    public void setDateModification(Instant dateModification) { this.dateModification = dateModification; }

    public List<String> getDomainesMedicaux() { return domainesMedicaux; }
    public void setDomainesMedicaux(List<String> domainesMedicaux) { this.domainesMedicaux = domainesMedicaux; }

    public List<Garantie> getGaranties() { return garanties; }
    public void setGaranties(List<Garantie> garanties) { this.garanties = garanties; }
}