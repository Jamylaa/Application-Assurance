package tn.vermeg.gestionproduit.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.vermeg.gestionproduit.dto.GarantieDTO;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.entities.Statut;
import tn.vermeg.gestionproduit.exceptions.ApiException;
import tn.vermeg.gestionproduit.services.GarantieService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GarantieController.class)
class GarantieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GarantieService garantieService;

    @Autowired
    private ObjectMapper objectMapper;

    private Garantie garantieTest;
    private GarantieDTO garantieDTOTest;

    @BeforeEach
    void setUp() {
        garantieTest = new Garantie();
        garantieTest.setIdGarantie("1");
        garantieTest.setNomGarantie("Hospitalisation");
        garantieTest.setDescription("Couverture frais d'hospitalisation");
        garantieTest.setStatut(Statut.ACTIF);

        garantieDTOTest = new GarantieDTO();
        garantieDTOTest.setIdGarantie("1");
        garantieDTOTest.setNomGarantie("Hospitalisation");
        garantieDTOTest.setDescription("Couverture frais d'hospitalisation");
        garantieDTOTest.setStatut(Statut.ACTIF);
    }

    @Test
    void testGetAllGaranties_Success() throws Exception {
        // Given
        List<Garantie> garanties = Arrays.asList(garantieTest);
        when(garantieService.getAllGaranties()).thenReturn(garanties);

        // When & Then
        mockMvc.perform(get("/api/garanties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomGarantie").value("Hospitalisation"));

        verify(garantieService, times(1)).getAllGaranties();
    }

    @Test
    void testGetGarantieById_Success() throws Exception {
        // Given
        when(garantieService.getGarantieById("1")).thenReturn(garantieTest);

        // When & Then
        mockMvc.perform(get("/api/garanties/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomGarantie").value("Hospitalisation"));

        verify(garantieService, times(1)).getGarantieById("1");
    }

    @Test
    void testCreateGarantie_Success() throws Exception {
        // Given
        when(garantieService.createGarantie(any(Garantie.class))).thenReturn(garantieTest);

        // When & Then
        mockMvc.perform(post("/api/garanties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(garantieDTOTest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomGarantie").value("Hospitalisation"));

        verify(garantieService, times(1)).createGarantie(any(Garantie.class));
    }

    @Test
    void testUpdateGarantie_Success() throws Exception {
        // Given
        when(garantieService.updateGarantie(eq("1"), any(Garantie.class))).thenReturn(garantieTest);

        // When & Then
        mockMvc.perform(put("/api/garanties/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(garantieDTOTest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomGarantie").value("Hospitalisation"));

        verify(garantieService, times(1)).updateGarantie(eq("1"), any(Garantie.class));
    }

    @Test
    void testDeleteGarantie_Success() throws Exception {
        // Given
        doNothing().when(garantieService).deleteGarantie("1");

        // When & Then
        mockMvc.perform(delete("/api/garanties/1"))
                .andExpect(status().isNoContent());

        verify(garantieService, times(1)).deleteGarantie("1");
    }
}
