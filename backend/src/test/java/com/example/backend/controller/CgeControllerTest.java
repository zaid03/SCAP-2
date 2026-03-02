package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.config.TestExceptionHandler;
import com.example.backend.dto.CentroGestorLogin;
import com.example.backend.sqlserver2.model.Cge;
import com.example.backend.sqlserver2.model.Dep;
import com.example.backend.sqlserver2.model.Dpe;
import com.example.backend.sqlserver2.repository.CgeRepository;
import com.example.backend.sqlserver2.repository.DepRepository;
import com.example.backend.sqlserver2.repository.DpeRepository;
import com.example.backend.sqlserver2.repository.GbsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Map;

import org.mockito.Mockito;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CgeController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandler.class})
public class CgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DpeRepository dpeRepository;

    @MockitoBean
    private DepRepository depRepository;

    @MockitoBean
    private CgeRepository cgeRepository;

    @MockitoBean
    private GbsRepository gbsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getCentrosGestores_returnsList() throws Exception {
        Dpe d1 = new Dpe(); d1.setDEPCOD("S1");
        Dpe d2 = new Dpe(); d2.setDEPCOD("S2");
        when(dpeRepository.findByENTAndEJEAndPERCOD(1, "E1", "U1")).thenReturn(List.of(d1, d2));

        Dep dep1 = new Dep(); dep1.setCGECOD("G1"); dep1.setDEPINT(1); dep1.setDEPALM(2); dep1.setDEPCOM(3);
        Dep dep2 = new Dep(); dep2.setCGECOD("G2"); dep2.setDEPINT(0); dep2.setDEPALM(0); dep2.setDEPCOM(0);
        when(depRepository.findByENTAndEJEAndDEPCODIn(1, "E1", List.of("S1","S2"))).thenReturn(List.of(dep1, dep2));

        Cge c1 = new Cge(); c1.setCGECOD("G1"); c1.setCGEDES("Desc1");
        Cge c2 = new Cge(); c2.setCGECOD("G2"); c2.setCGEDES("Desc2");
        when(cgeRepository.findByENTAndEJEAndCGECODIn(1, "E1", List.of("G1","G2"))).thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/api/cge/1/E1/U1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].cge.cgedes", hasItem("Desc1")))
            .andExpect(jsonPath("$[*].depint", hasItem(1)));
    }

    @Test
    void getCentrosGestores_returnsNotFoundWhenNoDpes() throws Exception {
        when(dpeRepository.findByENTAndEJEAndPERCOD(1, "E1", "U1")).thenReturn(List.of());

        mockMvc.perform(get("/api/cge/1/E1/U1"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void getCentrosGestores_returnsNotFoundWhenDepsMissing() throws Exception {
        Dpe d = new Dpe(); d.setDEPCOD("S1");
        when(dpeRepository.findByENTAndEJEAndPERCOD(1, "E1", "U1")).thenReturn(List.of(d));
        when(depRepository.findByENTAndEJEAndDEPCODIn(1, "E1", List.of("S1"))).thenReturn(List.of());

        mockMvc.perform(get("/api/cge/1/E1/U1"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void getCentrosGestores_returnsNotFoundWhenCgesMissing() throws Exception {
        Dpe d = new Dpe(); d.setDEPCOD("S1");
        when(dpeRepository.findByENTAndEJEAndPERCOD(1, "E1", "U1")).thenReturn(List.of(d));
        Dep dep = new Dep(); dep.setCGECOD("G1"); dep.setDEPINT(1); dep.setDEPALM(0); dep.setDEPCOM(0);
        when(depRepository.findByENTAndEJEAndDEPCODIn(1, "E1", List.of("S1"))).thenReturn(List.of(dep));
        when(cgeRepository.findByENTAndEJEAndCGECODIn(1, "E1", List.of("G1"))).thenReturn(List.of());

        mockMvc.perform(get("/api/cge/1/E1/U1"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void getCentrosGestores_returns500OnException() throws Exception {
        when(dpeRepository.findByENTAndEJEAndPERCOD(anyInt(), anyString(), anyString()))
            .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/cge/1/E1/U1"))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(containsString("Error:")));
    }

    @Test
    void fetchAllCentroGestores_returns200WithList() throws Exception {
        Cge c = new Cge(); c.setCGECOD("G1"); c.setCGEDES("Desc");
        when(cgeRepository.findByENTAndEJE(1, "E1")).thenReturn(List.of(c));

        mockMvc.perform(get("/api/cge/fetch-all/1/E1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void fetchAllCentroGestores_returns404WhenEmpty() throws Exception {
        when(cgeRepository.findByENTAndEJE(1, "E1")).thenReturn(List.of());

        mockMvc.perform(get("/api/cge/fetch-all/1/E1"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void fetchAllCentroGestores_returns500OnDataAccessException() throws Exception {
        when(cgeRepository.findByENTAndEJE(anyInt(), anyString()))
            .thenThrow(new DataAccessResourceFailureException("DB down"));

        mockMvc.perform(get("/api/cge/fetch-all/1/E1"))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(containsString("Error:")));
    }

    @Test
    void updateCentro_returnsNoContentOnSuccess() throws Exception {
        Cge existing = new Cge();
        existing.setCGECOD("G1");
        when(cgeRepository.findById(new com.example.backend.sqlserver2.model.CgeId(1, "E1", "G1"))).thenReturn(Optional.of(existing));

        Map<String, Object> payload = Map.of(
            "cgedes", "New",
            "cgeorg", "Org",
            "cgefun", "Fun",
            "cgedat", "Dat",
            "cgecic", 5
        );

        mockMvc.perform(patch("/api/cge/update-cge/1/E1/G1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isNoContent());

        ArgumentCaptor<Cge> cap = ArgumentCaptor.forClass(Cge.class);
        verify(cgeRepository).save(cap.capture());
        assertEquals("New", cap.getValue().getCGEDES());
    }

    @Test
    void updateCentro_returnsBadRequestOnMissingFields() throws Exception {
        Map<String, Object> payload = Map.of("cgedes", "X"); // missing fields

        mockMvc.perform(patch("/api/cge/update-cge/1/E1/G1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Faltan datos obligatorios")));
    }

    @Test
    void updateCentro_returnsNotFoundWhenMissing() throws Exception {
        when(cgeRepository.findById(new com.example.backend.sqlserver2.model.CgeId(1, "E1", "NX"))).thenReturn(Optional.empty());

        Map<String, Object> payload = Map.of(
            "cgedes", "A", "cgeorg", "B", "cgefun", "C", "cgedat", "D", "cgecic", 1
        );

        mockMvc.perform(patch("/api/cge/update-cge/1/E1/NX")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void updateCentro_returnsBadRequestOnDataAccessException() throws Exception {
        when(cgeRepository.findById(new com.example.backend.sqlserver2.model.CgeId(1, "E1", "G1")))
            .thenThrow(new DataAccessResourceFailureException("DB down"));

        Map<String, Object> payload = Map.of(
            "cgedes", "A", "cgeorg", "B", "cgefun", "C", "cgedat", "D", "cgecic", 1
        );

        mockMvc.perform(patch("/api/cge/update-cge/1/E1/G1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error:")));
    }

    @Test
    void addCentroGestor_returnsCreatedOnSuccess() throws Exception {
        Map<String, Object> payload = Map.of(
            "ent", 1, "eje", "E1", "cgecod", "G1", "cgedes", "Desc",
            "cgeorg", "Org", "cgefun", "Fun", "cgedat", "Dat", "cgecic", 1
        );
        when(cgeRepository.findByENTAndEJEAndCGECOD(1, "E1", "G1")).thenReturn(List.of());

        mockMvc.perform(post("/api/cge/Insert-familia")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isCreated());

        verify(cgeRepository).save(Mockito.<Cge>any());
    }

    @Test
    void addCentroGestor_returnsConflictWhenExists() throws Exception {
        Map<String, Object> payload = Map.of(
            "ent", 1, "eje", "E1", "cgecod", "G1", "cgedes", "Desc",
            "cgeorg", "Org", "cgefun", "Fun", "cgedat", "Dat", "cgecic", 1
        );
        when(cgeRepository.findByENTAndEJEAndCGECOD(1, "E1", "G1")).thenReturn(List.of(new Cge()));

        mockMvc.perform(post("/api/cge/Insert-familia")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isConflict())
            .andExpect(content().string(containsString("Sin resultado")));
    }

    @Test
    void deleteCentroGestor_returnsConflictWhenHasBolsas() throws Exception {
        when(gbsRepository.countByENTAndEJEAndCGECOD(1, "E1", "G1")).thenReturn(2L);

        mockMvc.perform(delete("/api/cge/delete-centro-gestor/1/E1/G1"))
            .andDo(print())
            .andExpect(status().isConflict())
            .andExpect(content().string(containsString("bolsas")));
    }

    @Test
    void deleteCentroGestor_returnsConflictWhenHasServices() throws Exception {
        when(gbsRepository.countByENTAndEJEAndCGECOD(1, "E1", "G1")).thenReturn(0L);
        when(depRepository.countByENTAndEJEAndCGECOD(1, "E1", "G1")).thenReturn(1L);

        mockMvc.perform(delete("/api/cge/delete-centro-gestor/1/E1/G1"))
            .andDo(print())
            .andExpect(status().isConflict())
            .andExpect(content().string(containsString("servicios")));
    }

    @Test
    void deleteCentroGestor_returnsNotFoundWhenMissing() throws Exception {
        when(gbsRepository.countByENTAndEJEAndCGECOD(1, "E1", "G1")).thenReturn(0L);
        when(depRepository.countByENTAndEJEAndCGECOD(1, "E1", "G1")).thenReturn(0L);
        when(cgeRepository.existsById(new com.example.backend.sqlserver2.model.CgeId(1, "E1", "G1"))).thenReturn(false);

        mockMvc.perform(delete("/api/cge/delete-centro-gestor/1/E1/G1"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void deleteCentroGestor_returnsNoContentOnSuccess() throws Exception {
        when(gbsRepository.countByENTAndEJEAndCGECOD(1, "E1", "G1")).thenReturn(0L);
        when(depRepository.countByENTAndEJEAndCGECOD(1, "E1", "G1")).thenReturn(0L);
        when(cgeRepository.existsById(new com.example.backend.sqlserver2.model.CgeId(1, "E1", "G1"))).thenReturn(true);

        mockMvc.perform(delete("/api/cge/delete-centro-gestor/1/E1/G1"))
            .andDo(print())
            .andExpect(status().isNoContent());

        verify(cgeRepository).deleteById(new com.example.backend.sqlserver2.model.CgeId(1, "E1", "G1"));
    }

    @Test
    void fetchDescriptionForCge_returnsDescription() throws Exception {
        Cge c = new Cge(); c.setCGEDES("MyDesc");
        when(cgeRepository.findFirstByENTAndEJEAndCGECOD(1, "E1", "G1")).thenReturn(Optional.of(c));

        mockMvc.perform(get("/api/cge/fetch-description-services/1/E1/G1"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string("MyDesc"));
    }

    @Test
    void fetchDescriptionForCge_returnsNotFoundWhenMissing() throws Exception {
        when(cgeRepository.findFirstByENTAndEJEAndCGECOD(1, "E1", "G1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/cge/fetch-description-services/1/E1/G1"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void fetchDescriptionForCge_returnsBadRequestOnException() throws Exception {
        when(cgeRepository.findFirstByENTAndEJEAndCGECOD(anyInt(), anyString(), anyString()))
            .thenThrow(new DataAccessResourceFailureException("DB down"));

        mockMvc.perform(get("/api/cge/fetch-description-services/1/E1/G1"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error:")));
    }

    @Test
    void searchCentros_returnsResults() throws Exception {
        Cge c = new Cge(); c.setCGECOD("G1"); c.setCGEDES("D1");
        when(cgeRepository.findByENTAndEJEAndCGECODOrENTAndEJEAndCGEDESContaining(1, "E1", "term", 1, "E1", "term"))
            .thenReturn(List.of(c));

        mockMvc.perform(get("/api/cge/search-centros/1/E1/term")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].cgecod").value("G1"));
    }

    @Test
    void searchCentros_returnsNotFoundWhenEmpty() throws Exception {
        when(cgeRepository.findByENTAndEJEAndCGECODOrENTAndEJEAndCGEDESContaining(anyInt(), anyString(), anyString(), anyInt(), anyString(), anyString()))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/cge/search-centros/1/E1/term"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void searchCentros_returnsBadRequestOnDataAccessException() throws Exception {
        when(cgeRepository.findByENTAndEJEAndCGECODOrENTAndEJEAndCGEDESContaining(anyInt(), anyString(), anyString(), anyInt(), anyString(), anyString()))
            .thenThrow(new DataAccessResourceFailureException("DB down"));

        mockMvc.perform(get("/api/cge/search-centros/1/E1/term"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error:")));
    }
}