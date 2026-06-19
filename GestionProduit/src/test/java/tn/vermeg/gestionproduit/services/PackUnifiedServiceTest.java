package tn.vermeg.gestionproduit.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.vermeg.gestionproduit.entities.*;
import tn.vermeg.gestionproduit.enums.*;
import tn.vermeg.gestionproduit.exceptions.ResourceNotFoundException;
import tn.vermeg.gestionproduit.repositories.GarantieRepository;
import tn.vermeg.gestionproduit.repositories.PackGarantieRepository;
import tn.vermeg.gestionproduit.repositories.PackUnifiedRepository;
import tn.vermeg.gestionproduit.repositories.ProduitRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class PackUnifiedServiceTest {

    @Mock
    private PackUnifiedRepository packRepository;
    
    @Mock
    private PackGarantieRepository packGarantieRepository;
    
    @Mock
    private GarantieRepository garantieRepository;
    
    @Mock
    private ProduitRepository produitRepository;

    @InjectMocks
    private PackUnifiedService packUnifiedService;

    private Pack packTest;
    private PackGarantie packGarantieTest;
    private Garantie garantieTest;
    private Produit produitTest;

    @BeforeEach
    void setUp() {
        // Setup Pack de test
        packTest = new Pack();
        packTest.setIdPack("1");
        packTest.setNomPack("Pack Santé Premium");
        packTest.setDescription("Pack santé complet avec garanties étendues");
        packTest.setPrixMensuel(150.0);
        packTest.setAgeMinimum(18);
        packTest.setAgeMaximum(70);
        packTest.setStatut(Statut.ACTIF);
        packTest.setNiveauCouverture(NiveauCouverture.PREMIUM);
        packTest.setCouvertureGeographique(CouvertureGeographique.NATIONAL);
        packTest.setDateCreation(Instant.now());
        packTest.setDateModification(Instant.now());

        // Setup PackGarantie de test
        packGarantieTest = new PackGarantie();
        packGarantieTest.setIdPackGarantie("pg1");
        packGarantieTest.setPackId("1");

        // Setup Garantie de test
        garantieTest = new Garantie();
        garantieTest.setIdGarantie("g1");
        garantieTest.setNomGarantie("Hospitalisation");
        garantieTest.setDescription("Couverture hospitalière complète");

        // Setup Produit de test
        produitTest = new Produit();
        produitTest.setIdProduit("p1");
        produitTest.setNomProduit("Assurance Santé");
        produitTest.setTypeProduit(TypeProduit.SANTE);
    }

    @Test
    void testGetAllPacks() {
        // Given
        List<Pack> packs = Arrays.asList(packTest);
        when(packRepository.findAll()).thenReturn(packs);

        // When
        List<Pack> result = packUnifiedService.getAllPacks();

        // Then
        assertEquals(1, result.size());
        assertEquals("Pack Santé Premium", result.get(0).getNomPack());
        verify(packRepository, times(1)).findAll();
    }

    @Test
    void testGetPackById_Success() {
        // Given
        when(packRepository.findById("1")).thenReturn(Optional.of(packTest));

        // When
        Pack result = packUnifiedService.getPackById("1");

        // Then
        assertNotNull(result);
        assertEquals("Pack Santé Premium", result.getNomPack());
        verify(packRepository, times(1)).findById("1");
    }

    @Test
    void testGetPackById_NotFound() {
        // Given
        when(packRepository.findById("999")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            packUnifiedService.getPackById("999");
        });
        verify(packRepository, times(1)).findById("999");
    }

    @Test
    void testCreatePack_Success() {
        // Given
        Pack newPack = new Pack();
        newPack.setNomPack("Nouveau Pack");
        newPack.setDescription("Description du nouveau pack");
        newPack.setPrixMensuel(100.0);
        newPack.setAgeMinimum(18);
        newPack.setAgeMaximum(65);
        newPack.setCouvertureGeographique(CouvertureGeographique.NATIONAL);
        newPack.setNiveauCouverture(NiveauCouverture.BASIC);

        when(packRepository.existsByNomPackIgnoreCase("Nouveau Pack")).thenReturn(false);
        when(packRepository.save(any(Pack.class))).thenReturn(newPack);

        // When
        Pack result = packUnifiedService.createPack(newPack);

        // Then
        assertNotNull(result);
        assertEquals(Statut.ACTIF, result.getStatut());
        verify(packRepository, times(1)).save(any(Pack.class));
    }

    @Test
    void testCreatePack_NameAlreadyExists() {
        // Given
        when(packRepository.existsByNomPackIgnoreCase("Pack Santé Premium")).thenReturn(true);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            packUnifiedService.createPack(packTest);
        });
        verify(packRepository, never()).save(any(Pack.class));
    }

    @Test
    void testUpdatePack_Success() {
        // Given
        Pack updatedDetails = new Pack();
        updatedDetails.setNomPack("Pack Santé Mis à Jour");
        updatedDetails.setDescription("Description mise à jour");
        updatedDetails.setPrixMensuel(200.0);
        updatedDetails.setAgeMinimum(20);
        updatedDetails.setAgeMaximum(75);
        updatedDetails.setStatut(Statut.ACTIF);
        updatedDetails.setNiveauCouverture(NiveauCouverture.PREMIUM);
        updatedDetails.setCouvertureGeographique(CouvertureGeographique.NATIONAL);

        when(packRepository.findById("1")).thenReturn(Optional.of(packTest));
        when(packRepository.save(any(Pack.class))).thenReturn(packTest);
        when(packRepository.existsByNomPackIgnoreCase("Pack Santé Mis à Jour")).thenReturn(false);

        // When
        Pack result = packUnifiedService.updatePack("1", updatedDetails);

        // Then
        assertNotNull(result);
        verify(packRepository, times(1)).save(any(Pack.class));
    }

    @Test
    void testDeletePack_Success() {
        // Given
        when(packRepository.findById("1")).thenReturn(Optional.of(packTest));
        when(packGarantieRepository.findByPackId("1")).thenReturn(Collections.emptyList());

        // When
        assertDoesNotThrow(() -> packUnifiedService.deletePack("1"));

        // Then
        verify(packRepository, times(1)).deleteById("1");
    }

    @Test
    void testDeletePack_HasAssociatedGaranties() {
        // Given
        when(packRepository.findById("1")).thenReturn(Optional.of(packTest));
        when(packGarantieRepository.findByPackId("1")).thenReturn(Arrays.asList(packGarantieTest));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            packUnifiedService.deletePack("1");
        });
        verify(packRepository, never()).deleteById(anyString());
    }

    @Test
    void testSearchPacksByNom() {
        // Given
        List<Pack> packs = Arrays.asList(packTest);
        when(packRepository.findByNomPackIgnoreCaseContaining("Santé")).thenReturn(packs);

        // When
        List<Pack> result = packUnifiedService.searchPacksByNom("Santé");

        // Then
        assertEquals(1, result.size());
        assertTrue(result.get(0).getNomPack().contains("Santé"));
        verify(packRepository, times(1)).findByNomPackIgnoreCaseContaining("Santé");
    }

    @Test
    void testGetPacksByStatut() {
        // Given
        List<Pack> packs = Arrays.asList(packTest);
        when(packRepository.findByStatut(Statut.ACTIF)).thenReturn(packs);

        // When
        List<Pack> result = packUnifiedService.getPacksByStatut(Statut.ACTIF);

        // Then
        assertEquals(1, result.size());
        assertEquals(Statut.ACTIF, result.get(0).getStatut());
        verify(packRepository, times(1)).findByStatut(Statut.ACTIF);
    }

    @Test
    void testGetPacksByNiveau() {
        // Given
        List<Pack> packs = Arrays.asList(packTest);
        when(packRepository.findByNiveauCouverture(NiveauCouverture.PREMIUM)).thenReturn(packs);

        // When
        List<Pack> result = packUnifiedService.getPacksByNiveau(NiveauCouverture.PREMIUM);

        // Then
        assertEquals(1, result.size());
        assertEquals(NiveauCouverture.PREMIUM, result.get(0).getNiveauCouverture());
        verify(packRepository, times(1)).findByNiveauCouverture(NiveauCouverture.PREMIUM);
    }

    @Test
    void testGetPacksByTypeClient() {
        // Given
        List<Pack> packs = Arrays.asList(packTest);
        when(packRepository.findByTypeClientsContaining(TypeClient.INDIVIDUEL)).thenReturn(packs);

        // When
        List<Pack> result = packUnifiedService.getPacksByTypeClient(TypeClient.INDIVIDUEL);

        // Then
        assertEquals(1, result.size());
        verify(packRepository, times(1)).findByTypeClientsContaining(TypeClient.INDIVIDUEL);
    }

    @Test
    void testGetPacksByPrixRange() {
        // Given
        List<Pack> packs = Arrays.asList(packTest);
        when(packRepository.findByPrixMensuelBetween(100.0, 200.0)).thenReturn(packs);

        // When
        List<Pack> result = packUnifiedService.getPacksByPrixRange(100.0, 200.0);

        // Then
        assertEquals(1, result.size());
        verify(packRepository, times(1)).findByPrixMensuelBetween(100.0, 200.0);
    }

    @Test
    void testGetPacksByProduitId() {
        // Given
        List<Pack> packs = Arrays.asList(packTest);
        when(packRepository.findByProduitId("p1")).thenReturn(packs);

        // When
        List<Pack> result = packUnifiedService.getPacksByProduitId("p1");

        // Then
        assertEquals(1, result.size());
        verify(packRepository, times(1)).findByProduitId("p1");
    }

    @Test
    void testGetGarantiesByPackId() {
        // Given
        List<PackGarantie> garanties = Arrays.asList(packGarantieTest);
        when(packGarantieRepository.findByPackId("1")).thenReturn(garanties);

        // When
        List<PackGarantie> result = packUnifiedService.getGarantiesByPackId("1");

        // Then
        assertEquals(1, result.size());
        verify(packGarantieRepository, times(1)).findByPackId("1");
    }

    @Test
    void testAssociatePackToProduit_Success() {
        // Given
        when(packRepository.findById("1")).thenReturn(Optional.of(packTest));
        when(produitRepository.findById("p1")).thenReturn(Optional.of(produitTest));
        when(packRepository.save(any(Pack.class))).thenReturn(packTest);

        // When
        Pack result = packUnifiedService.associatePackToProduit("1", "p1");

        // Then
        assertNotNull(result);
        verify(packRepository, times(1)).save(any(Pack.class));
    }

    @Test
    void testDissociatePackFromProduit_Success() {
        // Given
        packTest.setProduitId("p1");
        when(packRepository.findById("1")).thenReturn(Optional.of(packTest));
        when(packRepository.save(any(Pack.class))).thenReturn(packTest);

        // When
        Pack result = packUnifiedService.dissociatePackFromProduit("1");

        // Then
        assertNotNull(result);
        assertNull(result.getProduitId());
        verify(packRepository, times(1)).save(any(Pack.class));
    }

    @Test
    void testDesactiverPack_Success() {
        // Given
        when(packRepository.findById("1")).thenReturn(Optional.of(packTest));
        when(packRepository.save(any(Pack.class))).thenReturn(packTest);

        // When
        Pack result = packUnifiedService.desactiverPack("1");

        // Then
        assertNotNull(result);
        assertEquals(Statut.INACTIF, result.getStatut());
        verify(packRepository, times(1)).save(any(Pack.class));
    }

    @Test
    void testCalculerPrixTotalPack() {
        // Given
        when(packRepository.findById("1")).thenReturn(Optional.of(packTest));
        when(packGarantieRepository.findByPackId("1")).thenReturn(Arrays.asList(packGarantieTest));
        
        packGarantieTest.setSupplementPrix(25.0);

        // When
        Double result = packUnifiedService.calculerPrixTotalPack("1");

        // Then
        assertNotNull(result);
        assertEquals(175.0, result); // 150.0 (pack) + 25.0 (supplément garantie)
    }
}
