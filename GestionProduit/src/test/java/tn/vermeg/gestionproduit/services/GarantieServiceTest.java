package tn.vermeg.gestionproduit.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.entities.Statut;
import tn.vermeg.gestionproduit.repositories.GarantieRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GarantieServiceTest {

    @Mock
    private GarantieRepository garantieRepository;

    @InjectMocks
    private GarantieService garantieService;

    private Garantie garantieTest;

    @BeforeEach
    void setUp() {
        garantieTest = new Garantie();
        garantieTest.setIdGarantie("1");
        garantieTest.setNomGarantie("Hospitalisation");
        garantieTest.setDescription("Couverture frais d'hospitalisation");
        garantieTest.setStatut(Statut.ACTIF);
    }

    @Test
    void testGetAllGaranties() {
        // Given
        List<Garantie> garanties = Arrays.asList(garantieTest);
        when(garantieRepository.findAll()).thenReturn(garanties);

        // When
        List<Garantie> result = garantieService.getAllGaranties();

        // Then
        assertEquals(1, result.size());
        assertEquals("Hospitalisation", result.get(0).getNomGarantie());
        verify(garantieRepository, times(1)).findAll();
    }

    @Test
    void testGetGarantieById_Success() {
        // Given
        when(garantieRepository.findById("1")).thenReturn(Optional.of(garantieTest));

        // When
        Garantie result = garantieService.getGarantieById("1");

        // Then
        assertNotNull(result);
        assertEquals("Hospitalisation", result.getNomGarantie());
        verify(garantieRepository, times(1)).findById("1");
    }

    @Test
    void testGetGarantieById_NotFound() {
        // Given
        when(garantieRepository.findById("999")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            garantieService.getGarantieById("999");
        });
        verify(garantieRepository, times(1)).findById("999");
    }

    @Test
    void testCreateGarantie_Success() {
        // Given
        when(garantieRepository.save(any(Garantie.class))).thenReturn(garantieTest);

        // When
        Garantie result = garantieService.createGarantie(garantieTest);

        // Then
        assertNotNull(result);
        assertEquals(Statut.ACTIF, result.getStatut());
        verify(garantieRepository, times(1)).save(any(Garantie.class));
    }

    @Test
    void testUpdateGarantie_Success() {
        // Given
        Garantie updatedDetails = new Garantie();
        updatedDetails.setNomGarantie("Hospitalisation Premium");
        updatedDetails.setDescription("Description mise à jour");
        updatedDetails.setStatut(Statut.ACTIF);

        when(garantieRepository.findById("1")).thenReturn(Optional.of(garantieTest));
        when(garantieRepository.save(any(Garantie.class))).thenReturn(garantieTest);

        // When
        Garantie result = garantieService.updateGarantie("1", updatedDetails);

        // Then
        assertNotNull(result);
        verify(garantieRepository, times(1)).save(any(Garantie.class));
    }

    @Test
    void testDeleteGarantie_Success() {
        // Given
        when(garantieRepository.existsById("1")).thenReturn(true);

        // When
        assertDoesNotThrow(() -> garantieService.deleteGarantie("1"));

        // Then
        verify(garantieRepository, times(1)).deleteById("1");
    }

    @Test
    void testGetGarantiesByStatut() {
        // Given
        List<Garantie> garanties = Arrays.asList(garantieTest);
        when(garantieRepository.findByStatut(Statut.ACTIF)).thenReturn(garanties);

        // When
        List<Garantie> result = garantieService.getGarantiesByStatut(Statut.ACTIF);

        // Then
        assertEquals(1, result.size());
        assertEquals(Statut.ACTIF, result.get(0).getStatut());
        verify(garantieRepository, times(1)).findByStatut(Statut.ACTIF);
    }

    @Test
    void testSearchGaranties() {
        // Given
        List<Garantie> garanties = Arrays.asList(garantieTest);
        when(garantieRepository.findByNomGarantieContainingIgnoreCase("Hospitalisation")).thenReturn(garanties);

        // When
        List<Garantie> result = garantieService.searchGaranties("Hospitalisation");

        // Then
        assertEquals(1, result.size());
        assertTrue(result.get(0).getNomGarantie().contains("Hospitalisation"));
        verify(garantieRepository, times(1)).findByNomGarantieContainingIgnoreCase("Hospitalisation");
    }
}
