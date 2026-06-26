# Entités et Énums - GestionProduit

Ce document décrit toutes les entités et énumérations utilisées dans le microservice GestionProduit.

---

## 1. Entités

### 1.1 Produit

**Collection MongoDB** : `produits`

**Description** : Représente un produit d'assurance de base (Santé, Vie, Auto, Habitation, Épargne).

**Attributs** :
| Attribut | Type | Description | Obligatoire |
|----------|------|-------------|-------------|
| `idProduit` | String | Identifiant unique du produit (auto-généré) | Non |
| `nomProduit` | String | Nom du produit d'assurance | Oui |
| `description` | String | Description détaillée du produit | Non |
| `typeProduit` | TypeProduit | Type de produit (SANTE, VIE, AUTO, HABITATION, EPARGNE) | Oui |
| `statut` | Statut | Statut du produit (ACTIF, INACTIF, EN_ATTENTE, SUSPENDU, EXPIRE, RESILIE) | Oui |
| `dateCreation` | Instant | Date de création du produit (auto-généré) | Non |
| `dateModification` | Instant | Date de dernière modification (auto-généré) | Non |

**Méthodes métier** :
- `estActif()` : Retourne `true` si le statut est ACTIF
- `estValide()` : Vérifie que le nom, le type et le statut sont définis

---

### 1.2 Pack

**Collection MongoDB** : `packs`

**Description** : Représente un pack d'assurance composé de garanties, associé à un produit.

**Attributs** :
| Attribut | Type | Description | Obligatoire |
|----------|------|-------------|-------------|
| `idPack` | String | Identifiant unique du pack (auto-généré) | Non |
| `nomPack` | String | Nom du pack d'assurance | Oui |
| `description` | String | Description détaillée du pack | Non |
| `produitId` | String | Identifiant du produit associé | Oui |
| `nomProduit` | String | Nom du produit associé (pour référencement) | Oui |
| `ageMinimum` | Integer | Âge minimum d'éligibilité | Non |
| `ageMaximum` | Integer | Âge maximum d'éligibilité | Non |
| `typeClients` | List<TypeClient> | Types de clients éligibles (INDIVIDUEL, FAMILLE, ENFANT, SENIOR, ENTREPRISE, ETUDIANT) | Non |
| `ancienneteContratMois` | int | Ancienneté de contrat requise en mois | Non |
| `couvertureGeographique` | CouvertureGeographique | Zone de couverture (LOCAL, NATIONAL, INTERNATIONAL, UE, MAGHREB) | Non |
| `prixMensuel` | double | Prix mensuel du pack | Oui |
| `dureeMinContrat` | int | Durée minimale du contrat en mois | Non |
| `dureeMaxContrat` | int | Durée maximale du contrat en mois | Non |
| `niveauCouverture` | NiveauCouverture | Niveau de couverture (BASIC, PREMIUM, GOLD) | Non |
| `statut` | Statut | Statut du pack | Oui |
| `domainesMedicaux` | List<String> | Domaines médicaux couverts par le pack | Non |
| `dateCreation` | Instant | Date de création (auto-généré) | Non |
| `dateModification` | Instant | Date de dernière modification (auto-généré) | Non |

---

### 1.3 Garantie

**Collection MongoDB** : `garanties`

**Description** : Représente une garantie d'assurance avec ses paramètres financiers et contractuels.

**Attributs** :
| Attribut | Type | Description | Obligatoire |
|----------|------|-------------|-------------|
| `idGarantie` | String | Identifiant unique de la garantie (auto-généré) | Non |
| `nomGarantie` | String | Nom de la garantie | Oui |
| `description` | String | Description détaillée de la garantie | Non |
| `statut` | Statut | Statut de la garantie | Oui |
| `domaine` | DomaineMedical | Domaine médical de la garantie (voir énumération détaillée) | Non |
| `tauxRemboursement` | double | Taux de remboursement (0 → 1 recommandé) | Non |
| `typeMontant` | TypeMontant | Type de calcul (FORFAIT, FRAIS_REELS, TARIF_CONVENTIONNE) | Non |
| `typePlafond` | TypePlafond | Type de plafond (PAR_ACTE, ANNUEL, MENSUEL, GLOBAL, PAR_SOINS) | Non |
| `plafondAnnuel` | double | Plafond annuel de remboursement | Non |
| `plafondMensuel` | double | Plafond mensuel de remboursement | Non |
| `plafondParActe` | double | Plafond par acte | Non |
| `franchise` | double | Franchise à la charge de l'assuré | Non |
| `coutMoyenParSinistre` | double | Coût moyen par sinistre | Non |
| `dureeMinContrat` | int | Durée minimale du contrat | Non |
| `dureeMaxContrat` | int | Durée maximale du contrat | Non |
| `resiliableAnnuellement` | boolean | Indique si le contrat est résiliable annuellement | Non |
| `creePar` | String | Utilisateur ayant créé la garantie | Non |
| `dateCreation` | Instant | Date de création (auto-généré) | Non |
| `dateModification` | Instant | Date de dernière modification (auto-généré) | Non |
| `dateDesactivation` | Instant | Date de désactivation | Non |

**Méthodes métier** :
- `estValide()` : Vérifie que le taux de remboursement, le plafond annuel et la franchise sont positifs
- `estActive()` : Retourne `true` si le statut est ACTIF et aucune date de désactivation

---

### 1.4 PackGarantie

**Collection MongoDB** : `pack_garanties`

**Description** : Table d'association entre un pack et une garantie, avec des paramètres personnalisés.

**Attributs** :
| Attribut | Type | Description | Obligatoire |
|----------|------|-------------|-------------|
| `idPackGarantie` | String | Identifiant unique (auto-généré) | Non |
| `packId` | String | Identifiant du pack | Oui |
| `garantieId` | String | Identifiant de la garantie | Oui |
| `nomGarantie` | String | Nom de la garantie (pour référencement) | Oui |
| `tauxRemboursement` | double | Taux de remboursement personnalisé pour ce pack | Non |
| `plafond` | double | Plafond personnalisé pour ce pack | Non |
| `franchise` | double | Franchise personnalisée pour ce pack | Non |
| `typeMontant` | TypeMontant | Type de calcul du montant | Non |
| `delaiCarence` | int | Délai de carence en jours/mois | Non |
| `priorite` | int | Priorité de la garantie dans le pack | Non |
| `actif` | boolean | Indique si l'association est active | Non |
| `dateActivation` | Instant | Date d'activation (auto-généré) | Non |
| `dateDesactivation` | Instant | Date de désactivation | Non |
| `optionnelle` | boolean | Indique si la garantie est optionnelle dans le pack | Non |
| `supplementPrix` | double | Supplément de prix pour cette garantie optionnelle | Non |

**Méthodes métier** :
- `estValide()` : Vérifie que le plafond et le taux de remboursement sont positifs
- `calculerRemboursement(double montant)` : Calcule le remboursement en appliquant la franchise, le taux et le plafond

---

## 2. Hiérarchie des entités

```
Produit (Produit d'assurance)
  └── Pack (Ensemble de garanties)
      └── PackGarantie (Association Pack-Garantie)
          └── Garantie (Couverture spécifique)
```

**Explication** :
- Un **Produit** est le niveau le plus haut (ex: Santé, Vie, Auto)
- Un **Pack** est une offre commerciale basée sur un Produit
- Une **Garantie** est une couverture spécifique indépendante
- **PackGarantie** est l'association entre un Pack et une Garantie avec des paramètres personnalisés

---

## 3. Énumérations

### 3.1 TypeProduit

**Description** : Types de produits d'assurance disponibles.

**Valeurs** :
| Valeur | Description |
|--------|-------------|
| `SANTE` | Assurance santé |
| `HABITATION` | Assurance habitation |
| `AUTO` | Assurance automobile |
| `EPARGNE` | Produits d'épargne |
| `VIE` | Assurance vie |

---

### 3.2 Statut

**Description** : Statut d'une entité (produit, pack, garantie).

**Valeurs** :
| Valeur | Description |
|--------|-------------|
| `ACTIF` | Entité active et disponible |
| `INACTIF` | Entité inactive |
| `EN_ATTENTE` | En attente de validation |
| `SUSPENDU` | Temporairement suspendu |
| `EXPIRE` | Expiré |
| `RESILIE` | Résilié |

---

### 3.3 NiveauCouverture

**Description** : Niveau de couverture d'un pack.

**Valeurs** :
| Valeur | Description |
|--------|-------------|
| `BASIC` | Couverture basique |
| `PREMIUM` | Couverture premium |
| `GOLD` | Couverture gold (maximale) |

---

### 3.4 CouvertureGeographique

**Description** : Zone géographique de couverture.

**Valeurs** :
| Valeur | Description |
|--------|-------------|
| `LOCAL` | Couverture locale |
| `NATIONAL` | Couverture nationale |
| `INTERNATIONAL` | Couverture internationale |
| `UE` | Couverture Union Européenne |
| `MAGHREB` | Couverture Maghreb |

---

### 3.5 TypeClient

**Description** : Types de clients éligibles pour un pack.

**Valeurs** :
| Valeur | Description |
|--------|-------------|
| `INDIVIDUEL` | Client individuel |
| `FAMILLE` | Famille |
| `ENFANT` | Enfant |
| `SENIOR` | Senior |
| `ENTREPRISE` | Entreprise |
| `ETUDIANT` | Étudiant |

---

### 3.6 DomaineMedical

**Description** : Domaines médicaux couverts par les garanties.

**Valeurs principales** :

**Médecine générale** :
- `CONSULTATION_GENERALE` - Consultation générale
- `MEDECINE_FAMILIALE` - Médecine familiale
- `MEDECINE_INTERNE` - Médecine interne
- `MEDECINE_PREVENTIVE` - Médecine préventive

**Spécialités médicales** :
- `CARDIOLOGIE` - Cardiologie
- `DERMATOLOGIE` - Dermatologie
- `ENDOCRINOLOGIE` - Endocrinologie
- `GASTRO_ENTEROLOGIE` - Gastro-entérologie
- `HEMATOLOGIE` - Hématologie
- `INFECTIOLOGIE` - Infectiologie
- `NEPHROLOGIE` - Néphrologie
- `NEUROLOGIE` - Neurologie
- `ONCOLOGIE` - Oncologie
- `PNEUMOLOGIE` - Pneumologie
- `RHEUMATOLOGIE` - Rhumatologie
- `ALLERGOLOGIE` - Allergologie

**Santé de la femme** :
- `GYNECOLOGIE` - Gynécologie
- `OBSTETRIQUE` - Obstétrique
- `FERTILITE_PMA` - Fertilité / PMA
- `SUIVI_GROSSESSE` - Suivi grossesse

**Santé de l'enfant** :
- `PEDIATRIE` - Pédiatrie
- `NEONATOLOGIE` - Néonatologie
- `PEDOPSYCHIATRIE` - Pédopsychiatrie

**Santé mentale** :
- `PSYCHIATRIE` - Psychiatrie
- `PSYCHOLOGIE` - Psychologie
- `PSYCHOTHERAPIE` - Psychothérapie
- `ADDICTOLOGIE` - Addictologie

**Chirurgie** :
- `CHIRURGIE_GENERALE` - Chirurgie générale
- `CHIRURGIE_ESTHETIQUE` - Chirurgie esthétique
- `CHIRURGIE_ORTHOPEDIQUE` - Chirurgie orthopédique
- `NEUROCHIRURGIE` - Neurochirurgie
- `CHIRURGIE_CARDIAQUE` - Chirurgie cardiaque
- `CHIRURGIE_DIGESTIVE` - Chirurgie digestive
- `CHIRURGIE_UROLOGIQUE` - Chirurgie urologique

**Dentaire** :
- `DENTISTERIE_GENERALE` - Dentisterie générale
- `ORTHODONTIE` - Orthodontie
- `IMPLANTOLOGIE` - Implantologie
- `CHIRURGIE_DENTAIRE` - Chirurgie dentaire

**Vision et ORL** :
- `OPHTALMOLOGIE` - Ophtalmologie
- `ORL` - ORL
- `AUDIOLOGIE` - Audiologie

**Rééducation et thérapies** :
- `KINESITHERAPIE` - Kinésithérapie
- `PHYSIOTHERAPIE` - Physiothérapie
- `ERGOTHERAPIE` - Ergothérapie
- `ORTHOPHONIE` - Orthophonie
- `CHIROPRATIE` - Chiropractie
- `OSTEOPATHIE` - Ostéopathie

**Examens et diagnostic** :
- `RADIOLOGIE` - Radiologie
- `IMAGERIE_MEDICALE` - Imagerie médicale
- `ANALYSES_BIOLOGIQUES` - Analyses biologiques
- `MEDECINE_NUCLEAIRE` - Médecine nucléaire

**Médecines spécialisées** :
- `MEDECINE_SPORT` - Médecine du sport
- `MEDECINE_TRAVAIL` - Médecine du travail
- `GERIATRIE` - Gériatrie
- `NUTRITION_DIETETIQUE` - Nutrition / Diététique
- `MEDECINE_ESTHETIQUE` - Médecine esthétique
- `MEDECINE_ALTERNATIVE` - Médecine alternative

**Urgences et soins critiques** :
- `URGENCES_MEDICALES` - Urgences médicales
- `REANIMATION` - Réanimation
- `SOINS_INTENSIFS` - Soins intensifs

**Téléconsultation** :
- `CONSULTATION_VIDEO` - Consultation vidéo
- `SUIVI_DISTANCE` - Suivi à distance
- `DEUXIEME_AVIS_MEDICAL` - Deuxième avis médical international

**Domaines existants (compatibilité)** :
- `HOSPITALISATION` - Hospitalisation
- `DENTAIRE` - Dentaire
- `OPTIQUE` - Optique
- `PHARMACIE` - Pharmacie
- `MATERNITE` - Maternité
- `PREVENTION` - Prévention
- `SOINS_DENTAIRES` - Soins dentaires
- `SOINS_OPTIQUES` - Soins optiques
- `HOSPITALISATION_CHIRURGICALE` - Hospitalisation chirurgicale
- `HOSPITALISATION_MEDICALE` - Hospitalisation médicale
- `MEDECINE_GENERALE` - Médecine générale
- `SPECIALITES_MEDICALES` - Spécialités médicales
- `PARAMEDICAL` - Paramédical
- `ANALYSES_MEDICALES` - Analyses médicales
- `REPATRIEMENT` - Rapatriement
- `DECES` - Décès
- `INVALIDITE` - Invalidité
- `AUTRE` - Autre (valeur par défaut)

**Méthode utilitaire** :
- `fromString(String text)` : Convertit une chaîne en DomaineMedical (gère les espaces, tirets, slashes)

---

### 3.7 TypeMontant

**Description** : Type de calcul du montant de remboursement.

**Valeurs** :
| Valeur | Description |
|--------|-------------|
| `FORFAIT` | Montant forfaitaire |
| `FRAIS_REELS` | Remboursement sur frais réels |
| `TARIF_CONVENTIONNE` | Tarif conventionné |

---

### 3.8 TypePlafond

**Description** : Type de plafond de remboursement.

**Valeurs** :
| Valeur | Description |
|--------|-------------|
| `PAR_ACTE` | Plafond par acte |
| `ANNUEL` | Plafond annuel |
| `MENSUEL` | Plafond mensuel |
| `GLOBAL` | Plafond global |
| `PAR_SOINS` | Plafond par type de soins |

---

## 4. Relations entre entités

### Relation Produit → Pack (One-to-Many)
- Un Produit peut avoir plusieurs Packs
- Un Pack est associé à un seul Produit
- Attribut de liaison : `produitId` dans Pack

### Relation Pack → PackGarantie (One-to-Many)
- Un Pack peut avoir plusieurs PackGarantie
- Un PackGarantie est associé à un seul Pack
- Attribut de liaison : `packId` dans PackGarantie

### Relation Garantie → PackGarantie (One-to-Many)
- Une Garantie peut être associée à plusieurs PackGarantie
- Un PackGarantie est associé à une seule Garantie
- Attribut de liaison : `garantieId` dans PackGarantie

---

## 5. Collections MongoDB

| Collection | Entité | Description |
|------------|--------|-------------|
| `produits` | Produit | Produits d'assurance de base |
| `packs` | Pack | Packs d'assurance |
| `garanties` | Garantie | Garanties d'assurance |
| `pack_garanties` | PackGarantie | Association Pack-Garantie |

---

**Document généré pour le microservice GestionProduit**  
**Version** : 1.0.0  
**Date** : 24 Juin 2026
