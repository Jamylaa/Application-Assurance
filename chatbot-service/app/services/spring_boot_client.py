import logging
from typing import List, Optional
import httpx
from app.config import settings
from app.models.schemas import GarantieDTO, PackDTO, ProduitDTO

logger = logging.getLogger(__name__)


class SpringBootClient:
    """Client for communicating with Spring Boot backend."""
    
    def __init__(self):
        self.base_url = settings.spring_boot_base_url
        self.timeout = 30.0
    
    async def create_garantie(self, garantie: GarantieDTO) -> GarantieDTO:
        """Create a guarantee via Spring Boot API."""
        url = f"{self.base_url}/garanties"
        data = garantie.model_dump(exclude_none=True, exclude={"id_garantie"})
        
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.post(url, json=data)
            response.raise_for_status()
            return GarantieDTO(**response.json())
    
    async def create_produit(self, produit: ProduitDTO) -> ProduitDTO:
        """Create a product via Spring Boot API."""
        url = f"{self.base_url}/produits"
        data = produit.model_dump(exclude_none=True, exclude={"id_produit"})
        
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.post(url, json=data)
            response.raise_for_status()
            return ProduitDTO(**response.json())
    
    async def create_pack(self, pack: PackDTO) -> PackDTO:
        """Create a pack via Spring Boot API."""
        url = f"{self.base_url}/packs"
        data = pack.model_dump(exclude_none=True, exclude={"id_pack"})
        
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.post(url, json=data)
            response.raise_for_status()
            return PackDTO(**response.json())
    
    async def get_all_packs(self) -> List[PackDTO]:
        """Get all packs from Spring Boot API."""
        url = f"{self.base_url}/packs"
        
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.get(url)
            response.raise_for_status()
            return [PackDTO(**item) for item in response.json()]
    
    async def get_pack_by_name(self, name: str) -> Optional[PackDTO]:
        """Get a pack by name from Spring Boot API."""
        url = f"{self.base_url}/packs/search/nom/{name}"
        
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.get(url)
            if response.status_code == 404:
                return None
            response.raise_for_status()
            return PackDTO(**response.json())
    
    async def get_garantie_by_name(self, name: str) -> Optional[GarantieDTO]:
        """Get a guarantee by name from Spring Boot API."""
        url = f"{self.base_url}/garanties/search/nom/{name}"
        
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.get(url)
            if response.status_code == 404:
                return None
            response.raise_for_status()
            return GarantieDTO(**response.json())
    
    async def get_produit_by_name(self, name: str) -> Optional[ProduitDTO]:
        """Get a product by name from Spring Boot API."""
        url = f"{self.base_url}/produits/search/nom/{name}"
        
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.get(url)
            if response.status_code == 404:
                return None
            response.raise_for_status()
            return ProduitDTO(**response.json())
    
    async def add_garantie_to_pack(self, pack_id: str, garantie_id: str, 
                                   taux_remboursement: float, plafond: float,
                                   franchise: float = 0.0, optionnelle: bool = False) -> bool:
        """Add a guarantee to a pack via Spring Boot API."""
        url = f"{self.base_url}/packs/{pack_id}/garanties"
        data = {
            "garantieId": garantie_id,
            "tauxRemboursement": taux_remboursement,
            "plafond": plafond,
            "franchise": franchise,
            "optionnelle": optionnelle
        }
        
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.post(url, json=data)
            response.raise_for_status()
            return True
    
    # Synchronous versions for compatibility with non-async code
    
    def create_garantie_sync(self, garantie: GarantieDTO) -> GarantieDTO:
        """Synchronous version of create_garantie."""
        import asyncio
        return asyncio.run(self.create_garantie(garantie))
    
    def create_produit_sync(self, produit: ProduitDTO) -> ProduitDTO:
        """Synchronous version of create_produit."""
        import asyncio
        return asyncio.run(self.create_produit(produit))
    
    def create_pack_sync(self, pack: PackDTO) -> PackDTO:
        """Synchronous version of create_pack."""
        import asyncio
        return asyncio.run(self.create_pack(pack))
    
    def get_all_packs_sync(self) -> List[PackDTO]:
        """Synchronous version of get_all_packs."""
        import asyncio
        return asyncio.run(self.get_all_packs())
    
    def get_pack_by_name_sync(self, name: str) -> Optional[PackDTO]:
        """Synchronous version of get_pack_by_name."""
        import asyncio
        return asyncio.run(self.get_pack_by_name(name))
    
    def get_garantie_by_name_sync(self, name: str) -> Optional[GarantieDTO]:
        """Synchronous version of get_garantie_by_name."""
        import asyncio
        return asyncio.run(self.get_garantie_by_name(name))
    
    def get_produit_by_name_sync(self, name: str) -> Optional[ProduitDTO]:
        """Synchronous version of get_produit_by_name."""
        import asyncio
        return asyncio.run(self.get_produit_by_name(name))
