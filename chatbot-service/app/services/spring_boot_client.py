import logging
from typing import List, Optional, Dict, Any
import httpx
import json
from app.config import settings
from app.models.schemas import GarantieDTO, PackDTO, ProduitDTO, PackGarantieDTO

logger = logging.getLogger(__name__)

class SpringBootClient:
    """Client for communicating with Spring Boot backend."""
    
    def __init__(self):
        self.base_url = settings.spring_boot_base_url
        self.timeout = settings.spring_boot_timeout
        self.max_retries = settings.spring_boot_max_retries
        self.retry_delay = settings.spring_boot_retry_delay
    
    def _handle_response(self, response: httpx.Response, operation: str) -> Any:
        """Handle HTTP response with proper error handling and logging."""
        try:
            if response.status_code == 200:
                return response.json()
            elif response.status_code == 201:
                logger.info(f"✅ {operation} - Created successfully")
                return response.json()
            elif response.status_code == 204:
                logger.info(f"✅ {operation} - No content (successful)")
                return None
            elif response.status_code == 400:
                error_detail = response.text
                logger.error(f"❌ {operation} - Bad Request: {error_detail}")
                raise Exception(f"Bad Request: {error_detail}")
            elif response.status_code == 401:
                logger.error(f"❌ {operation} - Unauthorized")
                raise Exception("Unauthorized: Invalid or missing JWT token")
            elif response.status_code == 403:
                logger.error(f"❌ {operation} - Forbidden")
                raise Exception("Forbidden: Insufficient permissions")
            elif response.status_code == 404:
                logger.error(f"❌ {operation} - Not Found")
                raise Exception("Resource not found")
            elif response.status_code == 422:
                error_detail = response.text
                logger.error(f"❌ {operation} - Unprocessable Entity: {error_detail}")
                raise Exception(f"Validation error: {error_detail}")
            elif response.status_code == 500:
                logger.error(f"❌ {operation} - Internal Server Error")
                raise Exception("Internal server error")
            else:
                logger.error(f"❌ {operation} - Unexpected status code: {response.status_code}")
                raise Exception(f"Unexpected error: {response.status_code}")
        except json.JSONDecodeError as e:
            logger.error(f"❌ {operation} - JSON decode error: {e}")
            raise Exception(f"Invalid JSON response: {e}")
    
    async def _make_request_with_retry(self, method: str, url: str, **kwargs) -> httpx.Response:
        """Make HTTP request with retry logic."""
        last_error = None
        for attempt in range(self.max_retries):
            try:
                async with httpx.AsyncClient(timeout=self.timeout) as client:
                    response = await client.request(method, url, **kwargs)
                    return response
            except httpx.TimeoutException as e:
                last_error = e
                logger.warning(f"⚠️ Timeout on attempt {attempt + 1}/{self.max_retries} for {url}")
                if attempt < self.max_retries - 1:
                    import asyncio
                    await asyncio.sleep(self.retry_delay / 1000)
            except httpx.ConnectError as e:
                last_error = e
                logger.warning(f"⚠️ Connection error on attempt {attempt + 1}/{self.max_retries} for {url}")
                if attempt < self.max_retries - 1:
                    import asyncio
                    await asyncio.sleep(self.retry_delay / 1000)
        
        logger.error(f"❌ Failed to connect to Spring Boot service after {self.max_retries} attempts")
        raise Exception(f"Failed to connect to Spring Boot service: {last_error}")
    
    async def create_garantie(self, garantie: GarantieDTO, jwt_token: Optional[str] = None) -> GarantieDTO:
        """Create a guarantee via Spring Boot API."""
        url = f"{self.base_url}/garanties"
        camel_case_data = garantie.model_dump(mode='json', exclude_none=True, exclude={"id_garantie"}, by_alias=True)

        headers = {"Content-Type": "application/json"}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"

        logger.info(f"🔵 Creating garantie: {garantie.nom_garantie}")
        logger.info(f"🔵 Request data: {json.dumps(camel_case_data, ensure_ascii=False)}")

        response = await self._make_request_with_retry("POST", url, json=camel_case_data, headers=headers)
        result = self._handle_response(response, "Create Garantie")

        return GarantieDTO.model_validate(result)
    
    async def create_produit(self, produit: ProduitDTO, jwt_token: Optional[str] = None) -> ProduitDTO:
        """Create a product via Spring Boot API."""
        url = f"{self.base_url}/produits"
        camel_case_data = produit.model_dump(mode='json', exclude_none=True, exclude={"id_produit"}, by_alias=True)

        headers = {"Content-Type": "application/json"}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"

        logger.info(f"🔵 Creating produit: {produit.nom_produit}")
        logger.info(f"🔵 Request data: {json.dumps(camel_case_data, ensure_ascii=False)}")

        response = await self._make_request_with_retry("POST", url, json=camel_case_data, headers=headers)
        result = self._handle_response(response, "Create Produit")

        return ProduitDTO.model_validate(result)
    
    async def create_pack(self, pack: PackDTO, jwt_token: Optional[str] = None) -> PackDTO:
        """Create a pack via Spring Boot API."""
        url = f"{self.base_url}/packs"
        camel_case_data = pack.model_dump(mode='json', exclude_none=True, exclude={"id_pack", "garanties", "nom_produit"}, by_alias=True)

        headers = {"Content-Type": "application/json"}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"

        logger.info(f"🔵 Creating pack: {pack.nom_pack}")
        logger.info(f"🔵 Request data: {json.dumps(camel_case_data, ensure_ascii=False)}")

        response = await self._make_request_with_retry("POST", url, json=camel_case_data, headers=headers)
        result = self._handle_response(response, "Create Pack")

        return PackDTO.model_validate(result)
    
    async def get_pack_detail(self, pack_id: str, jwt_token: Optional[str] = None) -> Dict[str, Any]:
        """Détail enrichi d'un pack (idGaranties + domainesMedicaux calculés côté serveur)."""
        url = f"{self.base_url}/packs/{pack_id}/detail"

        headers = {}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"

        response = await self._make_request_with_retry("GET", url, headers=headers)
        return self._handle_response(response, "Get Pack Detail") or {}

    async def get_all_packs(self, jwt_token: Optional[str] = None) -> List[PackDTO]:
        """Get all packs from Spring Boot API."""
        url = f"{self.base_url}/packs"

        headers = {}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"

        logger.info("🔵 Fetching all packs")

        response = await self._make_request_with_retry("GET", url, headers=headers)
        result = self._handle_response(response, "Get All Packs")

        return [PackDTO.model_validate(item) for item in result]

    async def get_all_produits(self, jwt_token: Optional[str] = None) -> List[ProduitDTO]:
        """Get all products from Spring Boot API."""
        url = f"{self.base_url}/produits"

        headers = {}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"

        logger.info("🔵 Fetching all produits")

        response = await self._make_request_with_retry("GET", url, headers=headers)
        result = self._handle_response(response, "Get All Produits")

        return [ProduitDTO.model_validate(item) for item in result]
    
    async def get_pack_by_name(self, name: str, jwt_token: Optional[str] = None) -> Optional[PackDTO]:
        """Get a pack by name from Spring Boot API."""
        url = f"{self.base_url}/packs/search?nomPack={name}"
        
        headers = {}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"
        
        logger.info(f"🔵 Searching pack by name: {name}")
        
        try:
            response = await self._make_request_with_retry("GET", url, headers=headers)
            result = self._handle_response(response, "Get Pack By Name")
            if result and len(result) > 0:
                return PackDTO.model_validate(result[0])
            return None
        except Exception as e:
            if "404" in str(e) or "Resource not found" in str(e):
                return None
            raise

    async def get_garantie_by_name(self, name: str, jwt_token: Optional[str] = None) -> Optional[GarantieDTO]:
        """Get a guarantee by name from Spring Boot API."""
        url = f"{self.base_url}/garanties/search?nomGarantie={name}"

        headers = {}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"

        logger.info(f"🔵 Searching garantie by name: {name}")

        try:
            response = await self._make_request_with_retry("GET", url, headers=headers)
            result = self._handle_response(response, "Get Garantie By Name")
            if result and len(result) > 0:
                return GarantieDTO.model_validate(result[0])
            return None
        except Exception as e:
            if "404" in str(e) or "Resource not found" in str(e):
                return None
            raise

    async def get_produit_by_name(self, name: str, jwt_token: Optional[str] = None) -> Optional[ProduitDTO]:
        """Get a product by name from Spring Boot API."""
        url = f"{self.base_url}/produits/search?nom={name}"

        headers = {}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"

        logger.info(f"🔵 Searching produit by name: {name}")

        try:
            response = await self._make_request_with_retry("GET", url, headers=headers)
            result = self._handle_response(response, "Get Produit By Name")
            if result and len(result) > 0:
                return ProduitDTO.model_validate(result[0])
            return None
        except Exception as e:
            if "404" in str(e) or "Resource not found" in str(e):
                return None
            raise
    
    async def add_garantie_to_pack(self, pack_id: str, garantie_id: str,
                                   pack_garantie_dto, jwt_token: Optional[str] = None) -> bool:
        """Add a guarantee to a pack via Spring Boot API."""
        url = f"{self.base_url}/packs/{pack_id}/garanties/{garantie_id}"

        camel_case_data = pack_garantie_dto.model_dump(mode='json', exclude_none=True, by_alias=True)

        headers = {"Content-Type": "application/json"}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"
        
        logger.info(f"🔵 Adding garantie {garantie_id} to pack {pack_id}")
        logger.debug(f"🔵 Request data: {json.dumps(camel_case_data, indent=2)}")
        
        response = await self._make_request_with_retry("POST", url, json=camel_case_data, headers=headers)
        self._handle_response(response, "Add Garantie To Pack")

        return True

    async def get_pack_garanties(self, pack_id: str, jwt_token: Optional[str] = None) -> List[PackGarantieDTO]:
        """Liste les associations pack-garantie d'un pack."""
        url = f"{self.base_url}/packs/{pack_id}/garanties"

        headers = {}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"

        response = await self._make_request_with_retry("GET", url, headers=headers)
        result = self._handle_response(response, "Get Pack Garanties") or []
        return [PackGarantieDTO.model_validate(item) for item in result]

    async def update_pack_garantie(self, id_pack_garantie: str, pack_garantie_dto,
                                    jwt_token: Optional[str] = None) -> PackGarantieDTO:
        """Met à jour la configuration d'une association pack-garantie existante."""
        url = f"{self.base_url}/packs/associations/{id_pack_garantie}"
        camel_case_data = pack_garantie_dto.model_dump(mode='json', exclude_none=True, by_alias=True)

        headers = {"Content-Type": "application/json"}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"

        response = await self._make_request_with_retry("PUT", url, json=camel_case_data, headers=headers)
        result = self._handle_response(response, "Update Pack Garantie")
        return PackGarantieDTO.model_validate(result)

    async def update_garantie(self, id_garantie: str, garantie: GarantieDTO, jwt_token: Optional[str] = None) -> GarantieDTO:
        """Update a guarantee via Spring Boot API."""
        url = f"{self.base_url}/garanties/{id_garantie}"
        camel_case_data = garantie.model_dump(mode='json', exclude_none=True, exclude={"id_garantie"}, by_alias=True)

        headers = {"Content-Type": "application/json"}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"

        logger.info(f"🔵 Updating garantie: {id_garantie}")

        response = await self._make_request_with_retry("PUT", url, json=camel_case_data, headers=headers)
        result = self._handle_response(response, "Update Garantie")

        return GarantieDTO.model_validate(result)

    async def update_produit(self, id_produit: str, produit: ProduitDTO, jwt_token: Optional[str] = None) -> ProduitDTO:
        """Update a product via Spring Boot API."""
        url = f"{self.base_url}/produits/{id_produit}"
        camel_case_data = produit.model_dump(mode='json', exclude_none=True, exclude={"id_produit"}, by_alias=True)

        headers = {"Content-Type": "application/json"}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"

        logger.info(f"🔵 Updating produit: {id_produit}")

        response = await self._make_request_with_retry("PUT", url, json=camel_case_data, headers=headers)
        result = self._handle_response(response, "Update Produit")

        return ProduitDTO.model_validate(result)

    async def update_pack(self, id_pack: str, pack: PackDTO, jwt_token: Optional[str] = None) -> PackDTO:
        """Update a pack via Spring Boot API."""
        url = f"{self.base_url}/packs/{id_pack}"
        camel_case_data = pack.model_dump(mode='json', exclude_none=True, exclude={"id_pack", "garanties", "nom_produit"}, by_alias=True)

        headers = {"Content-Type": "application/json"}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"

        logger.info(f"🔵 Updating pack: {id_pack}")

        response = await self._make_request_with_retry("PUT", url, json=camel_case_data, headers=headers)
        result = self._handle_response(response, "Update Pack")

        return PackDTO.model_validate(result)
    
    async def delete_garantie(self, id_garantie: str, jwt_token: Optional[str] = None) -> bool:
        """Delete a guarantee via Spring Boot API."""
        url = f"{self.base_url}/garanties/{id_garantie}"
        
        headers = {}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"
        
        logger.info(f"🔵 Deleting garantie: {id_garantie}")
        
        response = await self._make_request_with_retry("DELETE", url, headers=headers)
        self._handle_response(response, "Delete Garantie")
        
        return True
    
    async def delete_produit(self, id_produit: str, jwt_token: Optional[str] = None) -> bool:
        """Delete a product via Spring Boot API."""
        url = f"{self.base_url}/produits/{id_produit}"
        
        headers = {}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"
        
        logger.info(f"🔵 Deleting produit: {id_produit}")
        
        response = await self._make_request_with_retry("DELETE", url, headers=headers)
        self._handle_response(response, "Delete Produit")
        
        return True
    
    async def delete_pack(self, id_pack: str, jwt_token: Optional[str] = None) -> bool:
        """Delete a pack via Spring Boot API."""
        url = f"{self.base_url}/packs/{id_pack}"
        
        headers = {}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"
        
        logger.info(f"🔵 Deleting pack: {id_pack}")
        
        response = await self._make_request_with_retry("DELETE", url, headers=headers)
        self._handle_response(response, "Delete Pack")
        
        return True
    
    # -----------------------------------------------------------------------
    # Helper: run an async coroutine from a sync context.
    # asyncio.run() raises RuntimeError when called from inside a running
    # event loop (FastAPI/uvicorn). In that case we spin up a fresh thread
    # with its own event loop and block until it finishes.
    # -----------------------------------------------------------------------
    def _run_sync(self, coro):
        import asyncio
        import concurrent.futures
        try:
            asyncio.get_running_loop()
            # Already inside a running loop — delegate to a worker thread
            with concurrent.futures.ThreadPoolExecutor(max_workers=1) as pool:
                return pool.submit(asyncio.run, coro).result()
        except RuntimeError:
            # No running loop — safe to call asyncio.run() directly
            return asyncio.run(coro)

    # Synchronous versions for compatibility with non-async code
    def create_garantie_sync(self, garantie: GarantieDTO, jwt_token: Optional[str] = None) -> GarantieDTO:
        return self._run_sync(self.create_garantie(garantie, jwt_token))

    def create_produit_sync(self, produit: ProduitDTO, jwt_token: Optional[str] = None) -> ProduitDTO:
        return self._run_sync(self.create_produit(produit, jwt_token))

    def create_pack_sync(self, pack: PackDTO, jwt_token: Optional[str] = None) -> PackDTO:
        return self._run_sync(self.create_pack(pack, jwt_token))

    def get_pack_detail_sync(self, pack_id: str, jwt_token: Optional[str] = None) -> Dict[str, Any]:
        return self._run_sync(self.get_pack_detail(pack_id, jwt_token))

    def get_all_packs_sync(self, jwt_token: Optional[str] = None) -> List[PackDTO]:
        return self._run_sync(self.get_all_packs(jwt_token))

    def get_all_produits_sync(self, jwt_token: Optional[str] = None) -> List[ProduitDTO]:
        return self._run_sync(self.get_all_produits(jwt_token))

    def get_pack_by_name_sync(self, name: str, jwt_token: Optional[str] = None) -> Optional[PackDTO]:
        return self._run_sync(self.get_pack_by_name(name, jwt_token))

    def get_garantie_by_name_sync(self, name: str, jwt_token: Optional[str] = None) -> Optional[GarantieDTO]:
        return self._run_sync(self.get_garantie_by_name(name, jwt_token))

    def get_produit_by_name_sync(self, name: str, jwt_token: Optional[str] = None) -> Optional[ProduitDTO]:
        return self._run_sync(self.get_produit_by_name(name, jwt_token))

    def update_garantie_sync(self, id_garantie: str, garantie: GarantieDTO, jwt_token: Optional[str] = None) -> GarantieDTO:
        return self._run_sync(self.update_garantie(id_garantie, garantie, jwt_token))

    def update_produit_sync(self, id_produit: str, produit: ProduitDTO, jwt_token: Optional[str] = None) -> ProduitDTO:
        return self._run_sync(self.update_produit(id_produit, produit, jwt_token))

    def update_pack_sync(self, id_pack: str, pack: PackDTO, jwt_token: Optional[str] = None) -> PackDTO:
        return self._run_sync(self.update_pack(id_pack, pack, jwt_token))

    def delete_garantie_sync(self, id_garantie: str, jwt_token: Optional[str] = None) -> bool:
        return self._run_sync(self.delete_garantie(id_garantie, jwt_token))

    def delete_produit_sync(self, id_produit: str, jwt_token: Optional[str] = None) -> bool:
        return self._run_sync(self.delete_produit(id_produit, jwt_token))

    def delete_pack_sync(self, id_pack: str, jwt_token: Optional[str] = None) -> bool:
        return self._run_sync(self.delete_pack(id_pack, jwt_token))

    def add_garantie_to_pack_sync(self, pack_id: str, garantie_id: str,
                                   pack_garantie_dto, jwt_token: Optional[str] = None) -> bool:
        return self._run_sync(self.add_garantie_to_pack(pack_id, garantie_id, pack_garantie_dto, jwt_token))

    def get_pack_garanties_sync(self, pack_id: str, jwt_token: Optional[str] = None) -> List[PackGarantieDTO]:
        return self._run_sync(self.get_pack_garanties(pack_id, jwt_token))

    def update_pack_garantie_sync(self, id_pack_garantie: str, pack_garantie_dto,
                                   jwt_token: Optional[str] = None) -> PackGarantieDTO:
        return self._run_sync(self.update_pack_garantie(id_pack_garantie, pack_garantie_dto, jwt_token))
