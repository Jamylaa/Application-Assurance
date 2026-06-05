package tn.vermeg.gestionproduit.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.entities.Statut;
import tn.vermeg.gestionproduit.entities.TypeProduit;
import tn.vermeg.gestionproduit.repositories.ProduitRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProduitServiceTest {

    @Mock
    private ProduitRepository produitRepository;

    @InjectMocks
    private ProduitService produitService;

    private Produit produitTest;

    @BeforeEach
    void setUp() {
        produitTest = new Produit();
        produitTest.setIdProduit("1");
        produitTest.setNomProduit("Assurance Santé Familiale");
        produitTest.setDescription("Assurance santé complète pour la famille");
        produitTest.setTypeProduit(TypeProduit.SANTE);
        produitTest.setStatut(Statut.ACTIF);
    }

    @Test
    void testGetAllProduits() {
        // Given
        List<Produit> produits = Arrays.asList(produitTest);
        when(produitRepository.findAll()).thenReturn(produits);

        // When
        List<Produit> result = produitService.getAllProduits();

        // Then
        assertEquals(1, result.size());
        assertEquals("Assurance Santé Familiale", result.get(0).getNomProduit());
        verify(produitRepository, times(1)).findAll();
    }

    @Test
    void testGetProduitById_Success() {
        // Given
        when(produitRepository.findById("1")).thenReturn(Optional.of(produitTest));

        // When
        Produit result = produitService.getProduitById("1");

        // Then
        assertNotNull(result);
        assertEquals("Assurance Santé Familiale", result.getNomProduit());
        verify(produitRepository, times(1)).findById("1");
    }

    @Test
    void testGetProduitById_NotFound() {
        // Given
        when(produitRepository.findById("999")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            produitService.getProduitById("999");
        });
        verify(produitRepository, times(1)).findById("999");
    }

    @Test
    void testCreateProduit_Success() {
        // Given
        when(produitRepository.save(any(Produit.class))).thenReturn(produitTest);
        when(produitRepository.existsByNomProduitIgnoreCase("Assurance Santé Familiale")).thenReturn(false);

        // When
        Produit result = produitService.createProduit(produitTest);

        // Then
        assertNotNull(result);
        assertEquals(Statut.ACTIF, result.getStatut());
        verify(produitRepository, times(1)).save(any(Produit.class));
    }

    @Test
    void testCreateProduit_NameAlreadyExists() {
        // Given
        when(produitRepository.existsByNomProduitIgnoreCase("Assurance Santé Familiale")).thenReturn(true);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            produitService.createProduit(produitTest);
        });
        verify(produitRepository, never()).save(any(Produit.class));
    }

    @Test
    void testUpdateProduit_Success() {
        // Given
        Produit updatedDetails = new Produit();
        updatedDetails.setNomProduit("Assurance Santé Premium");
        updatedDetails.setDescription("Description mise à jour");
        updatedDetails.setTypeProduit(TypeProduit.SANTE);
        updatedDetails.setStatut(Statut.ACTIF);

        when(produitRepository.findById("1")).thenReturn(Optional.of(produitTest));
        when(produitRepository.save(any(Produit.class))).thenReturn(produitTest);
        when(produitRepository.existsByNomProduitIgnoreCase("Assurance Santé Premium")).thenReturn(false);

        // When
        Produit result = produitService.updateProduit("1", updatedDetails);

        // Then
        assertNotNull(result);
        verify(produitRepository, times(1)).save(any(Produit.class));
    }

    @Test
    void testDeleteProduit_Success() {
        // Given
        when(produitRepository.existsById("1")).thenReturn(true);

        // When
        assertDoesNotThrow(() -> produitService.deleteProduit("1"));

        // Then
        verify(produitRepository, times(1)).deleteById("1");
    }

    @Test
    void testGetProduitsByType() {
        // Given
        List<Produit> produits = Arrays.asList(produitTest);
        when(produitRepository.findByTypeProduit(TypeProduit.SANTE)).thenReturn(produits);

        // When
        List<Produit> result = produitService.getProduitsByType(TypeProduit.SANTE);

        // Then
        assertEquals(1, result.size());
        assertEquals(TypeProduit.SANTE, result.get(0).getTypeProduit());
        verify(produitRepository, times(1)).findByTypeProduit(TypeProduit.SANTE);
    }

    @Test
    void testGetProduitsByStatut() {
        // Given
        List<Produit> produits = Arrays.asList(produitTest);
        when(produitRepository.findByStatut(Statut.ACTIF)).thenReturn(produits);

        // When
        List<Produit> result = produitService.getProduitsByStatut(Statut.ACTIF);

        // Then
        assertEquals(1, result.size());
        assertEquals(Statut.ACTIF, result.get(0).getStatut());
        verify(produitRepository, times(1)).findByStatut(Statut.ACTIF);
    }

    @Test
    void testSearchProduits() {
        // Given
        List<Produit> produits = Arrays.asList(produitTest);
        when(produitRepository.findByNomProduitContainingIgnoreCase("Santé")).thenReturn(produits);

        // When
        List<Produit> result = produitService.searchProduits("Santé");

        // Then
        assertEquals(1, result.size());
        assertTrue(result.get(0).getNomProduit().contains("Santé"));
        verify(produitRepository, times(1)).findByNomProduitContainingIgnoreCase("Santé");
    }
}