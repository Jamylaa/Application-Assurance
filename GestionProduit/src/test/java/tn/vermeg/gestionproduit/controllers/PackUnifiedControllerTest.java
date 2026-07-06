package tn.vermeg.gestionproduit.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.vermeg.gestionproduit.entities.*;
import tn.vermeg.gestionproduit.enums.*;
import tn.vermeg.gestionproduit.services.HierarchicalService;
import tn.vermeg.gestionproduit.services.PackUnifiedService;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(PackUnifiedController.class)
@AutoConfigureMockMvc(addFilters = false)
class PackUnifiedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PackUnifiedService packUnifiedService;

    @MockBean
    private HierarchicalService hierarchicalService;

    private Pack packTest;
    private PackGarantie packGarantieTest;

    @BeforeEach
    void setUp() {
        // Setup Pack de test
        packTest = new Pack();
        packTest.setIdPack("1");
        packTest.setCodePack("SANTE-PREMIUM-001");
        packTest.setNomPack("Pack Santé Premium");
        packTest.setDescription("Pack santé complet avec garanties étendues");
        packTest.setProduitId("p1");
        packTest.setPrixMensuel(150.0);
        packTest.setNiveauCouverture(NiveauCouverture.PREMIUM);
        packTest.setDateCreation(Instant.now());
        packTest.setDateModification(Instant.now());

        // Setup PackGarantie de test
        packGarantieTest = new PackGarantie();
        packGarantieTest.setIdPackGarantie("pg1");
        packGarantieTest.setPackId("1");
        packGarantieTest.setGarantieId("g1");
    }

    @Test
    void testGetAllPacks() throws Exception {
        // Given
        List<Pack> packs = Arrays.asList(packTest);
        when(packUnifiedService.getAllPacks()).thenReturn(packs);

        // When & Then
        mockMvc.perform(get("/api/packs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idPack").value("1"))
                .andExpect(jsonPath("$[0].nomPack").value("Pack Santé Premium"))
                .andExpect(jsonPath("$[0].prixMensuel").value(150.0));
    }

    @Test
    void testGetPackById_Success() throws Exception {
        // Given
        when(packUnifiedService.getPackById("1")).thenReturn(packTest);

        // When & Then
        mockMvc.perform(get("/api/packs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPack").value("1"))
                .andExpect(jsonPath("$.nomPack").value("Pack Santé Premium"));
    }

    @Test
    void testSearchPacks() throws Exception {
        // Given
        List<Pack> packs = Arrays.asList(packTest);
        when(packUnifiedService.searchPacksByNom("Santé")).thenReturn(packs);

        // When & Then
        mockMvc.perform(get("/api/packs/search")
                        .param("nomPack", "Santé"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomPack").value("Pack Santé Premium"));
    }

    @Test
    void testGetPacksByNiveau() throws Exception {
        // Given
        List<Pack> packs = Arrays.asList(packTest);
        when(packUnifiedService.getPacksByNiveau(NiveauCouverture.PREMIUM)).thenReturn(packs);

        // When & Then
        mockMvc.perform(get("/api/packs/niveau/PREMIUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].niveauCouverture").value("PREMIUM"));
    }

    @Test
    void testGetPacksByPrixRange() throws Exception {
        // Given
        List<Pack> packs = Arrays.asList(packTest);
        when(packUnifiedService.getPacksByPrixRange(100.0, 200.0)).thenReturn(packs);

        // When & Then
        mockMvc.perform(get("/api/packs/prix-range")
                        .param("prixMin", "100.0")
                        .param("prixMax", "200.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].prixMensuel").value(150.0));
    }

    @Test
    void testGetPacksByProduitId() throws Exception {
        // Given
        List<Pack> packs = Arrays.asList(packTest);
        when(packUnifiedService.getPacksByProduitId("p1")).thenReturn(packs);

        // When & Then
        mockMvc.perform(get("/api/packs/by-produit/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idPack").value("1"));
    }

    @Test
    void testGetAllPackGaranties() throws Exception {
        // Given
        List<PackGarantie> garanties = Arrays.asList(packGarantieTest);
        when(packUnifiedService.getAllPackGaranties()).thenReturn(garanties);

        // When & Then
        mockMvc.perform(get("/api/packs/associations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idPackGarantie").value("pg1"));
    }

    @Test
    void testGetPackStatistics() throws Exception {
        // La méthode getPackStatistics est implémentée inline dans le controller
        // et n'appelle pas de service, donc pas de mocking nécessaire

        // When & Then
        mockMvc.perform(get("/api/packs/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPacks").value(0))
                .andExpect(jsonPath("$.packsActifs").value(0));
    }

    @Test
    void testHealth() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/packs/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Pack Unified Service"));
    }

    @Test
    void testCreatePack() throws Exception {
        // Given
        Pack newPack = new Pack();
        newPack.setCodePack("NOUVEAU-PACK-001");
        newPack.setNomPack("Nouveau Pack");
        newPack.setDescription("Description");
        newPack.setProduitId("p1");
        newPack.setPrixMensuel(100.0);
        newPack.setNiveauCouverture(NiveauCouverture.BASIC);

        when(packUnifiedService.createPack(any(Pack.class))).thenReturn(newPack);

        // When & Then
        mockMvc.perform(post("/api/packs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPack)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomPack").value("Nouveau Pack"));
    }

    @Test
    void testGetGarantiesByPackId() throws Exception {
        // Given
        List<PackGarantie> garanties = Arrays.asList(packGarantieTest);
        when(packUnifiedService.getGarantiesByPackId("1")).thenReturn(garanties);

        // When & Then
        mockMvc.perform(get("/api/packs/1/garanties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idPackGarantie").value("pg1"));
    }

    @Test
    void testGetGarantiesOptionnelles() throws Exception {
        // Given
        List<PackGarantie> garanties = Arrays.asList(packGarantieTest);
        when(packUnifiedService.getGarantiesOptionnellesByPackId("1")).thenReturn(garanties);

        // When & Then
        mockMvc.perform(get("/api/packs/1/garanties/optionnelles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idPackGarantie").value("pg1"));
    }

    @Test
    void testGetGarantiesObligatoires() throws Exception {
        // Given
        List<PackGarantie> garanties = Arrays.asList(packGarantieTest);
        when(packUnifiedService.getGarantiesObligatoiresByPackId("1")).thenReturn(garanties);

        // When & Then
        mockMvc.perform(get("/api/packs/1/garanties/obligatoires"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idPackGarantie").value("pg1"));
    }

    @Test
    void testGetGarantiesDisponiblesPourPack() throws Exception {
        // Given
        Garantie garantie = new Garantie();
        garantie.setIdGarantie("g1");
        garantie.setNomGarantie("Hospitalisation");
        
        List<Garantie> garanties = Arrays.asList(garantie);
        when(packUnifiedService.getGarantiesDisponiblesPourPack("1")).thenReturn(garanties);

        // When & Then
        mockMvc.perform(get("/api/packs/1/garanties-disponibles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idGarantie").value("g1"));
    }

    @Test
    void testCalculerPrixTotalPack() throws Exception {
        // Given
        when(packUnifiedService.calculerPrixTotalPack("1")).thenReturn(175.0);

        // When & Then
        mockMvc.perform(get("/api/packs/1/prix-total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(175.0));
    }

    @Test
    void testAjouterGarantieAuPack() throws Exception {
        // Given
        when(packUnifiedService.ajouterGarantieAuPack(anyString(), anyString(), any(PackGarantie.class)))
                .thenReturn(packGarantieTest);

        // When & Then
        mockMvc.perform(post("/api/packs/1/garanties/g1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(packGarantieTest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPackGarantie").value("pg1"));
    }

    @Test
    void testAssociatePackToProduit() throws Exception {
        // Given
        when(packUnifiedService.associatePackToProduit("1", "p1")).thenReturn(packTest);

        // When & Then
        mockMvc.perform(post("/api/packs/1/associate-produit/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPack").value("1"));
    }

    @Test
    void testDissociatePackFromProduit() throws Exception {
        // Given
        when(packUnifiedService.dissociatePackFromProduit("1")).thenReturn(packTest);

        // When & Then
        mockMvc.perform(delete("/api/packs/1/dissociate-produit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPack").value("1"));
    }

    @Test
    void testUpdatePack() throws Exception {
        // Given
        Pack updatedPack = new Pack();
        updatedPack.setCodePack("SANTE-PREMIUM-001");
        updatedPack.setNomPack("Pack Mis à Jour");
        updatedPack.setDescription("Description mise à jour");
        updatedPack.setProduitId("p1");
        updatedPack.setPrixMensuel(150.0);
        updatedPack.setNiveauCouverture(NiveauCouverture.PREMIUM);

        when(packUnifiedService.updatePack(eq("1"), any(Pack.class))).thenReturn(updatedPack);

        // When & Then
        mockMvc.perform(put("/api/packs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedPack)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomPack").value("Pack Mis à Jour"));
    }

    @Test
    void testDeletePack() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/packs/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testUpdatePackGarantie() throws Exception {
        // Given
        when(packUnifiedService.updatePackGarantie(eq("pg1"), any(PackGarantie.class)))
                .thenReturn(packGarantieTest);

        // When & Then
        mockMvc.perform(put("/api/packs/associations/pg1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(packGarantieTest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPackGarantie").value("pg1"));
    }

    @Test
    void testSupprimerGarantieDuPack() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/packs/associations/pg1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testToggleGarantieActivation() throws Exception {
        // Given
        packGarantieTest.setActif(true);
        when(packUnifiedService.toggleGarantieActivation("pg1", true)).thenReturn(packGarantieTest);

        // When & Then
        mockMvc.perform(patch("/api/packs/associations/pg1/activation")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(true));
    }
}
