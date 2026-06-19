# Documentation des Structures de Prompts - Chatbot Gestion Produit

## Table des Matières
1. [Vue d'ensemble du système](#vue-densemble-du-système)
2. [Fonctionnalités supportées](#fonctionnalités-supportées)
3. [Détection des intentions](#détection-des-intentions)
4. [Structures de prompts par fonctionnalité](#structures-de-prompts-par-fonctionnalité)
   - [Création de Produits](#création-de-produits)
   - [Création de Packs](#création-de-packs)
   - [Création de Garanties](#création-de-garanties)
   - [Configuration de Packs](#configuration-de-packs)
   - [Ajout de Garanties aux Packs](#ajout-de-garanties-aux-packs)
   - [Recommandations](#recommandations)
   - [Consultation](#consultation)
   - [Modification](#modification)
   - [Suppression](#suppression)
5. [Règles de validation](#règles-de-validation)
6. [Bonnes pratiques](#bonnes-pratiques)

---

## Vue d'ensemble du système

Le chatbot utilise une architecture à plusieurs niveaux pour traiter les prompts utilisateurs :

- **Niveau 1 (100% confiance)** : Détection de phrases explicites prédéfinies
- **Niveau 2 (80% confiance)** : Détection par combinaison action + entité
- **Niveau 3 (60% confiance)** : Détection par entité seule avec priorisation

Le système utilise deux méthodes d'extraction :
1. **Extraction IA** (Google Gemini) : Analyse intelligente du langage naturel
2. **Fallback Regex** : Extraction par motifs regex si l'IA n'est pas disponible

---

## Fonctionnalités supportées

| Action | Description | Statut |
|--------|-------------|--------|
| `GARANTIE` | Création d'une garantie | ✅ Implémenté |
| `PRODUIT` | Création d'un produit | ✅ Implémenté |
| `PACK` | Création d'un pack | ✅ Implémenté |
| `CONFIGURATION_PACK` | Configuration d'un pack existant | ✅ Implémenté |
| `AJOUT_GARANTIE_PACK` | Ajout d'une garantie à un pack | ✅ Implémenté |
| `RECOMMANDATION` | Recommandation intelligente de packs | ✅ Implémenté |
| `CONSULTATION_GARANTIE` | Consultation des garanties | ⚠️ Défini, non implémenté |
| `CONSULTATION_PRODUIT` | Consultation des produits | ⚠️ Défini, non implémenté |
| `CONSULTATION_PACK` | Consultation des packs | ⚠️ Défini, non implémenté |
| `MODIFICATION_GARANTIE` | Modification d'une garantie | ⚠️ Défini, non implémenté |
| `MODIFICATION_PRODUIT` | Modification d'un produit | ⚠️ Défini, non implémenté |
| `MODIFICATION_PACK` | Modification d'un pack | ⚠️ Défini, non implémenté |
| `SUPPRESSION_GARANTIE` | Suppression d'une garantie | ⚠️ Défini, non implémenté |
| `SUPPRESSION_PRODUIT` | Suppression d'un produit | ⚠️ Défini, non implémenté |
| `SUPPRESSION_PACK` | Suppression d'un pack | ⚠️ Défini, non implémenté |

---

## Détection des intentions

### Mots-clés par catégorie

**Produit** : `produit`, `product`, `assurance-produit`, `formule principale`

**Garantie** : `garantie`, `warranty`, `couverture`, `assurance`, `protection`

**Pack** : `pack`, `package`, `offre`, `formule`

**Recommandation** : `recommand`, `recommend`, `conseil`, `adapter`, `adapté`, `correspond`, `correspondant`, `cherche`, `recherche`, `besoin`, `besoins`, `idéal`, `ideal`, `meilleur`, `meilleure`, `top`, `top 3`, `top3`, `suggérer`, `suggere`, `proposer`, `propose`

**Création** : `créer`, `create`, `ajouter`, `add`, `nouveau`, `new`, `génération`, `générer`

**Configuration** : `configurer`, `configure`, `configuration`, `paramétrer`, `setting`, `modifier`, `update`

**Ajout** : `ajout`, `ajouter`, `add`, `intégrer`, `integration`

---

## Structures de prompts par fonctionnalité

### Création de Produits

**Intention détectée** : `PRODUIT`

**Structure générale** :
```
[verbe de création] + [mot-clé produit] + [nom du produit] + [type de produit] + [description] + [statut]
```

**Paramètres** :
- **Obligatoires** : `nomProduit`
- **Facultatifs** : `description`, `typeProduit`, `statut`

**Types de produits supportés** :
- `SANTE` : santé, sante
- `AUTO` : auto, voiture, véhicule
- `HABITATION` : habitation, logement, maison
- `VIE` : vie, décès, survie
- `PREVOYANCE` : prévoyance, prevoyance, prévision
- `EPARGNE` : épargne, epargne, investissement

**Statuts supportés** : `ACTIF`, `INACTIF` (défaut: ACTIF)

**Exemples de prompts valides** :
```
- "Créer un produit d'assurance santé nommé 'Santé Premium'"
- "Créer un produit nommé 'Produit Assurance Santé Minimum', avec la description 'Couverture médicale complète pour particuliers et familles', de type SANTE et avec le statut ACTIF"
- "Nouveau produit d'assurance auto appelé 'Auto Sécurité'"
- "Ajouter un produit d'assurance habitation nommé 'Habitation Confort'"
- "Create a product named 'Life Insurance Premium' with type VIE"
- "Produit d'assurance vie nommé 'Vie Protection'"
```

**Règles de validation** :
- Le nom du produit doit être unique (détection par business hash)
- Le type de produit doit correspondre à une valeur de l'énumération
- Le statut par défaut est ACTIF si non spécifié

---

### Création de Packs

**Intention détectée** : `PACK`

**Structure générale** :
```
[verbe de création] + [mot-clé pack] + [nom du pack] + [paramètres du pack] + [liaison produit] + [garanties]
```

**Paramètres** :
- **Obligatoires** : `nomPack`, `nomProduit` (ou `produitId`)
- **Facultatifs** : `description`, `ageMinimum`, `ageMaximum`, `typeClients`, `ancienneteContratMois`, `couvertureGeographique`, `prixMensuel`, `dureeMinContrat`, `dureeMaxContrat`, `niveauCouverture`, `statut`, `garanties`

**Types de clients supportés** :
- `INDIVIDUEL` : individu, individuel
- `FAMILLE` : famille, familiale, enfants
- `SENIOR` : senior, 60 ans
- `ENTREPRISE` : entreprise

**Couvertures géographiques** : `INTERNATIONAL`, `UE`, `MAGHREB`, `NATIONAL`, `REGIONAL`

**Niveaux de couverture** : `BASIC`, `PREMIUM`, `GOLD`

**Exemples de prompts valides** :
```
- "Créer un pack Gold lié au produit Santé Premium avec les garanties hospitalisation et dentaire"
- "Créer un pack santé premium nommé 'Santé Premium Gold' pour un produit SANTE intitulé 'Produit Assurance Santé Minimum'. Le pack doit respecter les conditions suivantes : un âge minimum de 18 ans et un âge maximum de 70 ans, un type de clients FAMILLE et INDIVIDUEL, une ancienneté minimale de 0 mois, une couverture géographique INTERNATIONAL, un prix mensuel de 120, une durée de contrat comprise entre 12 et 60 mois, un niveau de couverture GOLD et un statut ACTIF"
- "Nouveau pack Silver pour le produit Santé Premium avec prix mensuel 50€"
- "Créer un pack Famille Santé lié au produit Santé Premium, pour les familles, avec couverture dentaire et optique"
- "Pack Senior Gold pour produit Santé Premium, âge 60-80 ans, prix 80€"
- "Créer un pack nommé 'Pack Essentiel' avec niveau de couverture BASIC, type de client INDIVIDUEL, prix mensuel 30€"
```

**Règles de validation** :
- Le nom du pack doit être unique par produit (détection par business hash)
- Le produit doit exister dans la base de données
- L'âge minimum doit être inférieur à l'âge maximum
- Le prix mensuel doit être un nombre positif
- Les garanties mentionnées doivent exister dans la base de données
- Valeurs par défaut appliquées si non spécifiées :
  - ageMinimum: 0
  - ageMaximum: 120
  - typeClients: INDIVIDUEL
  - ancienneteContratMois: 0
  - dureeMinContrat: 12
  - dureeMaxContrat: 60
  - statut: ACTIF

---

### Création de Garanties

**Intention détectée** : `GARANTIE`

**Structure générale** :
```
[verbe de création] + [mot-clé garantie] + [nom de la garantie] + [type] + [domaine] + [paramètres de la garantie]
```

**Paramètres** :
- **Obligatoires** : `nomGarantie`
- **Facultatifs** :  `domaine`, `description`, `tauxRemboursement`, `typeMontant`, `plafondAnnuel`, `plafondMensuel`, `plafondParActe`, `franchise`, `coutMoyenParSinistre`, `dureeMinContrat`, `dureeMaxContrat`, `resiliableAnnuellement`, `statut`

**Note importante** : Il y a deux champs distincts :
- **`domaine`** (DomaineMedical enum) : Domaine médical précis (voir liste complète ci-dessous)

**Domaines médicaux (champ `domaine` - 60+ valeurs)** :

**Médecine générale** :
- `CONSULTATION_GENERALE` : Consultation générale
- `MEDECINE_FAMILIALE` : Médecine familiale
- `MEDECINE_INTERNE` : Médecine interne
- `MEDECINE_PREVENTIVE` : Médecine préventive

**Spécialités médicales** :
- `CARDIOLOGIE` : Cardiologie
- `DERMATOLOGIE` : Dermatologie
- `ENDOCRINOLOGIE` : Endocrinologie
- `GASTRO_ENTEROLOGIE` : Gastro-entérologie
- `HEMATOLOGIE` : Hématologie
- `INFECTIOLOGIE` : Infectiologie
- `NEPHROLOGIE` : Néphrologie
- `NEUROLOGIE` : Neurologie
- `ONCOLOGIE` : Oncologie
- `PNEUMOLOGIE` : Pneumologie
- `RHUMATOLOGIE` : Rhumatologie
- `ALLERGOLOGIE` : Allergologie

**Santé de la femme** :
- `GYNECOLOGIE` : Gynécologie
- `OBSTETRIQUE` : Obstétrique
- `FERTILITE_PMA` : Fertilité / PMA
- `SUIVI_GROSSESSE` : Suivi grossesse

**Santé de l'enfant** :
- `PEDIATRIE` : Pédiatrie
- `NEONATOLOGIE` : Néonatologie
- `PEDOPSYCHIATRIE` : Pédopsychiatrie

**Santé mentale** :
- `PSYCHIATRIE` : Psychiatrie
- `PSYCHOLOGIE` : Psychologie
- `PSYCHOTHERAPIE` : Psychothérapie
- `ADDICTOLOGIE` : Addictologie

**Chirurgie** :
- `CHIRURGIE_GENERALE` : Chirurgie générale
- `CHIRURGIE_ESTHETIQUE` : Chirurgie esthétique
- `CHIRURGIE_ORTHOPEDIQUE` : Chirurgie orthopédique
- `NEUROCHIRURGIE` : Neurochirurgie
- `CHIRURGIE_CARDIAQUE` : Chirurgie cardiaque
- `CHIRURGIE_DIGESTIVE` : Chirurgie digestive
- `CHIRURGIE_UROLOGIQUE` : Chirurgie urologique

**Dentaire** :
- `DENTISTERIE_GENERALE` : Dentisterie générale
- `ORTHODONTIE` : Orthodontie
- `IMPLANTOLOGIE` : Implantologie
- `CHIRURGIE_DENTAIRE` : Chirurgie dentaire

**Vision et ORL** :
- `OPHTALMOLOGIE` : Ophtalmologie
- `ORL` : ORL
- `AUDIOLOGIE` : Audiologie

**Rééducation et thérapies** :
- `KINESITHERAPIE` : Kinésithérapie
- `PHYSIOTHERAPIE` : Physiothérapie
- `ERGOTHERAPIE` : Ergothérapie
- `ORTHOPHONIE` : Orthophonie
- `CHIROPRATIE` : Chiropractie
- `OSTEOPATHIE` : Ostéopathie

**Examens et diagnostic** :
- `RADIOLOGIE` : Radiologie
- `IMAGERIE_MEDICALE` : Imagerie médicale
- `ANALYSES_BIOLOGIQUES` : Analyses biologiques
- `MEDECINE_NUCLEAIRE` : Médecine nucléaire

**Médecines spécialisées** :
- `MEDECINE_SPORT` : Médecine du sport
- `MEDECINE_TRAVAIL` : Médecine du travail
- `GERIATRIE` : Gériatrie
- `NUTRITION_DIETETIQUE` : Nutrition / Diététique
- `MEDECINE_ESTHETIQUE` : Médecine esthétique
- `MEDECINE_ALTERNATIVE` : Médecine alternative

**Urgences et soins critiques** :
- `URGENCES_MEDICALES` : Urgences médicales
- `REANIMATION` : Réanimation
- `SOINS_INTENSIFS` : Soins intensifs

**Téléconsultation** :
- `CONSULTATION_VIDEO` : Consultation vidéo
- `SUIVI_DISTANCE` : Suivi à distance
- `DEUXIEME_AVIS_MEDICAL` : Deuxième avis médical international

**Domaines existants conservés pour compatibilité** :
- `HOSPITALISATION` : Hospitalisation
- `DENTAIRE` : Dentaire
- `OPTIQUE` : Optique
- `PHARMACIE` : Pharmacie
- `MATERNITE` : Maternité
- `PREVENTION` : Prévention
- `SOINS_DENTAIRES` : Soins dentaires
- `SOINS_OPTIQUES` : Soins optiques
- `HOSPITALISATION_CHIRURGICALE` : Hospitalisation chirurgicale
- `HOSPITALISATION_MEDICALE` : Hospitalisation médicale
- `MEDECINE_GENERALE` : Médecine générale
- `SPECIALITES_MEDICALES` : Spécialités médicales
- `PARAMEDICAL` : Paramédical
- `ANALYSES_MEDICALES` : Analyses médicales
- `REPATRIEMENT` : Rapatriement
- `DECES` : Décès
- `INVALIDITE` : Invalidité

**Autre** :
- `AUTRE` : Autre (pour les nouveaux types non classés)

**Types de montants** : `FRAIS_REELS`, `TARIF_CONVENTIONNE`, `FORFAITAIRE`

**Exemples de prompts valides** :
```
- "Créer une garantie hospitalisation premium active avec un remboursement de 90% sur les frais réels, un plafond annuel de 50000, un plafond mensuel de 10000 et un plafond par acte de 5000, avec une franchise de 100, un coût moyen par sinistre de 2000, une durée de contrat comprise entre 12 et 60 mois, résiliable annuellement"
- "Créer une garantie dentaire avec un taux de remboursement de 80%, plafond annuel 2000€, franchise 20€"
- "Nouvelle garantie optique nommée 'Optique Premium' avec remboursement 90% et plafond annuel 500€"
- "Ajouter une garantie consultation médicale avec taux de remboursement 70%"
- "Créer une garantie hospitalisation avec remboursement 100%, sans franchise"
- "Garantie médicament nommée 'Pharmacie Confort' avec plafond annuel 1000€ et taux 80%"
- "Créer une garantie kinésithérapie avec domaine KINESITHERAPIE, type AUTRE, remboursement 85%"
- "Garantie cardiologie nommée 'Cœur Sain' avec domaine CARDIOLOGIE, type CONSULTATION, taux 90%"
```

**Règles de validation** :
- Le nom de la garantie doit être unique (détection par business hash basé sur nom + type)
- Le taux de remboursement doit être entre 0 et 1 (ex: 0.8 pour 80%)
- Les plafonds doivent être des nombres positifs
- Le domaine médical doit correspondre à une valeur de l'énumération DomaineMedical
- Valeurs par défaut appliquées si non spécifiées :
  - tauxRemboursement: 0.8 (80%)
  - typeMontant: FRAIS_REELS
  - coutMoyenParSinistre: 100.0
  - dureeMinContrat: 12
  - dureeMaxContrat: 60
  - resiliableAnnuellement: true
  - statut: ACTIF
  - franchise: 0.0
  - Les plafonds dérivés sont calculés automatiquement :
    - plafondMensuel = plafondAnnuel / 12
    - plafondParActe = plafondAnnuel / 24

---

### Configuration de Packs

**Intention détectée** : `CONFIGURATION_PACK`

**Structure générale** :
```
[verbe de configuration] + [mot-clé pack] + [nom du pack] + [paramètres de configuration]
```

**Paramètres** :
- **Obligatoires** : `packId` ou `nomPack`, `garantieId` ou `nomGarantie`
- **Facultatifs** : `tauxRemboursement`, `plafond`, `franchise`, `optionnelle`, `supplementPrix`

**Exemples de prompts valides** :
```
- "Configurer le pack Silver avec les garanties optique et dentaire"
- "Configurer le pack Gold avec la garantie hospitalisation à 90% de remboursement"
- "Configuration du pack Silver : ajouter garantie dentaire avec franchise 20€"
- "Configurer le pack Famille avec la garantie consultation médicale"
```

**Règles de validation** :
- Le pack doit exister dans la base de données
- La garantie doit exister dans la base de données
- Le taux de remboursement doit être entre 0 et 1

---

### Ajout de Garanties aux Packs

**Intention détectée** : `AJOUT_GARANTIE_PACK`

**Structure générale** :
```
[verbe d'ajout] + [mot-clé garantie] + [mot-clé pack] + [noms]
```

**Paramètres** :
- **Obligatoires** : `nomPack`, `nomGarantie`
- **Facultatifs** : `tauxRemboursement`, `plafond`, `franchise`, `optionnelle`

**Exemples de prompts valides** :
```
- "Ajouter la garantie optique au pack Silver"
- "Ajouter la garantie dentaire au pack Gold avec un taux de remboursement de 90%"
- "Ajouter la garantie hospitalisation au pack Premium avec franchise 50€"
- "Intégrer la garantie consultation médicale dans le pack Famille"
```

**Règles de validation** :
- Le pack doit exister dans la base de données
- La garantie doit exister dans la base de données
- Si le taux de remboursement n'est pas spécifié, celui de la garantie est utilisé par défaut

---

### Recommandations

**Intention détectée** : `RECOMMANDATION`

**Structure générale** :
```
[mot-clé recommandation] + [profil client] + [critères]
```

**Paramètres** :
- **Obligatoires** : Aucun (système fonctionne avec profil minimal)
- **Facultatifs** : `age`, `gender`, `maritalStatus`, `numberOfChildren`, `monthlyBudget`, `chronicDiseases`

**Exemples de prompts valides** :
```
- "Je suis un homme de 45 ans, marié, avec deux enfants. Je recherche un pack adapté à ma famille avec couverture dentaire et kinésithérapie"
- "Recommande-moi un pack pour une femme de 35 ans, célibataire, sans enfants, budget 50€ par mois"
- "Je cherche le meilleur pack pour ma famille : marié, 2 enfants, budget 100€/mois"
- "Conseillez-moi un pack adapté à mon profil : 50 ans, senior, avec diabète et hypertension"
- "Quel pack me convient le mieux ? J'ai 40 ans, marié, 3 enfants, budget 120€"
- "Recommandation pour homme 60 ans, célibataire, budget 80€, avec arthrite"
- "Je suis une femme de 28 ans, célibataire, sans enfants. Je cherche une couverture santé adaptée avec budget 40€"
```

**Règles de validation** :
- Le système extrait automatiquement les informations du profil
- Si plusieurs profils sont détectés, seul le premier est traité
- Le niveau de couverture est déduit du budget :
  - Budget < 50€ : BASIC
  - Budget 50-100€ : BASIC
  - Budget > 100€ : PREMIUM
- Le système retourne les 3 meilleurs packs par score de compatibilité

**Informations extraites** :
- **Âge** : "je ai X ans", "âge de X", "X ans"
- **Genre** : "homme", "femme", "marié", "mariée"
- **Statut marital** : "marié", "mariée", "célibataire"
- **Nombre d'enfants** : "X enfants", "X fils", "X filles"
- **Budget mensuel** : "budget X€", "X euros par mois"
- **Maladies chroniques** : diabète, hypertension, cholestérol, asthme, arthrite, cancer, etc.

---

### Consultation

**Intention détectée** : `CONSULTATION_PRODUIT`, `CONSULTATION_PACK`, `CONSULTATION_GARANTIE`

**Note** : Ces actions sont définies dans l'énumération mais ne sont pas encore implémentées dans le service d'orchestration.

**Structure générale** :
```
[verbe de consultation] + [mot-clé entité] + [critères de recherche]
```

**Mots-clés de consultation** : `consulter`, `voir`, `liste`, `afficher`, `montrer`

**Exemples de prompts (non implémentés)** :
```
- "Voir tous les produits"
- "Liste des packs disponibles"
- "Consulter les garanties actives"
- "Afficher les produits de type SANTE"
- "Montrer les packs avec niveau de couverture GOLD"
```

---

### Modification

**Intention détectée** : `MODIFICATION_PRODUIT`, `MODIFICATION_PACK`, `MODIFICATION_GARANTIE`

**Note** : Ces actions sont définies dans l'énumération mais ne sont pas encore implémentées dans le service d'orchestration.

**Structure générale** :
```
[verbe de modification] + [mot-clé entité] + [nom] + [modifications]
```

**Mots-clés de modification** : `modifier`, `update`, `changer`, `mettre à jour`

**Exemples de prompts (non implémentés)** :
```
- "Modifier le produit Santé Premium pour changer le statut en INACTIF"
- "Mettre à jour le pack Gold avec un nouveau prix de 150€"
- "Changer la garantie dentaire pour un taux de remboursement de 95%"
```

---

### Suppression

**Intention détectée** : `SUPPRESSION_PRODUIT`, `SUPPRESSION_PACK`, `SUPPRESSION_GARANTIE`

**Note** : Ces actions sont définies dans l'énumération mais ne sont pas encore implémentées dans le service d'orchestration.

**Structure générale** :
```
[verbe de suppression] + [mot-clé entité] + [nom]
```

**Mots-clés de suppression** : `supprimer`, `delete`, `retirer`, `effacer`

**Exemples de prompts (non implémentés)** :
```
- "Supprimer le produit Santé Premium"
- "Delete the pack Gold"
- "Retirer la garantie dentaire"
```

---

## Règles de validation

### Validation des prompts
- Le prompt ne doit pas être vide
- Le prompt doit contenir au moins une action détectable
- L'action doit être supportée par le système

### Validation des données extraites
- Les champs obligatoires doivent être présents
- Les types de données doivent correspondre (nombres pour les montants, etc.)
- Les valeurs d'énumération doivent être valides
- Les relations entre entités doivent exister (produit, pack, garantie)

### Validation métier
- Détection des doublons via business hash
- Cohérence des données (âge min < âge max, etc.)
- Application des valeurs par défaut si non spécifiées
- Validation des scores de confiance d'extraction

---

## Bonnes pratiques

### 1. Soyez explicite
Utilisez des phrases complètes avec des verbes d'action clairs :
```
✅ "Créer un pack nommé 'Gold Pack' avec prix 100€"
❌ "Gold Pack 100€"
```

### 2. Spécifiez les unités
Indiquez toujours les unités pour les valeurs numériques :
```
✅ "Prix mensuel de 50€"
✅ "Durée de 12 mois"
❌ "Prix 50"
❌ "Durée 12"
```

### 3. Utilisez des guillemets pour les noms
Encadrez les noms propres avec des guillemets pour éviter les ambiguïtés :
```
✅ "Créer un produit nommé 'Santé Premium'"
❌ "Créer un produit Santé Premium"
```

### 4. Soyez précis avec les pourcentages
Indiquez clairement les pourcentages :
```
✅ "Taux de remboursement de 80%"
✅ "Remboursement à 90%"
❌ "Remboursement 80"
```

### 5. Regroupez les informations connexes
Structurez votre prompt de manière logique :
```
✅ "Créer un pack Famille avec âge 18-65 ans, prix 80€, couverture dentaire et optique"
❌ "Créer un pack 80€ 18-65 ans dentaire optique Famille"
```

### 6. Utilisez le vocabulaire du domaine
Employez les termes techniques appropriés :
```
✅ "Couverture géographique INTERNATIONAL"
✅ "Niveau de couverture PREMIUM"
✅ "Type de client FAMILLE"
```

### 7. Évitez les ambiguïtés
Si plusieurs entités partagent des mots-clés, soyez spécifique :
```
✅ "Créer une garantie dentaire pour le pack Gold"
❌ "Créer dentaire pour Gold"
```

### 8. Profitez de l'extraction IA
Le système utilise Google Gemini pour l'extraction intelligente, donc vous pouvez utiliser un langage naturel :
```
✅ "Je voudrais créer un nouveau pack d'assurance santé pour les familles avec un budget autour de 100€ par mois, incluant la couverture dentaire et optique"
```

### 9. Pour les garanties, précisez type et domaine
Pour les garanties, vous pouvez spécifier à la fois le type (général) et le domaine (précis) :
```
✅ "Créer une garantie de type CONSULTATION avec domaine CARDIOLOGIE"
✅ "Garantie type HOSPITALISATION avec domaine HOSPITALISATION_MEDICALE"
```

---

## Endpoint API

**URL** : `POST /api/chatbot/process`

**Corps de la requête** :
```json
{
  "prompt": "Votre prompt ici",
  "sessionId": "session-123 (optionnel)"
}
```

**Réponse** :
```json
{
  "success": true,
  "action": "PACK",
  "message": "Pack créé avec succès",
  "result": { ... },
  "data": {
    "sessionId": "session-123",
    "extractionConfidence": 0.95,
    "normalizedPrompt": "...",
    "recognizedEnums": [...]
  }
}
```

---

## Notes importantes

1. **Session ID** : Le sessionId permet de maintenir le contexte conversationnel. S'il n'est pas fourni, un nouveau est généré automatiquement.

2. **Confiance d'extraction** : Le système fournit un score de confiance pour l'extraction des données. Un score élevé (> 0.8) indique une extraction fiable.

3. **Valeurs par défaut** : Le système applique intelligemment des valeurs par défaut pour les champs non spécifiés, tout en laissant les champs critiques (comme le prix ou la couverture géographique) sans valeur par défaut pour demander confirmation si nécessaire.

4. **Détection de doublons** : Le système utilise un "business hash" pour détecter les doublons potentiels et éviter les créations en double.

5. **Mode fallback** : Si l'API Google Gemini n'est pas disponible, le système utilise automatiquement des motifs regex pour l'extraction des données.

6. **RAG (Retrieval-Augmented Generation)** : Le système utilise RAG pour rechercher des documents pertinents et enrichir le contexte avant de répondre.

7. **Domaines médicaux** : L'énumération DomaineMedical contient plus de 60 valeurs organisées en catégories. Utilisez les noms exacts de l'énumération pour une meilleure précision.

8. **Type vs Domaine** : Pour les garanties, le champ `type` est une chaîne de caractères générale (ex: HOSPITALISATION, DENTAIRE) tandis que `domaine` est une énumération précise (ex: HOSPITALISATION_MEDICALE, CARDIOLOGIE).

---

## Conclusion

Ce chatbot est conçu pour comprendre le langage naturel tout en fournissant une structure claire pour les prompts. Pour les meilleurs résultats, suivez les bonnes pratiques décrites ci-dessus et utilisez des phrases complètes et explicites.

Le système continuera d'évoluer avec l'ajout de nouvelles fonctionnalités (consultation, modification, suppression) et l'amélioration de l'extraction IA.
