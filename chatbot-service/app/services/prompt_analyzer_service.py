import re
import logging
from typing import Optional
from app.models.enums import ChatbotAction
logger = logging.getLogger(__name__)
class PromptAnalyzerService:
    """Service for analyzing prompts and determining actions."""
    # Keywords for each action
    ACTION_KEYWORDS = {
        ChatbotAction.CREATE_GARANTIE: [
            "créer une garantie", "créer garantie", "nouvelle garantie", "ajouter garantie"
        ],
        ChatbotAction.CREATE_PRODUIT: [
            "créer un produit", "créer produit", "nouveau produit", "ajouter produit"
        ],
        ChatbotAction.CREATE_PACK: [
            "créer un pack", "créer pack", "nouveau pack", "ajouter pack"
        ],
        ChatbotAction.UPDATE_GARANTIE: [
            "modifier une garantie", "mettre à jour une garantie", "changer garantie", "update garantie"
        ],
        ChatbotAction.UPDATE_PRODUIT: [
            "modifier un produit", "mettre à jour un produit", "changer produit", "update produit"
        ],
        ChatbotAction.UPDATE_PACK: [
            "modifier un pack", "mettre à jour un pack", "changer pack", "update pack"
        ],
        ChatbotAction.DELETE_GARANTIE: [
            "supprimer une garantie", "effacer garantie", "désactiver garantie", "delete garantie"
        ],
        ChatbotAction.DELETE_PRODUIT: [
            "supprimer un produit", "effacer produit", "désactiver produit", "delete produit"
        ],
        ChatbotAction.DELETE_PACK: [
            "supprimer un pack", "effacer pack", "désactiver pack", "delete pack"
        ],
        ChatbotAction.CONFIGURATION_PACK: [
            "configurer", "modifier", "ajuster", "paramétrer", "configuration"
        ],
        ChatbotAction.AJOUT_GARANTIE_PACK: [
            "ajouter", "inclure", "ajout", "ajoute", "inclure"
        ],
        ChatbotAction.RECOMMANDATION: [
            "recommander", "suggérer", "conseiller", "me conseiller", "que me recommandez",
            "quel pack", "quelle offre", "adapté à", "pour moi", "mon profil",
            "je cherche", "besoin", "budget", "ans", "marié", "enfants"
        ]
    }
    
    # Creation keywords
    CREATION_KEYWORDS = [
        "créer", "create", "nouveau", "nouvelle", "ajouter", "ajoute",
        "enregistrer", "sauvegarder", "établir"
    ]
    
    def analyze_action(self, prompt: str) -> Optional[ChatbotAction]:
        """Analyze prompt and determine the action."""
        if not prompt:
            return None
        prompt_lower = prompt.lower()
        
        # Priority 1: Check for DELETE actions (highest priority for safety)
        if re.search(r'supprimer|effacer|désactiver|delete', prompt_lower):
            if 'garantie' in prompt_lower:
                return ChatbotAction.DELETE_GARANTIE
            elif 'produit' in prompt_lower:
                return ChatbotAction.DELETE_PRODUIT
            elif 'pack' in prompt_lower:
                return ChatbotAction.DELETE_PACK
        
        # Priority 2: Check for UPDATE actions
        if re.search(r'modifier|mettre à jour|changer|update', prompt_lower):
            if 'garantie' in prompt_lower:
                return ChatbotAction.UPDATE_GARANTIE
            elif 'produit' in prompt_lower:
                return ChatbotAction.UPDATE_PRODUIT
            elif 'pack' in prompt_lower:
                return ChatbotAction.UPDATE_PACK
        
        # Priority 3: Check for creation actions (highest priority)
        # Prioritize "créer un pack" before "produit" to avoid false positives
        if re.search(r'créer\s+un\s+pack|pack\s+nommé|nouveau\s+pack', prompt_lower):
            return ChatbotAction.CREATE_PACK
        if re.search(r'créer\s+un\s+produit|produit\s+nommé', prompt_lower):
            return ChatbotAction.CREATE_PRODUIT
        if re.search(r'créer\s+une\s+garantie|garantie\s+nommée', prompt_lower):
            return ChatbotAction.CREATE_GARANTIE

        # Priority 4: Check for pack configuration
        if any(kw in prompt_lower for kw in self.ACTION_KEYWORDS[ChatbotAction.CONFIGURATION_PACK]):
            return ChatbotAction.CONFIGURATION_PACK

        # Priority 5: Check for adding guarantee to pack
        if any(kw in prompt_lower for kw in self.ACTION_KEYWORDS[ChatbotAction.AJOUT_GARANTIE_PACK]):
            return ChatbotAction.AJOUT_GARANTIE_PACK

        # Priority 6: Check for recommendation (fallback)
        if self._is_recommendation_prompt(prompt_lower):
            return ChatbotAction.RECOMMANDATION

        # Default fallback based on context
        if "pack" in prompt_lower:
            return ChatbotAction.CREATE_PACK
        elif "produit" in prompt_lower:
            return ChatbotAction.CREATE_PRODUIT
        elif "garantie" in prompt_lower:
            return ChatbotAction.CREATE_GARANTIE

        return ChatbotAction.UNKNOWN
    
    def _is_recommendation_prompt(self, prompt: str) -> bool:
        """Check if prompt is a recommendation request."""
        recommendation_indicators = [
            # Direct recommendation requests
            "recommander", "recommande", "suggérer", "suggère", "conseiller", "conseille",
            "que me recommandez", "que me conseille", "me conseiller", "me recommander",
            # Profile questions
            "quel pack", "quelle offre", "quelle formule", "quel produit",
            "adapté à", "adapté pour", "conviennent", "convient",
            # Personal information indicators
            "je suis", "mon âge", "j'ai", "ans", "marié", "célibataire", "enfants",
            "budget", "revenu", "salaire", "profession", "travail",
            # Need-based questions
            "je cherche", "besoin", "j'ai besoin", "pour moi", "ma famille",
            "quel est le meilleur", "le plus adapté", "idéal pour"
        ]
        
        return any(indicator in prompt for indicator in recommendation_indicators)
    def extract_entities_from_prompt(self, prompt: str) -> dict:
        """Extract entity names from prompt."""
        entities = {
            "garanties": [],
            "packs": [],
            "produits": []
        }
        
        # Extract guarantees (simple pattern matching)
        garantie_pattern = r'(?:garantie|couverture)\s+([A-Z][a-zA-ZàâäéèêëïîôöùûüÿçÀÂÄÉÈÊËÏÎÔÖÙÛÜŸÇ\s]+?)(?:\s+avec|\s+et|,|\.$|$)'
        for match in re.finditer(garantie_pattern, prompt, re.IGNORECASE):
            entities["garanties"].append(match.group(1).strip())

        # Extract packs
        pack_pattern = r'(?:pack|formule)\s+([A-Z][a-zA-ZàâäéèêëïîôöùûüÿçÀÂÄÉÈÊËÏÎÔÖÙÛÜŸÇ\s]+?)(?:\s+avec|\s+et|,|\.$|$)'
        for match in re.finditer(pack_pattern, prompt, re.IGNORECASE):
            entities["packs"].append(match.group(1).strip())
        
        # Extract products
        produit_pattern = r'(?:produit|offre|police)\s+([A-Z][a-zA-ZàâäéèêëïîôöùûüÿçÀÂÄÉÈÊËÏÎÔÖÙÛÜŸÇ\s]+?)(?:\s+avec|\s+et|,|\.$|$)'
        for match in re.finditer(produit_pattern, prompt, re.IGNORECASE):
            entities["produits"].append(match.group(1).strip())
        
        return entities
