package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.config.TestExceptionHandler;
import com.example.backend.dto.DepWithCgeView;
import com.example.backend.sqlserver2.model.Dep;
import com.example.backend.sqlserver2.model.DepId;
import com.example.backend.sqlserver2.repository.CcoRepository;
import com.example.backend.sqlserver2.repository.DepRepository;
import com.example.backend.sqlserver2.repository.DpeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DepController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandler.class})
public class DepControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepRepository depRepository;

    @MockitoBean
    private DpeRepository dpeRepository;

    @MockitoBean
    private CcoRepository ccoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fetchServices_returns200WithList() throws Exception {
        Dep d = new Dep(); d.setDEPCOD("D1"); d.setDEPDES("S1");
        when(depRepository.findByENTAndEJE(1, "E1")).thenReturn(List.of(d));

        mockMvc.perform(get("/api/dep/fetch-services/1/E1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].depcod", is("D1")));
    }

    @Test
    void fetchServices_returns404WhenEmpty() throws Exception {
        when(depRepository.findByENTAndEJE(1, "E1")).thenReturn(List.of());

        mockMvc.perform(get("/api/dep/fetch-services/1/E1"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void fetchServices_returns500OnDataAccessException() throws Exception {
        when(depRepository.findByENTAndEJE(anyInt(), anyString()))
            .thenThrow(new DataAccessResourceFailureException("DB down"));

        mockMvc.perform(get("/api/dep/fetch-services/1/E1"))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(containsString("Error:")));
    }

    @Test
    void fetchServicesPersona_returnsList() throws Exception {
        DepWithCgeView v = new DepWithCgeView() {
            @Override public String getDEPCOD() { return "D1"; }
            @Override public String getDEPDES() { return "S1"; }
            @Override public String getCGECOD() { return "G1"; }
            @Override public Integer getDEPINT() { return 0; }
            @Override public Integer getDEPALM() { return 0; }
            @Override public Integer getDEPCOM() { return 0; }
        };
        when(depRepository.findByENTAndEJEAndDpes_PERCOD(1, "E1", "U1")).thenReturn(List.of(v));
 
         mockMvc.perform(get("/api/dep/fetch-services-persona/1/E1/U1")
             .accept(MediaType.APPLICATION_JSON))
             .andDo(print())
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(1)));
     }

    @Test
    void updateCentro_returns204OnSuccess() throws Exception {
        Dep existing = new Dep();
        existing.setDEPDES("old");
        when(depRepository.findById(new DepId(1, "E1", "D1"))).thenReturn(Optional.of(existing));

        Map<String,Object> payload = Map.of("depdes", "new", "depalm", 1, "depcom", 0, "depint", 0);

        mockMvc.perform(patch("/api/dep/update-service/1/E1/D1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isNoContent());

        ArgumentCaptor<Dep> cap = ArgumentCaptor.forClass(Dep.class);
        verify(depRepository).save(cap.capture());
        assertEquals("new", cap.getValue().getDEPDES());
    }

    @Test
    void updateCentro_returns400WhenMissingFields() throws Exception {
        Map<String,Object> payload = Map.of("depdes", "x", "depalm", 1); // missing depcom & depint

        mockMvc.perform(patch("/api/dep/update-service/1/E1/D1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Faltan datos obligatorios")));
    }

    @Test
    void updateCentro_returns404WhenNotFound() throws Exception {
        when(depRepository.findById(new DepId(1, "E1", "NX"))).thenReturn(Optional.empty());

        Map<String,Object> payload = Map.of("depdes", "n", "depalm", 1, "depcom", 1, "depint", 0);

        mockMvc.perform(patch("/api/dep/update-service/1/E1/NX")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void updateServiceSecond_returns204OnSuccess() throws Exception {
        Dep existing = new Dep();
        when(depRepository.findById(new DepId(1, "E1", "D2"))).thenReturn(Optional.of(existing));

        Map<String,Object> payload = Map.of(
            "depd1c", "a","depd1d","b",
            "depd2c","c","depd2d","d",
            "depd3c","e","depd3d","f",
            "depdco","g","depden","h"
        );

        mockMvc.perform(patch("/api/dep/update-service-second/1/E1/D2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isNoContent());

        verify(depRepository).save(Mockito.<Dep>any());
    }

    @Test
    void addCentroGestor_returns201OnSuccess() throws Exception {
        Map<String,Object> payload = Map.of(
            "ent", 1, "eje", "E1", "depcod", "D3", "depdes", "Desc",
            "depalm", 0, "depcom", 0, "depint", 0,
            "ccocod", "C1", "cgecod", "G1", "percod", "U1"
        );
        when(depRepository.findByENTAndEJEAndDEPCOD(1, "E1", "D3")).thenReturn(List.of());
        when(ccoRepository.countByENTAndEJEAndCCOCOD(1, "E1", "C1")).thenReturn(1L);

        mockMvc.perform(post("/api/dep/Insert-service")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isCreated());

        verify(depRepository).save(Mockito.<Dep>any());
    }

    @Test
    void addCentroGestor_returnsConflictWhenExists() throws Exception {
        Map<String,Object> payload = Map.of(
            "ent", 1, "eje", "E1", "depcod", "D3", "depdes", "Desc",
            "depalm", 0, "depcom", 0, "depint", 0,
            "ccocod", "C1", "cgecod", "G1", "percod", "U1"
        );
        when(depRepository.findByENTAndEJEAndDEPCOD(1, "E1", "D3")).thenReturn(List.of(new Dep()));

        mockMvc.perform(post("/api/dep/Insert-service")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isConflict())
            .andExpect(content().string(containsString("Este servicio ya existe")));
    }

    @Test
    void addCentroGestor_returnsConflictWhenCcoMissing() throws Exception {
        Map<String,Object> payload = Map.of(
            "ent", 1, "eje", "E1", "depcod", "D3", "depdes", "Desc",
            "depalm", 0, "depcom", 0, "depint", 0,
            "ccocod", "C1", "cgecod", "G1", "percod", "U1"
        );
        when(depRepository.findByENTAndEJEAndDEPCOD(1, "E1", "D3")).thenReturn(List.of());
        when(ccoRepository.countByENTAndEJEAndCCOCOD(1, "E1", "C1")).thenReturn(0L);

        mockMvc.perform(post("/api/dep/Insert-service")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isConflict())
            .andExpect(content().string(containsString("El Centro de Coste no existe")));
    }

    @Test
    void search_returnsResultsAndFilters() throws Exception {
        Dep d = new Dep(); d.setDEPCOD("S1"); d.setDEPDES("Servicio");
        when(depRepository.findByENTAndEJE(1, "E1")).thenReturn(List.of(d));

        mockMvc.perform(get("/api/dep/search")
                .param("ent", "1")
                .param("eje", "E1")
                .param("search", "serv")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void search_returns404WhenEmpty() throws Exception {
        when(depRepository.findByENTAndEJE(1, "E1")).thenReturn(List.of());

        mockMvc.perform(get("/api/dep/search")
                .param("ent", "1")
                .param("eje", "E1"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void search_returns500OnDataAccessException() throws Exception {
        when(depRepository.findByENTAndEJE(anyInt(), anyString()))
            .thenThrow(new DataAccessResourceFailureException("boom"));

        mockMvc.perform(get("/api/dep/search")
                .param("ent", "1")
                .param("eje", "E1"))
            .andDo(print())
            .andExpect(status().isInternalServerError());
    }
}