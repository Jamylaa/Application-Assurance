import re
import json
import logging
from typing import Dict, Any, Optional
import httpx
from app.config import settings

logger = logging.getLogger(__name__)


class AIExtractionService:
    """Service for AI-based data extraction using GitHub Models API."""

    def __init__(self):
        self.api_key = settings.github_api_key
        self.model_name = settings.github_model
        self.api_url = settings.github_url
        self.timeout_seconds = settings.github_timeout_seconds
        self.max_retries = settings.github_max_retries
        self.retry_delay_ms = settings.github_retry_delay_ms
        self.github_enabled = settings.github_enabled
        self.ai_extraction_enabled = settings.ai_extraction_enabled
        self.fallback_on_error = settings.fallback_on_ai_error

        logger.info("🤖 AIExtractionService initialized")
        logger.info(f"🔑 API Key configured: {bool(self.api_key and self.api_key.strip())}")
        logger.info(f"🔑 API Key length: {len(self.api_key) if self.api_key else 0}")
        logger.info(f"🔧 GitHub enabled: {self.github_enabled}")
        logger.info(f"🔧 AI extraction enabled: {self.ai_extraction_enabled}")
        logger.info(f"🔧 Fallback on error: {self.fallback_on_error}")
        logger.info(f"🔧 Model: {self.model_name}")
        logger.info(f"🔧 API URL: {self.api_url}")
        logger.info(f"🔧 Timeout: {self.timeout_seconds}s")
        logger.info(f"🔧 Max retries: {self.max_retries}")

    def is_ai_available(self) -> bool:
        return (
            self.api_key is not None
            and self.api_key.strip() != ""
            and self.github_enabled
            and self.ai_extraction_enabled
        )

    # ------------------------------------------------------------------
    # Extraction methods
    # ------------------------------------------------------------------

    def extract_garantie_data(self, prompt: str) -> Dict[str, Any]:
        """Extract guarantee data from natural language prompt."""
        try:
            logger.info("=== EXTRACTION IA GARANTIE ===")
            logger.info(f"Prompt: {prompt}")

            enhanced_prompt = prompt + """

Extrais les informations pour créer une garantie d'assurance. IMPORTANT:
- Retourne un JSON avec: nom, description, domaine (domaine médical parmi: CONSULTATION_GENERALE, CARDIOLOGIE, DENTAIRE, OPHTALMOLOGIE, HOSPITALISATION, ORL, PHARMACIE, MATERNITE, GYNECOLOGIE, PEDIATRIE, KINESITHERAPIE, RADIOLOGIE, URGENCES_MEDICALES, etc.)
- tauxRemboursement (en décimal, ex: 0.8 pour 80%)
- typeMontant (TARIF_CONVENTIONNE/FRAIS_REELS/FORFAIT - EXACTEMENT ces valeurs)
- plafondAnnuel, plafondMensuel, plafondParActe
- franchise, coutMoyenParSinistre
- dureeMinContrat, dureeMaxContrat (DEUX valeurs distinctes, en mois)
- resiliableAnnuellement (true/false)
- statut (ACTIF/INACTIF)
- Pour les plages de durée (ex: '12 à 36 mois'), séparez en dureeMinContrat=12 et dureeMaxContrat=36
- Utilise null si vraiment absent.
"""

            result = self._call_github_ai_with_retry(enhanced_prompt)
            logger.info(f"Résultat IA brut: {result}")

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
- typeProduit (SANTE/AUTO/HABITATION/VIE)
- statut (ACTIF/INACTIF)
Utilise null si absent.
"""
            return self._call_github_ai_with_retry(enhanced_prompt)
        except Exception as e:
            logger.error(f"Erreur extraction produit IA: {e}", exc_info=True)
            return {}

    def extract_pack_data(self, prompt: str) -> Dict[str, Any]:
        """Extract pack data including associated garanties from natural language prompt."""
        try:
            enhanced_prompt = prompt + """

Extrais les informations pour créer un pack d'assurance. Retourne UNIQUEMENT un objet JSON (pas de texte avant ou après).

Champs du pack:
- nom, description
- ageMin, ageMax (entiers)
- typeClients: "INDIVIDUEL", "FAMILLE", "SENIOR", "ETUDIANT" ou "ENTREPRISE"
- couvertureGeographique: "LOCAL", "NATIONAL" ou "INTERNATIONAL"
- prixMensuel (nombre décimal, ex: 95.0)
- dureeMinContrat, dureeMaxContrat (en mois, entiers)
- niveauCouverture: "BASIC", "PREMIUM" ou "GOLD"
- statut: "ACTIF" ou "INACTIF"
- nomProduit: nom exact du produit associé
- ancienneteContratMois: ancienneté minimale en mois (entier)
- garanties: liste de TOUTES les garanties mentionnées dans le prompt, chacune avec:
  {
    "nomGarantie": string,
    "tauxRemboursement": float entre 0 et 1 (ex: 0.9 pour 90%),
    "plafond": float (plafond en TND),
    "franchise": float,
    "delaiCarence": int (jours),
    "typeMontant": "FORFAIT" | "FRAIS_REELS" | "TARIF_CONVENTIONNE",
    "optionnelle": bool,
    "supplementPrix": float (0 si non mentionné),
    "priorite": int
  }

Règles importantes:
- niveauCouverture GOLD si "gold" dans le nom/description, PREMIUM si "premium", BASIC sinon
- typeClients SENIOR si "senior"/"sénior" dans le nom, FAMILLE si "famille", INDIVIDUEL sinon
- garanties: [] si aucune garantie mentionnée
- Utilise null pour les champs vraiment absents du texte
"""
            result = self._call_github_ai_with_retry(enhanced_prompt, max_tokens=2000)
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
            return self._call_github_ai_with_retry(enhanced_prompt)
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
            result = self._call_github_ai_with_retry(enhanced_prompt)

            if not result or not result.get("nomPack"):
                logger.warning("IA n'a pas extrait nomPack, utilisation du fallback regex")
                return self._extract_add_garantie_to_pack_data_fallback(prompt)

            return result
        except Exception as e:
            logger.error(f"Erreur extraction ajout garantie pack IA: {e}", exc_info=True)
            return self._extract_add_garantie_to_pack_data_fallback(prompt)

    # ------------------------------------------------------------------
    # API call
    # ------------------------------------------------------------------

    def _call_github_ai(self, prompt: str, max_tokens: int = 1000) -> Dict[str, Any]:
        """Call GitHub Models API for AI extraction."""
        if not self.is_ai_available():
            logger.warning("AI service not available, cannot call GitHub AI")
            return {}

        try:
            logger.info(f"🤖 Calling GitHub Models API with model: {self.model_name}")

            headers = {
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json"
            }

            payload = {
                "model": self.model_name,
                "messages": [
                    {
                        "role": "system",
                        "content": (
                            "You are a JSON extraction assistant for an insurance management system. "
                            "Always return ONLY valid JSON — no explanation, no markdown, no code fences. "
                            "Return a single JSON object starting with { and ending with }."
                        )
                    },
                    {
                        "role": "user",
                        "content": prompt
                    }
                ],
                "temperature": 0.1,
                "max_tokens": max_tokens
            }

            response = httpx.post(
                f"{self.api_url}/chat/completions",
                headers=headers,
                json=payload,
                timeout=self.timeout_seconds
            )

            logger.debug(f"🔵 Response status: {response.status_code}")

            if response.status_code == 200:
                result = response.json()
                if "choices" in result and len(result["choices"]) > 0:
                    content = result["choices"][0]["message"]["content"]
                    logger.info(f"✅ AI response received: {content[:300]}...")
                    parsed = self._parse_ai_response(content)
                    if parsed:
                        logger.info(f"✅ Parsed AI response keys: {list(parsed.keys())}")
                        if "garanties" in parsed:
                            logger.info(f"✅ Garanties extraites: {len(parsed['garanties'])}")
                        return parsed
                    else:
                        logger.warning("⚠️ Could not parse AI response as JSON")
                        return {}
                else:
                    logger.warning("⚠️ No choices in AI response")
                    return {}
            else:
                logger.error(f"❌ AI API error: {response.status_code} - {response.text}")
                return {}

        except httpx.TimeoutException:
            logger.error("❌ AI API timeout")
            return {}
        except httpx.ConnectError:
            logger.error("❌ AI API connection error")
            return {}
        except Exception as e:
            logger.error(f"❌ AI API error: {e}", exc_info=True)
            return {}

    def _call_github_ai_with_retry(self, prompt: str, max_tokens: int = 1000) -> Dict[str, Any]:
        """Call GitHub AI with retry logic."""
        if not self.is_ai_available():
            logger.warning("API KEY non configurée, fallback patterns")
            return {}

        attempt = 0
        while attempt < self.max_retries:
            try:
                logger.debug(f"Tentative {attempt + 1}/{self.max_retries} GitHub AI")
                result = self._call_github_ai(prompt, max_tokens=max_tokens)
                if result:
                    logger.info("Extraction IA réussie")
                    return result
            except Exception as e:
                logger.warning(f"Tentative {attempt + 1} échouée: {e}")
            attempt += 1
            if attempt < self.max_retries:
                import time
                time.sleep(self.retry_delay_ms / 1000)

        if self.fallback_on_error:
            logger.warning("Fallback patterns activé")
            return {}

        return {}

    # ------------------------------------------------------------------
    # JSON parsing — handles objects with nested arrays
    # ------------------------------------------------------------------

    def _parse_ai_response(self, ai_response: str) -> Dict[str, Any]:
        """Parse AI response to extract JSON, including nested arrays."""
        if not ai_response:
            return {}

        # 1. Try direct parse (AI returned clean JSON)
        try:
            cleaned = ai_response.strip()
            return json.loads(cleaned)
        except json.JSONDecodeError:
            pass

        # 2. Try stripping markdown code fences ```json ... ```
        fence_match = re.search(r'```(?:json)?\s*(\{.*?\})\s*```', ai_response, re.DOTALL)
        if fence_match:
            try:
                return json.loads(self._clean_json_string(fence_match.group(1)))
            except json.JSONDecodeError:
                pass

        # 3. Brace-counting extraction — correctly handles nested objects and arrays
        start = ai_response.find('{')
        if start == -1:
            logger.warning("Aucun JSON trouvé dans la réponse")
            return {}

        depth = 0
        in_string = False
        escape_next = False
        for i, ch in enumerate(ai_response[start:], start):
            if escape_next:
                escape_next = False
                continue
            if ch == '\\' and in_string:
                escape_next = True
                continue
            if ch == '"':
                in_string = not in_string
                continue
            if in_string:
                continue
            if ch == '{':
                depth += 1
            elif ch == '}':
                depth -= 1
                if depth == 0:
                    json_str = ai_response[start:i + 1]
                    try:
                        return json.loads(self._clean_json_string(json_str))
                    except json.JSONDecodeError as e:
                        logger.warning(f"Erreur parsing JSON extrait: {e}")
                        return {}

        logger.warning("Aucun JSON complet trouvé dans la réponse")
        return {}

    def _clean_json_string(self, json_str: str) -> str:
        """Clean JSON string for parsing."""
        # Remove control characters (except valid whitespace)
        json_str = re.sub(r'[\x00-\x08\x0b\x0c\x0e-\x1F]', '', json_str)
        return json_str

    # ------------------------------------------------------------------
    # Fallback / heuristic logic
    # ------------------------------------------------------------------

    def _apply_pack_fallback_logic(self, data: Dict[str, Any], original_prompt: str) -> Dict[str, Any]:
        """Apply fallback logic for pack data extraction."""
        if not data:
            return data

        pack_name = self._get_string_value(data, "nom", "").lower()
        prompt_lower = original_prompt.lower()

        # Deduce coverage level if missing
        if not data.get("niveauCouverture"):
            niveau = self._deduce_coverage_level(pack_name, prompt_lower)
            if niveau:
                data["niveauCouverture"] = niveau
                logger.info(f"Niveau de couverture déduit: {niveau}")

        # Deduce client type if missing
        if not data.get("typeClients"):
            type_client = self._deduce_client_type(pack_name, prompt_lower)
            if type_client:
                data["typeClients"] = type_client
                logger.info(f"Type de client déduit: {type_client}")

        # Extract price with regex if missing or zero
        if not data.get("prixMensuel") or data.get("prixMensuel", 0) <= 0:
            prix = self._extract_price_from_prompt(prompt_lower)
            if prix and prix > 0:
                data["prixMensuel"] = prix
                logger.info(f"Prix extrait via regex: {prix}")

        # Ensure garanties key exists (even if empty list)
        if "garanties" not in data:
            data["garanties"] = []

        return data

    def _deduce_coverage_level(self, pack_name: str, prompt: str) -> Optional[str]:
        """Deduce coverage level from pack name or prompt."""
        # Gold must be checked before premium (gold ⊂ premium-tier)
        if "gold" in pack_name or "gold" in prompt:
            return "GOLD"
        elif "premium" in pack_name or "premium" in prompt:
            return "PREMIUM"
        elif any(kw in pack_name or kw in prompt for kw in ["essentiel", "basic", "standard", "confort"]):
            return "BASIC"
        return None

    def _deduce_client_type(self, pack_name: str, prompt: str) -> Optional[str]:
        """Deduce client type from pack name or prompt."""
        if "famille" in pack_name or "enfants" in pack_name or "famille" in prompt:
            return "FAMILLE"
        elif "senior" in pack_name or "sénior" in pack_name or "senior" in prompt or "sénior" in prompt:
            return "SENIOR"
        elif "étudiant" in pack_name or "etudiant" in pack_name or "étudiant" in prompt:
            return "ETUDIANT"
        elif "entreprise" in pack_name or "entreprise" in prompt:
            return "ENTREPRISE"
        return "INDIVIDUEL"

    def _extract_price_from_prompt(self, prompt: str) -> Optional[float]:
        """Extract price from prompt using regex."""
        # Context-aware: prefer "prix de X" or "X TND/mois" patterns
        patterns = [
            r'prix\s+(?:mensuel\s+)?(?:de\s+)?(\d+(?:[.,]\d+)?)',
            r'(\d+(?:[.,]\d+)?)\s*(?:tnd|dt|dinars?)\s*(?:/mois|par mois|mensuel)',
            r'(\d+(?:[.,]\d+)?)\s*(?:€|euros?|eur)',
        ]
        for pattern in patterns:
            m = re.search(pattern, prompt, re.IGNORECASE)
            if m:
                try:
                    return float(m.group(1).replace(",", "."))
                except ValueError:
                    pass
        return None

    def _extract_add_garantie_to_pack_data_fallback(self, prompt: str) -> Dict[str, Any]:
        """Fallback regex extraction for adding guarantee to pack."""
        result = {}
        prompt_lower = prompt.lower()

        pack_pattern = re.compile(
            r'(?:au\s+pack|pack\s+)([a-zA-ZàâäéèêëïîôöùûüÿçÀÂÄÉÈÊËÏÎÔÖÙÛÜŸÇ\s]+?)(?:\s+avec|\s+et|,|\.$|$)',
            re.IGNORECASE
        )
        m = pack_pattern.search(prompt)
        if m:
            result["nomPack"] = m.group(1).strip()

        garantie_pattern = re.compile(
            r'(?:la\s+garantie|garantie\s+)([a-zA-ZàâäéèêëïîôöùûüÿçÀÂÄÉÈÊËÏÎÔÖÙÛÜŸÇ\s]+?)(?:\s+avec|\s+et|\s+un|,|\.$|$)',
            re.IGNORECASE
        )
        m = garantie_pattern.search(prompt)
        if m:
            result["nomGarantie"] = m.group(1).strip()

        taux_pattern = re.compile(
            r'(?:taux\s+de\s+remboursement\s*(?:de|:)?\s*|remboursement\s*(?:de|:)?\s*)(\d+)\s*%',
            re.IGNORECASE
        )
        m = taux_pattern.search(prompt)
        if m:
            try:
                result["tauxRemboursement"] = float(m.group(1)) / 100.0
            except ValueError:
                pass

        plafond_pattern = re.compile(r'plafond\s*(?:de\s+)?(\d+)', re.IGNORECASE)
        m = plafond_pattern.search(prompt)
        if m:
            try:
                result["plafond"] = float(m.group(1))
            except ValueError:
                pass

        franchise_pattern = re.compile(r'franchise\s*(?:de\s+)?(\d+)', re.IGNORECASE)
        m = franchise_pattern.search(prompt)
        if m:
            try:
                result["franchise"] = float(m.group(1))
            except ValueError:
                pass

        if "optionnelle" in prompt_lower or "optionnel" in prompt_lower:
            result["optionnelle"] = True

        return result

    # ------------------------------------------------------------------
    # Type-safe dict helpers
    # ------------------------------------------------------------------

    def _get_string_value(self, data: Dict[str, Any], key: str, default: str = "") -> str:
        if not data:
            return default
        value = data.get(key)
        if value is None:
            return default
        return str(value).strip() or default

    def _get_double_value(self, data: Dict[str, Any], key: str, default: float = 0.0) -> float:
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
        if not data:
            return default
        value = data.get(key)
        if isinstance(value, bool):
            return value
        try:
            return bool(value)
        except (ValueError, TypeError):
            return default
