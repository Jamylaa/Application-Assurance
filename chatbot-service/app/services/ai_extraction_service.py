import re
import json
import logging
from typing import Dict, Any, Optional
import google.generativeai as genai
from app.config import settings

logger = logging.getLogger(__name__)


class AIExtractionService:
    """Service for AI-based data extraction using Google Gemini API."""
    
    def __init__(self):
        self.api_key = settings.gemini_api_key
        self.model_name = settings.gemini_model
        self.gemini_url = settings.gemini_url
        self.timeout_seconds = settings.gemini_timeout_seconds
        self.max_retries = settings.gemini_max_retries
        self.retry_delay_ms = settings.gemini_retry_delay_ms
        self.gemini_enabled = settings.gemini_enabled
        self.ai_extraction_enabled = settings.ai_extraction_enabled
        self.fallback_on_error = settings.fallback_on_ai_error
        
        if self.api_key and self.gemini_enabled:
            genai.configure(api_key=self.api_key)
            self.model = genai.GenerativeModel(self.model_name)
    
    def is_ai_available(self) -> bool:
        """Check if AI service is available."""
        return (
            self.api_key is not None 
            and self.api_key.strip() != "" 
            and self.gemini_enabled 
            and self.ai_extraction_enabled
        )
    
    def extract_garantie_data(self, prompt: str) -> Dict[str, Any]:
        """Extract guarantee data from natural language prompt."""
        try:
            logger.info("=== EXTRACTION IA GARANTIE ===")
            logger.info(f"Prompt: {prompt}")
            
            enhanced_prompt = prompt + """
            
Extrais les informations pour créer une garantie d'assurance. IMPORTANT:
- Retourne un JSON avec: nom, description, domaine (domaine médical parmi: CONSULTATION_GENERALE, CARDIOLOGIE, DENTAIRE, OPHTALMOLOGIE, HOSPITALISATION, etc.)
- tauxRemboursement (en décimal, ex: 0.8 pour 80%)
- typeMontant (TARIF_CONVENTIONNE/FRAIS_REELS/FORFAIT - EXACTEMENT ces valeurs)
- plafondAnnuel, plafondMensuel, plafondParActe
- franchise, coutMoyenParSinistre
- dureeMinContrat, dureeMaxContrat (DEUX valeurs distinctes)
- resiliableAnnuellement (true/false)
- statut (ACTIF/INACTIF)
- Pour les plages de durée (ex: '12 à 36 mois'), séparez en dureeMinContrat=12 et dureeMaxContrat=36
- Utilise null si vraiment absent.
"""
            
            result = self._call_google_ai_with_retry(enhanced_prompt)
            
            logger.info(f"Résultat IA brut: {result}")
            
            # Validate critical fields
            if "typeMontant" in result:
                type_montant = result.get("typeMontant")
                logger.info(f"TypeMontant extrait par IA: {type_montant}")
                if type_montant not in ["TARIF_CONVENTIONNE", "FRAIS_REELS", "FORFAIT"]:
                    logger.warning(f"TypeMontant invalide extrait: {type_montant}")
            else:
                logger.warning("TypeMontant non extrait par l'IA")
            
            return result
        except Exception as e:
            logger.error(f"Erreur extraction garantie IA: {e}", exc_info=True)
            return {}
    
    def extract_produit_data(self, prompt: str) -> Dict[str, Any]:
        """Extract product data from natural language prompt."""
        try:
            enhanced_prompt = prompt + """
            
Extrais les informations pour créer un produit d'assurance. Retourne un JSON avec:
- nom, description
- typeProduit (SANTE/AUTO/HABITATION/VIE/PREVOYANCE/EPARGNE)
- statut (ACTIF/INACTIF)
Utilise null si absent.
"""
            return self._call_google_ai_with_retry(enhanced_prompt)
        except Exception as e:
            logger.error(f"Erreur extraction produit IA: {e}", exc_info=True)
            return {}
    
    def extract_pack_data(self, prompt: str) -> Dict[str, Any]:
        """Extract pack data from natural language prompt."""
        try:
            enhanced_prompt = prompt + """
            
Extrais les informations pour créer un pack d'assurance. IMPORTANT:
- Retourne un JSON avec: nom, description, ageMin, ageMax, typeClients (INDIVIDUEL/FAMILLE/SENIOR), couvertureGeographique, prixMensuel (nombre, pas de texte), dureeMinContrat, dureeMaxContrat, niveauCouverture (BASIC/PREMIUM/GOLD), statut (ACTIF/INACTIF), nomProduit (nom du produit associé, pas l'ID)
- Pour prixMensuel: cherche spécifiquement des chiffres suivis de € ou 'euros' ou 'TND'. Si aucun prix n'est mentionné, utilise 0.
- Pour typeClients: déduis du nom du pack (ex: 'Famille' → FAMILLE, 'Senior' → SENIOR, sinon INDIVIDUEL)
- Pour niveauCouverture: déduis du nom (ex: 'Essentiel' → BASIC, 'Premium' → PREMIUM, 'Gold' → GOLD)
- Pour nomProduit: cherche le nom du produit mentionné
- Utilise null si vraiment absent.
"""
            result = self._call_google_ai_with_retry(enhanced_prompt)
            return self._apply_pack_fallback_logic(result, prompt)
        except Exception as e:
            logger.error(f"Erreur extraction pack IA: {e}", exc_info=True)
            return {}
    
    def extract_pack_configuration_data(self, prompt: str) -> Dict[str, Any]:
        """Extract pack configuration data from natural language prompt."""
        try:
            enhanced_prompt = prompt + """
            
Extrais les informations pour configurer un pack:
- packId, garantieId, tauxRemboursement, plafond, franchise, optionnelle, supplementPrix
Utilise null si absent.
"""
            return self._call_google_ai_with_retry(enhanced_prompt)
        except Exception as e:
            logger.error(f"Erreur extraction configuration pack IA: {e}", exc_info=True)
            return {}
    
    def extract_add_garantie_to_pack_data(self, prompt: str) -> Dict[str, Any]:
        """Extract data for adding a guarantee to a pack."""
        try:
            enhanced_prompt = prompt + """
            
Extrais:
- nomPack, nomGarantie, tauxRemboursement, plafond, franchise, optionnelle
Utilise null si absent.
"""
            result = self._call_google_ai_with_retry(enhanced_prompt)
            
            # Fallback to regex if AI didn't extract pack name
            if not result or not result.get("nomPack"):
                logger.warn("IA n'a pas extrait nomPack, utilisation du fallback regex")
                return self._extract_add_garantie_to_pack_data_fallback(prompt)
            
            return result
        except Exception as e:
            logger.error(f"Erreur extraction ajout garantie pack IA: {e}", exc_info=True)
            return self._extract_add_garantie_to_pack_data_fallback(prompt)
    
    def _call_google_ai_with_retry(self, prompt: str) -> Dict[str, Any]:
        """Call Google AI with retry logic."""
        if not self.is_ai_available():
            logger.warn("API KEY non configurée, fallback patterns")
            return {}
        
        attempt = 0
        while attempt < self.max_retries:
            try:
                logger.debug(f"Tentative {attempt + 1}/{self.max_retries} Gemini")
                result = self._call_google_ai(prompt)
                if result:
                    logger.info("Extraction IA réussie")
                    return result
            except Exception as e:
                logger.warn(f"Tentative {attempt + 1} échouée: {e}")
                attempt += 1
                if attempt < self.max_retries:
                    import time
                    time.sleep(self.retry_delay_ms / 1000)
        
        if self.fallback_on_error:
            logger.warn("Fallback patterns activé")
            return {}
        
        return {}
    
    def _call_google_ai(self, prompt: str) -> Dict[str, Any]:
        """Call Google Gemini API."""
        try:
            response = self.model.generate_content(
                prompt,
                generation_config=genai.types.GenerationConfig(
                    temperature=0.3,
                    max_output_tokens=8192,
                )
            )
            
            if response.text:
                return self._parse_ai_response(response.text)
            
            logger.warn("Aucune réponse de l'IA")
            return {}
        except Exception as e:
            logger.error(f"Erreur appel Gemini: {e}", exc_info=True)
            raise
    
    def _parse_ai_response(self, ai_response: str) -> Dict[str, Any]:
        """Parse AI response to extract JSON."""
        try:
            # Try to find JSON in the response
            json_pattern = re.compile(r'\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}', re.DOTALL)
            matcher = json_pattern.search(ai_response)
            
            if matcher:
                json_str = self._clean_json_string(matcher.group())
                result = json.loads(json_str)
                logger.debug("JSON parsé avec succès")
                return result
            
            logger.warn("Aucun JSON trouvé dans la réponse")
            return {}
        except Exception as e:
            logger.warn(f"Erreur parsing JSON: {e}")
            return {}
    
    def _clean_json_string(self, json_str: str) -> str:
        """Clean JSON string for parsing."""
        # Replace single quotes with double quotes
        json_str = re.sub(r"(?<!\\)'([^']*)'", r'"\1"', json_str)
        # Remove control characters
        json_str = re.sub(r'[\x00-\x1F]', '', json_str)
        return json_str
    
    def _apply_pack_fallback_logic(self, data: Dict[str, Any], original_prompt: str) -> Dict[str, Any]:
        """Apply fallback logic for pack data extraction."""
        if not data:
            return data
        
        pack_name = self._get_string_value(data, "nom", "").lower()
        prompt_lower = original_prompt.lower()
        
        # Deduce coverage level
        if not data.get("niveauCouverture"):
            niveau = self._deduce_coverage_level(pack_name, prompt_lower)
            if niveau:
                data["niveauCouverture"] = niveau
                logger.info(f"Niveau de couverture déduit: {niveau}")
        
        # Deduce client type
        if not data.get("typeClients"):
            type_client = self._deduce_client_type(pack_name, prompt_lower)
            if type_client:
                data["typeClients"] = type_client
                logger.info(f"Type de client déduit: {type_client}")
        
        # Extract price with regex
        if not data.get("prixMensuel") or data.get("prixMensuel", 0) <= 0:
            prix = self._extract_price_from_prompt(prompt_lower)
            if prix and prix > 0:
                data["prixMensuel"] = prix
                logger.info(f"Prix extrait: {prix}")
        
        return data
    
    def _deduce_coverage_level(self, pack_name: str, prompt: str) -> Optional[str]:
        """Deduce coverage level from pack name or prompt."""
        keywords_basic = ["essentiel", "basic", "confort", "standard"]
        keywords_premium = ["premium", "gold"]
        
        if any(kw in pack_name or kw in prompt for kw in keywords_basic):
            return "BASIC"
        elif any(kw in pack_name or kw in prompt for kw in keywords_premium):
            return "PREMIUM"
        
        return None
    
    def _deduce_client_type(self, pack_name: str, prompt: str) -> Optional[str]:
        """Deduce client type from pack name or prompt."""
        if "famille" in pack_name or "enfants" in pack_name or "famille" in prompt or "enfants" in prompt:
            return "FAMILLE"
        elif "senior" in pack_name or "60" in prompt:
            return "SENIOR"
        
        return "INDIVIDUEL"
    
    def _extract_price_from_prompt(self, prompt: str) -> Optional[float]:
        """Extract price from prompt using regex."""
        price_pattern = re.compile(r'(?:prix\s*)?(\d+(?:[.,]\d+)?)\s*(?:€|euros|eur|TND|tunisien)?', re.IGNORECASE)
        matcher = price_pattern.search(prompt)
        
        if matcher:
            try:
                price_str = matcher.group(1).replace(",", ".")
                return float(price_str)
            except ValueError:
                logger.warn(f"Erreur parsing prix: {matcher.group(1)}")
        
        return None
    
    def _extract_add_garantie_to_pack_data_fallback(self, prompt: str) -> Dict[str, Any]:
        """Fallback regex extraction for adding guarantee to pack."""
        result = {}
        prompt_lower = prompt.lower()
        
        # Extract pack name
        pack_pattern = re.compile(r'(?:au\s+pack|pack\s+)([a-zA-ZàâäéèêëïîôöùûüÿçÀÂÄÉÈÊËÏÎÔÖÙÛÜŸÇ\s]+?)(?:\s+avec|\s+et|,|\.$|$)', re.IGNORECASE)
        pack_matcher = pack_pattern.search(prompt)
        if pack_matcher:
            result["nomPack"] = pack_matcher.group(1).strip()
            logger.info(f"nomPack extrait via fallback: {result['nomPack']}")
        
        # Extract guarantee name
        garantie_pattern = re.compile(r'(?:la\s+garantie|garantie\s+)([a-zA-ZàâäéèêëïîôöùûüÿçÀÂÄÉÈÊËÏÎÔÖÙÛÜŸÇ\s]+?)(?:\s+avec|\s+et|\s+un|,|\.$|$)', re.IGNORECASE)
        garantie_matcher = garantie_pattern.search(prompt)
        if garantie_matcher:
            result["nomGarantie"] = garantie_matcher.group(1).strip()
            logger.info(f"nomGarantie extrait via fallback: {result['nomGarantie']}")
        
        # Extract reimbursement rate
        taux_pattern = re.compile(r'(?:taux\s+de\s+remboursement\s*(?:de|:)?\s*|remboursement\s*(?:de|:)?\s*)(\d+)\s*(?:%|pourcent|pour\s+cent)', re.IGNORECASE)
        taux_matcher = taux_pattern.search(prompt)
        if taux_matcher:
            try:
                taux = float(taux_matcher.group(1)) / 100.0
                result["tauxRemboursement"] = taux
                logger.info(f"tauxRemboursement extrait via fallback: {taux}")
            except ValueError:
                pass
        
        # Extract ceiling
        plafond_pattern = re.compile(r'(?:plafond\s*(?:de|:)?\s*|plafonds?\s*(?:de|:)?\s*)(\d+)', re.IGNORECASE)
        plafond_matcher = plafond_pattern.search(prompt)
        if plafond_matcher:
            try:
                plafond = float(plafond_matcher.group(1))
                result["plafond"] = plafond
                logger.info(f"plafond extrait via fallback: {plafond}")
            except ValueError:
                pass
        
        # Extract deductible
        franchise_pattern = re.compile(r'(?:franchise\s*(?:de|:)?\s*)(\d+)', re.IGNORECASE)
        franchise_matcher = franchise_pattern.search(prompt)
        if franchise_matcher:
            try:
                franchise = float(franchise_matcher.group(1))
                result["franchise"] = franchise
                logger.info(f"franchise extraite via fallback: {franchise}")
            except ValueError:
                pass
        
        # Check if optional
        if "optionnelle" in prompt_lower or "optionnel" in prompt_lower:
            result["optionnelle"] = True
        
        return result
    
    def _get_string_value(self, data: Dict[str, Any], key: str, default: str = "") -> str:
        """Get string value from dict with default."""
        if not data:
            return default
        value = data.get(key)
        if value is None:
            return default
        return str(value).strip() or default
    
    def _get_double_value(self, data: Dict[str, Any], key: str, default: float = 0.0) -> float:
        """Get double value from dict with default."""
        if not data:
            return default
        value = data.get(key)
        if isinstance(value, (int, float)):
            return float(value)
        try:
            return float(value)
        except (ValueError, TypeError):
            return default
    
    def _get_integer_value(self, data: Dict[str, Any], key: str, default: int = 0) -> int:
        """Get integer value from dict with default."""
        if not data:
            return default
        value = data.get(key)
        if isinstance(value, int):
            return value
        try:
            return int(value)
        except (ValueError, TypeError):
            return default
    
    def _get_boolean_value(self, data: Dict[str, Any], key: str, default: bool = False) -> bool:
        """Get boolean value from dict with default."""
        if not data:
            return default
        value = data.get(key)
        if isinstance(value, bool):
            return value
        try:
            return bool(value)
        except (ValueError, TypeError):
            return default
