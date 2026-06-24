import re
import logging
from typing import Optional
from app.models.enums import ChatbotAction

logger = logging.getLogger(__name__)


class PromptAnalyzerService:
    """Service for analyzing prompts and determining actions."""
    
    # Keywords for each action
    ACTION_KEYWORDS = {
        ChatbotAction.GARANTIE: [
            "garantie", "couverture", "remboursement", "soins",
            "hospitalisation", "consultation", "médical", "dentaire", "optique"
        ],
        ChatbotAction.PRODUIT: [
            "produit", "police", "contrat d'assurance", "offre", "solution", "assurance"
        ],
        ChatbotAction.PACK: [
            "pack", "formule", "offre pack", "bundle", "ensemble"
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
        
        # Check for recommendation first (highest priority)
        if self._is_recommendation_prompt(prompt_lower):
            return ChatbotAction.RECOMMANDATION
        
        # Check for creation actions
        is_creation = any(kw in prompt_lower for kw in self.CREATION_KEYWORDS)
        
        # Priority 1: Explicit entity mentions with creation
        if is_creation:
            if "produit" in prompt_lower:
                return ChatbotAction.PRODUIT
            elif "pack" in prompt_lower:
                return ChatbotAction.PACK
            elif "garantie" in prompt_lower:
                return ChatbotAction.GARANTIE
        
        # Priority 2: Score each action based on keyword matches
        action_scores = {}
        for action, keywords in self.ACTION_KEYWORDS.items():
            score = sum(1 for kw in keywords if kw in prompt_lower)
            if score > 0:
                if is_creation and action in [ChatbotAction.GARANTIE, ChatbotAction.PRODUIT, ChatbotAction.PACK]:
                    score += 2  # Boost creation actions
                action_scores[action] = score
        
        # Return action with highest score
        if action_scores:
            return max(action_scores, key=action_scores.get)
        
        # Default fallback based on context
        if "pack" in prompt_lower:
            return ChatbotAction.PACK
        elif "produit" in prompt_lower:
            return ChatbotAction.PRODUIT
        elif "garantie" in prompt_lower:
            return ChatbotAction.GARANTIE
        
        return None
    
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
