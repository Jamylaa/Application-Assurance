import re
import logging
from typing import Dict, Any, Optional, List
from app.models.enums import Statut, TypeProduit, TypeMontant, DomaineMedical, NiveauCouverture, CouvertureGeographique, TypeClient, TypePlafond
from app.models.schemas import GarantieDTO, PackDTO, ProduitDTO, PackGarantieDTO

logger = logging.getLogger(__name__)

class PromptParserService:
    """Service for parsing natural language prompts with rule-based fallback."""
    
    def __init__(self):
        self.logger = logger
    
    def parse_produit_prompt(self, prompt: str) -> Optional[ProduitDTO]:
        """Parse a product creation prompt using rule-based patterns."""
        try:
            # Extract product name
            name_pattern = r'créer un produit nommé\s+["\']?([^"\',.]+)["\']?|créer un produit\s+["\']?([^"\',.]+)["\']?'
            name_match = re.search(name_pattern, prompt, re.IGNORECASE)
            if not name_match:
                return None
            
            nom_produit = name_match.group(1) or name_match.group(2)
            nom_produit = nom_produit.strip()
            
            # Extract description
            desc_pattern = r'avec la description\s+["\']?([^"\',.]+(?:[^"\',.]*[^"\',.])?)["\']?|description\s+["\']?([^"\',.]+(?:[^"\',.]*[^"\',.])?)["\']?'
            desc_match = re.search(desc_pattern, prompt, re.IGNORECASE)
            description = desc_match.group(1) or desc_match.group(2) if desc_match else None
            
            # Extract type
            type_produit = self._extract_type_produit(prompt)
            
            # Extract status
            statut = self._extract_statut(prompt)
            
            produit_dto = ProduitDTO(
                nom_produit=nom_produit,
                description=description,
                type_produit=type_produit,
                statut=statut
            )
            
            logger.info(f"✅ Parsed product: {nom_produit}, type: {type_produit}, status: {statut}")
            return produit_dto
            
        except Exception as e:
            logger.error(f"❌ Error parsing product prompt: {e}")
            return None
    
    def parse_pack_prompt(self, prompt: str) -> Optional[PackDTO]:
        """Parse a pack creation prompt using rule-based patterns."""
        try:
            # Extract pack name
            name_pattern = r'créer un pack nommé\s+["\']?([^"\',.]+)["\']?|créer un pack\s+["\']?([^"\',.]+)["\']?'
            name_match = re.search(name_pattern, prompt, re.IGNORECASE)
            if not name_match:
                return None
            
            nom_pack = name_match.group(1) or name_match.group(2)
            nom_pack = nom_pack.strip()
            
            # Extract product name
            produit_pattern = r'pour le produit\s+["\']?([^"\',.]+)["\']?|associé au produit\s+["\']?([^"\',.]+)["\']?|produit\s+["\']?([^"\',.]+)["\']?'
            produit_match = re.search(produit_pattern, prompt, re.IGNORECASE)
            nom_produit = produit_match.group(1) or produit_match.group(2) or produit_match.group(3) if produit_match else None
            
            # Extract description
            desc_pattern = r'avec pour description\s+["\']?([^"\',.]+(?:[^"\',.]*[^"\',.])?)["\']?|description\s+["\']?([^"\',.]+(?:[^"\',.]*[^"\',.])?)["\']?'
            desc_match = re.search(desc_pattern, prompt, re.IGNORECASE)
            description = desc_match.group(1) or desc_match.group(2) if desc_match else None
            
            # Extract age range
            age_min = self._extract_number(prompt, r'âge minimum de\s+(\d+)\s+ans?|age minimum de\s+(\d+)\s+ans?')
            age_max = self._extract_number(prompt, r'âge maximum de\s+(\d+)\s+ans?|age maximum de\s+(\d+)\s+ans?')
            
            # Extract client type
            type_client = self._extract_type_client(prompt)
            
            # Extract anciennete
            anciennete = self._extract_number(prompt, r'ancienneté minimale de\s+(\d+)\s+mois|anciennete minimale de\s+(\d+)\s+mois')
            
            # Extract geographical coverage
            couverture_geo = self._extract_couverture_geographique(prompt)
            
            # Extract price
            prix = self._extract_number(prompt, r'prix mensuel de\s+(\d+(?:\.\d+)?)|prix de\s+(\d+(?:\.\d+)?)')
            
            # Extract contract duration
            duree_min = self._extract_number(prompt, r'durée de contrat comprise entre\s+(\d+)\s+et|duree de contrat de\s+(\d+)\s+à')
            duree_max = self._extract_number(prompt, r'et\s+(\d+)\s+mois|à\s+(\d+)\s+mois')
            
            # Extract coverage level
            niveau = self._extract_niveau_couverture(prompt)
            
            # Extract status
            statut = self._extract_statut(prompt)
            
            pack_dto = PackDTO(
                nom_pack=nom_pack,
                nom_produit=nom_produit,
                description=description,
                age_minimum=age_min,
                age_maximum=age_max,
                type_clients=[type_client] if type_client else None,
                anciennete_contrat_mois=anciennete,
                couverture_geographique=couverture_geo,
                prix_mensuel=prix,
                duree_min_contrat=duree_min,
                duree_max_contrat=duree_max,
                niveau_couverture=niveau,
                statut=statut
            )
            
            logger.info(f"✅ Parsed pack: {nom_pack}, product: {nom_produit}, price: {prix}")
            return pack_dto
            
        except Exception as e:
            logger.error(f"❌ Error parsing pack prompt: {e}")
            return None
    
    def parse_garantie_prompt(self, prompt: str) -> Optional[GarantieDTO]:
        """Parse a guarantee creation prompt using rule-based patterns."""
        try:
            # Extract guarantee name
            name_pattern = r'créer une garantie nommée\s+["\']?([^"\',.]+)["\']?|créer une garantie\s+["\']?([^"\',.]+)["\']?'
            name_match = re.search(name_pattern, prompt, re.IGNORECASE)
            if not name_match:
                return None
            
            nom_garantie = name_match.group(1) or name_match.group(2)
            nom_garantie = nom_garantie.strip()
            
            # Extract domain
            domaine = self._extract_domaine_medical(prompt)
            
            # Extract amount type
            type_montant = self._extract_type_montant(prompt)
            
            # Extract reimbursement rate (convert percentage to decimal)
            taux_pattern = r'taux de remboursement de\s+(\d+(?:\.\d+)?)\s*%?|remboursement de\s+(\d+(?:\.\d+)?)\s*%?'
            taux_match = re.search(taux_pattern, prompt, re.IGNORECASE)
            taux = (float(taux_match.group(1) or taux_match.group(2)) / 100) if taux_match else None
            
            # Extract ceilings
            plafond_annuel = self._extract_number(prompt, r'plafond annuel de\s+(\d+(?:\.\d+)?)')
            plafond_mensuel = self._extract_number(prompt, r'plafond mensuel de\s+(\d+(?:\.\d+)?)')
            plafond_par_acte = self._extract_number(prompt, r'plafond par acte de\s+(\d+(?:\.\d+)?)')
            
            # Extract franchise
            franchise = self._extract_number(prompt, r'franchise de\s+(\d+(?:\.\d+)?)')
            
            # Extract cost per claim
            cout_moyen = self._extract_number(prompt, r'coût moyen par sinistre de\s+(\d+(?:\.\d+)?)|cout moyen par sinistre de\s+(\d+(?:\.\d+)?)')
            
            # Extract contract duration
            duree_min = self._extract_number(prompt, r'durée de contrat de\s+(\d+)\s+à|durée de contrat de\s+(\d+)\s+à')
            duree_max = self._extract_number(prompt, r'à\s+(\d+)\s+mois|\d+\s+à\s+(\d+)\s+mois')
            
            # Extract resiliation
            resiliable_pattern = r'résiliable\s+(true|false|annuellement)'
            resiliable_match = re.search(resiliable_pattern, prompt, re.IGNORECASE)
            resiliable = resiliable_match.group(1).lower() == 'true' or 'annuellement' in prompt.lower() if resiliable_match else None
            
            # Extract status
            statut = self._extract_statut(prompt)
            
            garantie_dto = GarantieDTO(
                nom_garantie=nom_garantie,
                domaine=domaine,
                type_montant=type_montant,
                taux_remboursement=taux,
                plafond_annuel=plafond_annuel,
                plafond_mensuel=plafond_mensuel,
                plafond_par_acte=plafond_par_acte,
                franchise=franchise,
                cout_moyen_par_sinistre=cout_moyen,
                duree_min_contrat=duree_min,
                duree_max_contrat=duree_max,
                resiliable_annuellement=resiliable,
                statut=statut
            )
            
            logger.info(f"✅ Parsed guarantee: {nom_garantie}, domain: {domaine}, rate: {taux}")
            return garantie_dto
            
        except Exception as e:
            logger.error(f"❌ Error parsing guarantee prompt: {e}")
            return None
    
    def parse_recommendation_prompt(self, prompt: str) -> Optional[Dict[str, Any]]:
        """Parse a recommendation prompt to extract client profile."""
        try:
            profile = {}
            
            # Extract age
            age_pattern = r'(\d+)\s*ans?'
            age_match = re.search(age_pattern, prompt)
            profile['age'] = int(age_match.group(1)) if age_match else None
            
            # Extract gender
            if 'femme' in prompt.lower() or 'féminin' in prompt.lower() or 'fille' in prompt.lower():
                profile['gender'] = 'F'
            elif 'homme' in prompt.lower() or 'masculin' in prompt.lower() or 'garçon' in prompt.lower():
                profile['gender'] = 'M'
            
            # Extract marital status
            if 'marié' in prompt.lower() or 'mariée' in prompt.lower():
                profile['marital_status'] = 'MARRIED'
            elif 'célibataire' in prompt.lower():
                profile['marital_status'] = 'SINGLE'
            elif 'divorcé' in prompt.lower() or 'divorcée' in prompt.lower():
                profile['marital_status'] = 'DIVORCED'
            elif 'veuf' in prompt.lower() or 'veuve' in prompt.lower():
                profile['marital_status'] = 'WIDOWED'
            
            # Extract number of children
            children_pattern = r'(\d+)\s*enfants?'
            children_match = re.search(children_pattern, prompt)
            profile['number_of_children'] = int(children_match.group(1)) if children_match else 0
            
            # Extract budget
            budget_pattern = r'budget\s+(?:de\s+)?(\d+(?:\.\d+)?)\s*(?:euros?|tnd?|dt)?'
            budget_match = re.search(budget_pattern, prompt, re.IGNORECASE)
            profile['monthly_budget'] = float(budget_match.group(1)) if budget_match else None
            
            # Extract coverage needs
            if 'hospitalisation' in prompt.lower():
                profile['coverage_needs'] = profile.get('coverage_needs', []) + ['HOSPITALISATION']
            if 'dentaire' in prompt.lower():
                profile['coverage_needs'] = profile.get('coverage_needs', []) + ['DENTAIRE']
            if 'optique' in prompt.lower():
                profile['coverage_needs'] = profile.get('coverage_needs', []) + ['OPTIQUE']
            if 'famille' in prompt.lower():
                profile['client_type'] = 'FAMILLE'
            if 'senior' in prompt.lower():
                profile['client_type'] = 'SENIOR'
            
            logger.info(f"✅ Parsed recommendation profile: {profile}")
            return profile
            
        except Exception as e:
            logger.error(f"❌ Error parsing recommendation prompt: {e}")
            return None
    
    def _extract_number(self, text: str, pattern: str) -> Optional[float]:
        """Extract a number from text using a regex pattern."""
        match = re.search(pattern, text, re.IGNORECASE)
        if match:
            for group in match.groups():
                if group is not None:
                    try:
                        return float(group)
                    except ValueError:
                        continue
        return None
    
    def _extract_type_produit(self, text: str) -> Optional[TypeProduit]:
        """Extract product type from text."""
        text_lower = text.lower()
        if 'santé' in text_lower or 'sante' in text_lower:
            return TypeProduit.SANTE
        elif 'habitation' in text_lower or 'logement' in text_lower:
            return TypeProduit.HABITATION
        elif 'auto' in text_lower or 'voiture' in text_lower:
            return TypeProduit.AUTO
        elif 'épargne' in text_lower or 'epargne' in text_lower:
            return TypeProduit.EPARGNE
        elif 'vie' in text_lower:
            return TypeProduit.VIE
        return None
    
    def _extract_statut(self, text: str) -> Optional[Statut]:
        """Extract status from text."""
        text_lower = text.lower()
        if 'actif' in text_lower:
            return Statut.ACTIF
        elif 'inactif' in text_lower:
            return Statut.INACTIF
        elif 'en attente' in text_lower:
            return Statut.EN_ATTENTE
        return Statut.ACTIF  # Default to active
    
    def _extract_type_client(self, text: str) -> Optional[TypeClient]:
        """Extract client type from text."""
        text_lower = text.lower()
        if 'individuel' in text_lower:
            return TypeClient.INDIVIDUEL
        elif 'famille' in text_lower:
            return TypeClient.FAMILLE
        elif 'enfant' in text_lower:
            return TypeClient.ENFANT
        elif 'senior' in text_lower:
            return TypeClient.SENIOR
        elif 'entreprise' in text_lower:
            return TypeClient.ENTREPRISE
        elif 'étudiant' in text_lower or 'etudiant' in text_lower:
            return TypeClient.ETUDIANT
        return TypeClient.INDIVIDUEL  # Default
    
    def _extract_couverture_geographique(self, text: str) -> Optional[CouvertureGeographique]:
        """Extract geographical coverage from text."""
        text_lower = text.lower()
        if 'national' in text_lower or 'nationale' in text_lower:
            return CouvertureGeographique.NATIONAL
        elif 'international' in text_lower or 'internationale' in text_lower:
            return CouvertureGeographique.INTERNATIONAL
        elif 'local' in text_lower or 'locale' in text_lower:
            return CouvertureGeographique.LOCAL
        elif 'ue' in text_lower or 'europe' in text_lower or 'européen' in text_lower:
            return CouvertureGeographique.UE
        elif 'maghreb' in text_lower:
            return CouvertureGeographique.MAGHREB
        return CouvertureGeographique.NATIONAL  # Default
    
    def _extract_niveau_couverture(self, text: str) -> Optional[NiveauCouverture]:
        """Extract coverage level from text."""
        text_lower = text.lower()
        if 'basic' in text_lower or 'essentiel' in text_lower or 'basique' in text_lower:
            return NiveauCouverture.BASIC
        elif 'premium' in text_lower:
            return NiveauCouverture.PREMIUM
        elif 'gold' in text_lower:
            return NiveauCouverture.GOLD
        return NiveauCouverture.BASIC  # Default
    
    def _extract_type_montant(self, text: str) -> Optional[TypeMontant]:
        """Extract amount type from text."""
        text_lower = text.lower()
        if 'forfait' in text_lower:
            return TypeMontant.FORFAIT
        elif 'frais réels' in text_lower or 'frais reels' in text_lower:
            return TypeMontant.FRAIS_REELS
        elif 'tarif conventionné' in text_lower or 'tarif conventionne' in text_lower:
            return TypeMontant.TARIF_CONVENTIONNE
        return TypeMontant.FRAIS_REELS  # Default
    
    def _extract_domaine_medical(self, text: str) -> Optional[DomaineMedical]:
        """Extract medical domain from text."""
        text_lower = text.lower()
        
        # Map common French terms to enum values
        domain_mapping = {
            'hospitalisation': DomaineMedical.HOSPITALISATION_MEDICALE,
            'hospitalisation médicale': DomaineMedical.HOSPITALISATION_MEDICALE,
            'dentaire': DomaineMedical.DENTISTERIE_GENERALE,
            'dentisterie': DomaineMedical.DENTISTERIE_GENERALE,
            'optique': DomaineMedical.OPHTALMOLOGIE,
            'ophtalmologie': DomaineMedical.OPHTALMOLOGIE,
            'cardiologie': DomaineMedical.CARDIOLOGIE,
            'neurologie': DomaineMedical.NEUROLOGIE,
            'pédiatrie': DomaineMedical.PEDIATRIE,
            'gynécologie': DomaineMedical.GYNECOLOGIE,
            'psychiatrie': DomaineMedical.PSYCHIATRIE,
            'psychologie': DomaineMedical.PSYCHOLOGIE,
            'kinésithérapie': DomaineMedical.KINESITHERAPIE,
            'kinesitherapie': DomaineMedical.KINESITHERAPIE,
            'radiologie': DomaineMedical.RADIOLOGIE,
            'imagerie': DomaineMedical.IMAGERIE_MEDICALE,
            'analyses': DomaineMedical.ANALYSES_BIOLOGIQUES,
            'analyses biologiques': DomaineMedical.ANALYSES_BIOLOGIQUES,
        }
        
        for term, domaine in domain_mapping.items():
            if term in text_lower:
                return domaine
        
        return None
    
    def parse_pack_garantie_association(self, prompt: str) -> Optional[Dict[str, Any]]:
        """Parse pack-garantie association from prompt."""
        try:
            result = {}
            
            # Extract guarantee name
            garantie_pattern = r'garantie\s+["\']?([^"\',.]+)["\']?'
            garantie_match = re.search(garantie_pattern, prompt, re.IGNORECASE)
            result['nom_garantie'] = garantie_match.group(1).strip() if garantie_match else None
            
            # Extract rate (convert percentage to decimal)
            taux_pattern = r'taux de remboursement de\s+(\d+(?:\.\d+)?)\s*%?'
            taux_match = re.search(taux_pattern, prompt, re.IGNORECASE)
            result['taux_remboursement'] = (float(taux_match.group(1)) / 100) if taux_match else None
            
            # Extract ceiling
            plafond_pattern = r'plafond de\s+(\d+(?:\.\d+)?)'
            plafond_match = re.search(plafond_pattern, prompt, re.IGNORECASE)
            result['plafond'] = float(plafond_match.group(1)) if plafond_match else None
            
            # Extract franchise
            franchise_pattern = r'franchise de\s+(\d+(?:\.\d+)?)'
            franchise_match = re.search(franchise_pattern, prompt, re.IGNORECASE)
            result['franchise'] = float(franchise_match.group(1)) if franchise_match else 0.0
            
            # Extract waiting period
            carence_pattern = r'délai de carence de\s+(\d+)\s+jours?'
            carence_match = re.search(carence_pattern, prompt, re.IGNORECASE)
            result['delai_carence'] = int(carence_match.group(1)) if carence_match else 0
            
            # Extract amount type
            result['type_montant'] = self._extract_type_montant(prompt)
            
            # Extract optional/mandatory
            if 'optionnelle' in prompt.lower():
                result['optionnelle'] = True
            elif 'obligatoire' in prompt.lower():
                result['optionnelle'] = False
            
            # Extract priority
            priorite_pattern = r'priorité\s+(\d+)'
            priorite_match = re.search(priorite_pattern, prompt, re.IGNORECASE)
            result['priorite'] = int(priorite_match.group(1)) if priorite_match else 1
            
            # Extract price supplement
            supplement_pattern = r'supplément de prix de\s+(\d+(?:\.\d+)?)'
            supplement_match = re.search(supplement_pattern, prompt, re.IGNORECASE)
            result['supplement_prix'] = float(supplement_match.group(1)) if supplement_match else 0.0
            
            logger.info(f"✅ Parsed pack-garantie association: {result}")
            return result
            
        except Exception as e:
            logger.error(f"❌ Error parsing pack-garantie association: {e}")
            return None
