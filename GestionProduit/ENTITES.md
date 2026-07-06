# Entités et Relations — GestionProduit

Référence pratique et exhaustive du modèle de données **actuel** du microservice `GestionProduit`. La section [9. Genèse du modèle](#9-genèse-du-modèle) explique le *pourquoi* de ces choix de conception.

> Généré à partir du code source (`entities/`, `entities/embedded/`, `enums/`) pour garantir l'exactitude. Toute évolution du modèle doit être répercutée ici.
>
> **Périmètre volontairement restreint à la relation d'assurance** (produit / pack / garantie et leur tarification) — les paramètres de gestion de contrat (durée, résiliation, renouvellement, workflow documentaire, versioning réglementaire, offres commerciales) et les critères d'éligibilité fine (âge, type de client, profession, ancienneté) ont été retirés du modèle : ce microservice décrit le catalogue, pas la gestion administrative des contrats ni la souscription. Le cycle de vie éditorial (`StatutWorkflow`) est l'unique mécanisme de statut — il n'y a plus de statut opérationnel séparé.

---

## 1. Schéma relationnel

```
Produit (1) ──────< Pack (N)                    [Pack.produitId]
Pack (1) ──────< PackGarantie (N) >────── (1) Garantie   [table de jonction enrichie]

Produit (1) ──────< CritereRecommandation (N)   [CritereRecommandation.produitId]
Pack    (0..1) ──── CritereRecommandation (N)   [CritereRecommandation.packId, optionnel]

Pack (N) ──────< Pack (N)        via packsCompatibles / packsIncompatibles / optionsPackIds (auto-référence par IDs)
Garantie (N) ──────< Garantie (N) via prerequisGarantieIds (auto-référence par IDs)
```

---

## 2. Entités principales

### 2.1 Produit

**Collection** : `produits` · **Fichier** : `entities/Produit.java`

Le produit est le niveau le plus haut du catalogue (ex: "Santé Complémentaire", "Auto Tous Risques"). Il ne porte pas de tarification directe — celle-ci est définie au niveau des `Pack`.

| Champ | Type | Description |
|-------|------|-------------|
| `idProduit` | String (@Id) | Identifiant technique auto-généré |
| `codeProduit` | String (unique, requis) | Code métier, ex: `"SANTE-COMP-001"` |
| `nomProduit` | String (requis) | Nom technique interne |
| `nomCommercial` | String | Nom affiché au client |
| `description` | String (requis) | Description complète |
| `typeProduit` | TypeProduit (requis) | SANTE / HABITATION / AUTO / EPARGNE / VIE |
| `statutWorkflow` | StatutWorkflow (défaut BROUILLON) | Cycle de vie éditorial — seul mécanisme de statut |
| `validePar`, `dateValidation` | String, Instant | Traçabilité de la validation |
| `prixBase` | double (positif, défaut 0) | Prime de référence (estimation/comparateur) |
| `devisePrix` | String (défaut "TND") | Devise |
| `couvertureGeographique` | CouvertureGeographique (défaut NATIONAL) | Zone couverte |
| `territoiresExclus` | List\<String\> | Zones explicitement exclues |
| `version` | String (défaut "1.0") | Version courante du produit |
| `versionPrecedenteId` | String | Chaînage vers la version antérieure |
| `dateEffet`, `dateExpiration` | Instant | Fenêtre de commercialisation |
| `packs` | List\<Pack\> (`@Transient`) | **Non persisté** — peuplé uniquement par `GET /api/produits/{id}/detail` |
| `creePar`, `modifiePar` | String | Audit |
| `dateCreation`, `dateModification` | Instant (auto) | Horodatage |

**Méthodes métier** : `estActif()` (⇔ `statutWorkflow == PUBLIE`), `estCommercialisable()`, `estValide()`

---

### 2.2 Pack

**Collection** : `packs` · **Fichier** : `entities/Pack.java`

Le pack est la formule commerciale concrète et tarifée d'un produit (ex: BASIC/PREMIUM/GOLD d'une mutuelle santé).

| Champ | Type | Description |
|-------|------|-------------|
| `idPack` | String (@Id) | Identifiant technique auto-généré |
| `codePack` | String (unique, requis) | Code métier, ex: `"SANTE-COMP-BASIC-001"` |
| `nomPack` | String (requis) | Nom technique interne |
| `nomCommercial` | String | Nom affiché au client (ex: "Formule Essentielle") |
| `description` | String (requis) | Description complète |
| `descriptionCourte` | String | Résumé (≤280 car.) |
| `produitId` | String (requis, indexé) | **Référence vers `Produit.idProduit`** |
| `niveauCouverture` | NiveauCouverture (requis) | BASIC / PREMIUM / GOLD |
| `statutWorkflow` | StatutWorkflow (défaut BROUILLON) | Cycle de vie éditorial — seul mécanisme de statut |
| `packRecommande` | boolean (défaut false) | Badge marketing |
| `colorTheme` | String (défaut "#0f4c81") | Couleur du badge (hex) |
| `prixMensuel` | double (positif, requis) | Prime mensuelle (TND) |
| `prixAnnuel` | double | Prime annuelle si différente de prixMensuel × 12 |
| `tauxRemiseAnnuelle` | double (défaut 0) | Remise en % pour paiement annuel |
| `devisePrix` | String (défaut "TND") | Devise |
| `versionPack` | String (défaut "1.0") | Version commerciale de la formule |
| `optionsDisponibles` | boolean (défaut false) | Le pack propose-t-il des extensions ? |
| `optionsPackIds` | List\<String\> | IDs de packs "extension" (auto-référence) |
| `packsCompatibles` | List\<String\> | IDs de packs combinables en multi-risques |
| `packsIncompatibles` | List\<String\> | IDs de packs mutuellement exclusifs |
| `dateEffet`, `dateExpiration` | Instant | Fenêtre de commercialisation |
| `garanties` | List\<PackGarantie\> (`@Transient`) | **Non persisté** — peuplé uniquement par `GET /api/packs/{id}/detail` |
| `creePar`, `modifiePar` | String | Audit |
| `dateCreation`, `dateModification` | Instant (auto) | Horodatage |

**Méthodes métier** : `estActif()` (⇔ `statutWorkflow == PUBLIE`), `estCommercialisable()`, `getPrixAnnuelEffectif()`, `estValide()`

---

### 2.3 Garantie

**Collection** : `garanties` · **Fichier** : `entities/Garantie.java`

Une garantie définit CE QUI EST COUVERT (l'événement, le montant, les conditions). Elle est **indépendante de tout pack** — l'association se fait exclusivement via `PackGarantie`, ce qui permet à une même garantie d'être proposée dans plusieurs packs avec des paramètres différents.

| Champ | Type | Description |
|-------|------|-------------|
| `idGarantie` | String (@Id) | Identifiant technique auto-généré |
| `codeGarantie` | String (unique, requis) | Code métier, ex: `"GAR-HOSP-001"` |
| `nomGarantie` | String (requis) | Nom complet |
| `nomCourt` | String | Nom court pour tableaux comparatifs (≤30 car.) |
| `description` | String (requis) | Description complète pour l'assuré |
| `descriptionTechnique` | String | Destinée aux actuaires/juristes |
| `domaine` | DomaineMedical (requis) | Domaine médical — voir §6.1 |
| `garantieObligatoireParDefaut` | boolean (défaut false) | Obligatoire par nature dans tout pack qui l'inclut |
| `statutWorkflow` | StatutWorkflow (défaut BROUILLON) | Cycle de vie éditorial — seul mécanisme de statut |
| `evenementsCouvertsParDefaut` | List\<String\> | Actes/événements pris en charge (doc + chatbot) |
| `plafond` | PlafondGarantie (embedded) | Value Object — voir §4.1 |
| `franchise` | FranchiseGarantie (embedded) | Value Object — voir §4.2 |
| `typeRemboursement` | TypeRemboursement (défaut FRAIS_REELS) | Mode de calcul |
| `tauxRemboursementBase` | double (0–100, défaut 80.0) | Taux de remboursement standard, **en %** |
| `tauxRemboursementMinimum` | double (0–100, défaut 0) | Plancher modulable |
| `tauxRemboursementMaximum` | double (0–100, défaut 100) | Plafond modulable |
| `regleCalcul` | RegleCalcul (embedded) | Value Object — voir §4.3 |
| `prerequisGarantieIds` | List\<String\> | IDs de garanties requises au préalable (auto-référence) |
| `parametresDynamiques` | Map\<String, Object\> | Paramètres variables de la formule de calcul |
| `primePureBase` | double (défaut 0) | Donnée actuarielle (TND/mois) |
| `creePar`, `modifiePar` | String | Audit |
| `dateCreation`, `dateModification`, `dateDesactivation` | Instant | Horodatage |

**Méthodes métier** : `estValide()`, `estActive()` (⇔ `statutWorkflow == PUBLIE` **et** `dateDesactivation == null`), `calculerRemboursement(montantSinistre)` (applique taux → plafond → franchise, ou délègue à `regleCalcul` si définie)

---

### 2.4 PackGarantie

**Collection** : `pack_garanties` · **Fichier** : `entities/PackGarantie.java`

Table de jonction **enrichie** entre `Pack` et `Garantie` : elle ne fait pas que lier les deux, elle permet de **surcharger** les paramètres de remboursement pour adapter une garantie de base à une formule commerciale précise (ex : Hospitalisation à 80% par défaut, mais 100% dans le pack GOLD).

| Champ | Type | Description |
|-------|------|-------------|
| `idPackGarantie` | String (@Id) | Identifiant technique auto-généré |
| `packId` | String (requis) | **Référence vers `Pack.idPack`** |
| `garantieId` | String (requis) | **Référence vers `Garantie.idGarantie`** |
| `nomGarantie`, `codeGarantie` | String | Dénormalisés depuis `Garantie` (lecture sans jointure) |
| `tauxRemboursementSpecifique` | Double (0–100, nullable) | Surcharge `Garantie.tauxRemboursementBase` ; `null` = hériter |
| `plafondSpecifique` | PlafondGarantie (embedded, nullable) | Surcharge `Garantie.plafond` ; `null` = hériter |
| `franchiseSpecifique` | FranchiseGarantie (embedded, nullable) | Surcharge `Garantie.franchise` ; `null` = hériter |
| `typeMontant` | TypeMontant (nullable) | FORFAIT / FRAIS_REELS / TARIF_CONVENTIONNE pour ce pack |
| `optionnelle` | boolean (défaut false) | Si true, le souscripteur choisit de l'inclure (avec `supplementPrix`) |
| `supplementPrix` | double (défaut 0) | Surcoût mensuel si optionnelle et souscrite |
| `actif` | boolean (défaut true) | Association active |
| `configurePar` | String | Audit |
| `dateActivation` (auto), `dateDesactivation`, `dateModification` (auto) | Instant | Horodatage |

**Contrainte** : index composé unique `(packId, garantieId)` — une garantie ne peut être associée qu'une fois au même pack.

**Méthodes métier** : `estValide()`, `estActif()`, `calculerRemboursement(montantSinistre, tauxBase, plafondBase, franchiseBase)`

---

## 3. Entités de support

### 3.1 CritereRecommandation

**Collection** : `criteres_recommandation` · **Fichier** : `entities/CritereRecommandation.java` · **API** : `/api/criteres-recommandation`

Règles de scoring éditables pour le moteur de recommandation du chatbot IA, sans avoir à modifier le code Python.

| Champ | Type | Description |
|-------|------|-------------|
| `idCritere` | String (@Id) | Identifiant technique |
| `produitId` | String (requis, indexé) | **Référence vers `Produit.idProduit`** |
| `packId` | String (indexé, nullable) | **Référence vers `Pack.idPack`** ; `null` = critères génériques du produit |
| `motsCles` | List\<String\> | Déclencheurs de recommandation |
| `synonymes` | List\<String\> | Variantes acceptées (NLP) |
| `casUsages` | List\<String\> | Situations typiques recommandées |
| `profilsRecommandes` | List\<String\> | Profils clients types |
| `scoreBase` | double (0–100, défaut 50.0) | Score de départ avant pondération |
| `poidsAttributs` | Map\<String, Double\> | Poids par attribut, clés libres (ex: `budget`, `age`, `couverture`, `duree`) |
| `bonusConditionnels` | Map\<String, Double\> | Bonus si condition remplie |
| `reglesDisqualification` | List\<String\> | Conditions excluant l'offre de la recommandation |
| `prioriteAffichage` | int (défaut 5) | Départage en cas d'égalité de score |
| `actif` | boolean (défaut true) | Règle active |
| `creePar`, `dateCreation`, `dateModification` | String / Instant | Audit |

**Contrainte** : un seul `CritereRecommandation` par pack (`findByPackId` retourne un `Optional`).

**Méthodes métier** : `calculerScore(attributsProfil)`, `contientMotCle(terme)`

---

## 4. Objets embarqués (Value Objects)

Ces objets n'ont pas de collection propre : ils sont sérialisés à l'intérieur du document parent (Produit/Pack/Garantie/PackGarantie).

### 4.1 PlafondGarantie
*(dans `Garantie.plafond` et `PackGarantie.plafondSpecifique`)*

| Champ | Type | Description |
|-------|------|-------------|
| `plafondParActe` | double | Par acte médical (0 = illimité) |
| `plafondMensuel` | double | Par mois (0 = illimité) |
| `plafondAnnuel` | double | Par année de contrat (0 = illimité) |
| `plafondGlobal` | double | Sur toute la durée du contrat (0 = illimité) |
| `plafondParSoins` | double | Par épisode de soins regroupant plusieurs actes |
| `typePrincipal` | TypePlafond | Type de plafond effectivement appliqué au calcul |
| `description` | String | Texte lisible, ex: "3 000 TND/an — 500 TND/acte" |
| `devise` | String (défaut "TND") | Devise |

**Méthodes** : `estIllimite()` (true si les 5 montants sont à 0), `appliquerPlafond(montantCalcule)`

### 4.2 FranchiseGarantie
*(dans `Garantie.franchise` et `PackGarantie.franchiseSpecifique`)*

| Champ | Type | Description |
|-------|------|-------------|
| `type` | TypeFranchise (défaut AUCUNE) | AUCUNE / FIXE / POURCENTAGE / RELATIVE / ABSOLUE |
| `montantFixe` | double (défaut 0) | Utilisé si FIXE ou ABSOLUE |
| `pourcentage` | double (défaut 0) | Utilisé si POURCENTAGE (0–100) |
| `montantMinimum`, `montantMaximum` | double (défaut 0) | Bornes si calculée en % (max sert aussi de seuil pour RELATIVE) |
| `description` | String | Texte lisible, ex: "10% du sinistre, min 50 TND" |
| `devise` | String (défaut "TND") | Devise |

**Méthode** : `calculerFranchise(montantSinistre)`

### 4.3 RegleCalcul
*(dans `Garantie.regleCalcul`)*

| Champ | Type | Description |
|-------|------|-------------|
| `formule` | String | Formule textuelle documentaire, ex: `"montant * taux / 100 - franchise"` |
| `descriptionFormule` | String | Explication pour juriste/assureur |
| `parametresFormule` | List\<String\> | Noms des variables utilisées |
| `valeursDefaut` | Map\<String, Double\> | Valeurs par défaut des paramètres |
| `prioriteCalcul` | int (défaut 1) | Ordre d'application en cas de cumul multi-garanties |
| `appliquerPlafondApresCalcul` | boolean (défaut true) | Ordre plafond/franchise |
| `deduireFranchiseAvantPlafond` | boolean (défaut false) | Idem |
| `baseConventionnee` | boolean (défaut false) | Base de calcul = tarif conventionné plutôt que montant réel |

**Méthode** : `calculerRemboursement(montant, taux, franchiseApplicable, plafondApplicable)` — implémentation simplifiée servant de fallback ; la logique métier complète vit dans le moteur de calcul applicatif.

---

## 5. Relations entre entités — détail

| Relation | Cardinalité | Portée par | Requête clé |
|----------|-------------|-----------|--------------|
| Produit → Pack | 1 – N | `Pack.produitId` | `PackUnifiedRepository.findByProduitId(produitId)` |
| Pack ↔ Garantie | N – N (via jonction) | `PackGarantie.packId` + `PackGarantie.garantieId` | `PackGarantieRepository.findByPackIdAndActifTrue(packId)` / `findByGarantieId(garantieId)` |
| Produit → CritereRecommandation | 1 – N | `CritereRecommandation.produitId` | `findByProduitIdAndActifTrue(produitId)` |
| Pack → CritereRecommandation | 0..1 – 1 | `CritereRecommandation.packId` (nullable) | `findByPackId(packId)` → `Optional` (un seul critère par pack) |
| Pack ↔ Pack | N – N (auto-référence) | `packsCompatibles` / `packsIncompatibles` / `optionsPackIds` | Listes d'IDs, non contraintes en base |
| Garantie ↔ Garantie | N – N (auto-référence) | `prerequisGarantieIds` | Liste d'IDs, non contrainte en base |

### Données dérivées (jamais stockées, calculées à la demande)

| Endpoint | DTO | Champs dérivés | Calcul |
|----------|-----|-----------------|--------|
| `GET /api/produits/{idProduit}/detail` | `ProduitDetailDTO` | `idPacks` | `PackUnifiedRepository.findByProduitId()` → liste des `idPack` |
| `GET /api/packs/{id}/detail` | `PackDetailDTO` | `idGaranties` | `PackGarantieRepository.findByPackIdAndActifTrue()` → liste des `garantieId` |
| `GET /api/packs/{id}/detail` | `PackDetailDTO` | `domainesMedicaux` | Enchaîne sur `GarantieRepository.findAllById(idGaranties)` → `Garantie::getDomaine`, dédupliqué |

Ces endpoints s'ajoutent aux endpoints CRUD existants sans les modifier (le chatbot et le frontend consomment déjà les endpoints de base tels quels).

---

## 6. Énumérations

### 6.1 DomaineMedical
*(dans `Garantie.domaine`)* — méthode utilitaire `fromString(text)` tolérant espaces/tirets/slashes.

| Catégorie | Valeurs |
|-----------|---------|
| Médecine générale | `CONSULTATION_GENERALE`, `MEDECINE_FAMILIALE`, `MEDECINE_INTERNE`, `MEDECINE_PREVENTIVE` |
| Spécialités médicales | `CARDIOLOGIE`, `DERMATOLOGIE`, `ENDOCRINOLOGIE`, `GASTRO_ENTEROLOGIE`, `HEMATOLOGIE`, `INFECTIOLOGIE`, `NEPHROLOGIE`, `NEUROLOGIE`, `ONCOLOGIE`, `PNEUMOLOGIE`, `RHUMATOLOGIE`, `ALLERGOLOGIE` |
| Santé de la femme | `GYNECOLOGIE`, `OBSTETRIQUE`, `FERTILITE_PMA`, `SUIVI_GROSSESSE` |
| Santé de l'enfant | `PEDIATRIE`, `NEONATOLOGIE`, `PEDOPSYCHIATRIE` |
| Santé mentale | `PSYCHIATRIE`, `PSYCHOLOGIE`, `PSYCHOTHERAPIE`, `ADDICTOLOGIE` |
| Chirurgie | `CHIRURGIE_GENERALE`, `CHIRURGIE_ESTHETIQUE`, `CHIRURGIE_ORTHOPEDIQUE`, `NEUROCHIRURGIE`, `CHIRURGIE_CARDIAQUE`, `CHIRURGIE_DIGESTIVE`, `CHIRURGIE_UROLOGIQUE` |
| Dentaire | `DENTISTERIE_GENERALE`, `ORTHODONTIE`, `IMPLANTOLOGIE`, `CHIRURGIE_DENTAIRE` |
| Vision et ORL | `OPHTALMOLOGIE`, `ORL`, `AUDIOLOGIE` |
| Rééducation et thérapies | `KINESITHERAPIE`, `PHYSIOTHERAPIE`, `ERGOTHERAPIE`, `ORTHOPHONIE`, `CHIROPRATIE`, `OSTEOPATHIE` |
| Examens et diagnostic | `RADIOLOGIE`, `IMAGERIE_MEDICALE`, `ANALYSES_BIOLOGIQUES`, `MEDECINE_NUCLEAIRE` |
| Médecines spécialisées | `MEDECINE_SPORT`, `MEDECINE_TRAVAIL`, `GERIATRIE`, `NUTRITION_DIETETIQUE`, `MEDECINE_ESTHETIQUE`, `MEDECINE_ALTERNATIVE` |
| Urgences et soins critiques | `URGENCES_MEDICALES`, `REANIMATION`, `SOINS_INTENSIFS` |
| Téléconsultation | `CONSULTATION_VIDEO`, `SUIVI_DISTANCE`, `DEUXIEME_AVIS_MEDICAL` |
| Compatibilité (historiques) | `HOSPITALISATION`, `DENTAIRE`, `OPTIQUE`, `PHARMACIE`, `MATERNITE`, `PREVENTION`, `SOINS_DENTAIRES`, `SOINS_OPTIQUES`, `HOSPITALISATION_CHIRURGICALE`, `HOSPITALISATION_MEDICALE`, `MEDECINE_GENERALE`, `SPECIALITES_MEDICALES`, `PARAMEDICAL`, `ANALYSES_MEDICALES`, `REPATRIEMENT`, `DECES`, `INVALIDITE`, `AUTRE` (défaut) |

### 6.2 TypeProduit
`SANTE`, `HABITATION`, `AUTO`, `EPARGNE`, `VIE`

### 6.3 StatutWorkflow
*(cycle éditorial : Produit, Pack, Garantie — seul mécanisme de statut du modèle)*
`BROUILLON`, `SOUMIS_VALIDATION`, `EN_COURS_VALIDATION`, `APPROUVE`, `REJETE`, `PUBLIE`, `ARCHIVE`, `SUSPENDU`

### 6.4 NiveauCouverture
*(Pack)* `BASIC`, `PREMIUM`, `GOLD`

### 6.5 CouvertureGeographique
*(Produit)* `LOCAL`, `NATIONAL`, `INTERNATIONAL`, `UE`, `MAGHREB`

### 6.6 TypePlafond
*(PlafondGarantie.typePrincipal)* `PAR_ACTE`, `ANNUEL`, `MENSUEL`, `GLOBAL`, `PAR_SOINS`

### 6.7 TypeFranchise
*(FranchiseGarantie.type)* `AUCUNE`, `FIXE`, `POURCENTAGE`, `RELATIVE`, `ABSOLUE`

### 6.8 TypeRemboursement
*(Garantie)* `FRAIS_REELS`, `FORFAIT`, `TARIF_CONVENTIONNE`, `CAPITAL_DECES`, `INDEMNITE_JOURNALIERE`, `RENTE_VIAGERE`, `RENTE_EDUCATION`, `VALEUR_A_NEUF`, `VALEUR_VENALE` — `estRemboursementVariable()` = FRAIS_REELS, TARIF_CONVENTIONNE ou VALEUR_VENALE

### 6.9 TypeMontant
*(PackGarantie.typeMontant)* `FORFAIT`, `FRAIS_REELS`, `TARIF_CONVENTIONNE`

---

## 7. Collections MongoDB

| Collection | Entité | Notes |
|------------|--------|-------|
| `produits` | Produit | Agrégat racine du catalogue |
| `packs` | Pack | |
| `garanties` | Garantie | Indépendante de tout pack |
| `pack_garanties` | PackGarantie | Table de jonction enrichie, index unique (packId, garantieId) |
| `criteres_recommandation` | CritereRecommandation | Un seul document par pack |

`PlafondGarantie`, `FranchiseGarantie` et `RegleCalcul` n'ont **pas** de collection — ce sont des sous-documents embedded.

---

## 8. Conventions de code

Conventions appliquées dans `GestionProduit` (Java 21 / Spring Boot 3) :

| Élément | Convention | Exemple |
|---------|-----------|---------|
| Packages | minuscule, structure `tn.vermeg.gestionproduit.<couche>` | `entities`, `dto`, `services`, `controllers`, `repositories`, `enums`, `config` |
| Entités | PascalCase, singulier | `Produit`, `Pack`, `Garantie`, `PackGarantie` |
| DTO | Nom entité + `DTO` | `ProduitDetailDTO`, `PackDetailDTO` |
| Services / Controllers / Repositories | Nom entité + suffixe | `ProduitService`, `GarantieController`, `PackUnifiedRepository` |
| Méthodes CRUD | `create/get/update/delete` + Entité | `createProduit`, `getAllProduits`, `updatePack` |
| Méthodes de recherche | `findBy` + critère | `findByProduitId`, `findByDomaine` |
| Méthodes booléennes | `est`/`is`/`has` + adjectif | `estActif()`, `estValide()`, `estCommercialisable()` |
| Variables / champs | camelCase | `nomProduit`, `prixMensuel`, `tauxRemboursementBase` |
| Constantes | UPPER_SNAKE_CASE | `DEFAULT_VALUE`, `MAX_RETRIES` |
| Endpoints REST | `/api/<ressource-pluriel>` | `/api/produits`, `/api/packs`, `/api/garanties` |
| Annotations validation | Jakarta Validation | `@NotNull`, `@NotBlank`, `@Min`/`@Max`, `@Valid` |

Formatage : indentation 4 espaces, lignes ≤ 120 caractères, imports ordonnés (Java standard → Jakarta/Spring → `tn.vermeg.gestionproduit` → imports statiques), Javadoc sur les classes et méthodes publiques.

---

## 9. Genèse du modèle

Le modèle a traversé 3 refontes successives. Comprendre cette trajectoire aide à ne pas réintroduire des champs déjà écartés délibérément.

**v1 (modèle plat initial)** — Produit à 5 champs sans information commerciale ; `Garantie.packId` couplait une garantie à un seul pack (alors qu'une même garantie peut apparaître dans plusieurs formules avec des taux différents) ; plafond et franchise étaient de simples `double` sans logique de calcul associée ; aucun cycle de vie (workflow), aucun versioning, aucune règle de recommandation éditable.

**v2 (refonte DDD)** — Introduction des Value Objects `PlafondGarantie`, `FranchiseGarantie`, `RegleCalcul` (avec méthodes de calcul), du cycle de vie `StatutWorkflow`, et de la table de jonction enrichie `PackGarantie` (surcharge taux/plafond/franchise par pack). En parallèle, le modèle s'est aussi étendu vers la **gestion administrative du contrat** : versioning réglementaire (`VersionProduit`), promotions (`OffreCommerciale`), critères d'éligibilité de souscription (`CritereEligibilite`), exclusions (`Exclusion`), pièces justificatives (`DocumentRequis`), canaux de distribution, délais de carence/résiliation, etc.

**v2.1 et v2.2 (recentrage sur le catalogue)** — Deux passes de simplification ont retiré tout ce qui relevait de la gestion de contrat ou de la souscription plutôt que du catalogue produit lui-même : `VersionProduit`, `OffreCommerciale`, `DocumentRequis`, `Exclusion` et `CritereEligibilite` ont été supprimés entièrement (entité, repository, service, controller), avec leurs enums associés (`TypeRenouvellement`, `TypeResiliation`, `CanalDistribution`, `TypeDocument`, `FrequenceRemboursement`, `TypeOffre`, `TypeExclusion`, `TypeClient`, `NiveauRisque`). Le champ `statut` (ACTIF/INACTIF), devenu redondant avec `StatutWorkflow`, a été retiré de `Produit`/`Pack`/`Garantie` — partout où il conditionnait une règle métier, la condition a été réécrite sur `statutWorkflow == PUBLIE`.

**Décision de périmètre qui en résulte** : `GestionProduit` décrit la **relation d'assurance** — quel produit, quelle formule, quelles garanties, à quel tarif — pas la gestion administrative des contrats ni la souscription. C'est ce modèle final, stable, que documentent les sections 1 à 7 ci-dessus.

**Effets de bord notables** :
- Le moteur de recommandation Python (`recommendation_service.py`) a perdu 2 de ses 5 critères de score (`eligibilite`, `type_client`, dérivés de `criteresEligibilite`) ; les 3 critères restants ont été rééquilibrés pour retotaliser 1.0, et le filtre "packs actifs" est passé de `statut == "ACTIF"` à `statut_workflow == "PUBLIE"`.
- Le frontend Angular a perdu tous les badges/filtres/formulaires liés au statut ACTIF/INACTIF et aux critères d'éligibilité (âge, type de client, ancienneté, couverture géographique).
- `Produit.couvertureGeographique` et `Produit.territoiresExclus` (champs directs de `Produit`, distincts de l'ancien `criteresEligibilite` de `Pack`) ont été conservés.

---

*Document généré pour le microservice GestionProduit — reflète le modèle simplifié centré sur la relation d'assurance (produit/pack/garantie), sans paramètres de gestion de contrat ni critères d'éligibilité de souscription.*
