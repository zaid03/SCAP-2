package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.config.TestExceptionHandler;
import com.example.backend.sqlserver2.model.Asu;
import com.example.backend.sqlserver2.model.AsuId;
import com.example.backend.sqlserver2.repository.AsuRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AsuController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandler.class})
public class AsuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AsuRepository asuRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getByEntAndAfacodOrAsucod_combinesResults() throws Exception {
        Asu a1 = new Asu(); a1.setASUCOD("A1"); a1.setASUDES("Desc1");
        Asu a2 = new Asu(); a2.setASUCOD("B1"); a2.setASUDES("Desc2");
        when(asuRepository.findByENTAndAFACOD(1, "A1")).thenReturn(List.of(a1));
        when(asuRepository.findByENTAndASUCOD(1, "B1")).thenReturn(List.of(a2));

        mockMvc.perform(get("/api/asu/by-ent/1/A1/B1").accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getByEntAndAfacodOrAsucod_returns400OnDataAccessException() throws Exception {
        when(asuRepository.findByENTAndAFACOD(anyInt(), anyString()))
            .thenThrow(new DataAccessResourceFailureException("DB down"));

        mockMvc.perform(get("/api/asu/by-ent/1/A1/B1"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error:")));
    }

    @Test
    void getByEntAndAsudesLike_returnsListOr404() throws Exception {
        Asu a = new Asu(); a.setASUCOD("X"); a.setASUDES("foo");
        when(asuRepository.findByENTAndASUDESContaining(1, "foo")).thenReturn(List.of(a));

        mockMvc.perform(get("/api/asu/by-ent-like/1/foo").accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        when(asuRepository.findByENTAndASUDESContaining(2, "nop")).thenReturn(List.of());
        mockMvc.perform(get("/api/asu/by-ent-like/2/nop"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void getArtName_returnsListOr404() throws Exception {
        Asu a = new Asu(); a.setASUCOD("C1"); a.setASUDES("desc");
        when(asuRepository.findByENTAndAFACODAndASUCOD(1, "AF", "C1")).thenReturn(List.of(a));

        mockMvc.perform(get("/api/asu/art-name/1/AF/C1").accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        when(asuRepository.findByENTAndAFACODAndASUCOD(2, "AF", "XX")).thenReturn(List.of());
        mockMvc.perform(get("/api/asu/art-name/2/AF/XX"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void getSubfamilias_returnsListOr404() throws Exception {
        Asu a = new Asu(); a.setASUCOD("Z"); a.setASUDES("d");
        when(asuRepository.findByENTAndAFACOD(1, "AF")).thenReturn(List.of(a));

        mockMvc.perform(get("/api/asu/by-ent-afacod/1/AF").accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        when(asuRepository.findByENTAndAFACOD(2, "XX")).thenReturn(List.of());
        mockMvc.perform(get("/api/asu/by-ent-afacod/2/XX"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void updateSubFamilia_successAndValidation() throws Exception {
        Asu existing = new Asu();
        AsuId id = new AsuId(1, "AF", "S1");
        existing.setASUDES("old");
        when(asuRepository.findById(id)).thenReturn(Optional.of(existing));

        Map<String,Object> payload = Map.of("ASUDES", "new", "ASUECO", "E", "MTACOD", 5);

        mockMvc.perform(patch("/api/asu/update-subfamilia/1/AF/S1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isNoContent());

        ArgumentCaptor<Asu> cap = ArgumentCaptor.forClass(Asu.class);
        verify(asuRepository).save(cap.capture());
        assertEquals("new", cap.getValue().getASUDES());

        // missing fields -> bad request
        Map<String,Object> bad = Map.of("ASUDES", "x");
        mockMvc.perform(patch("/api/asu/update-subfamilia/1/AF/S1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Faltan datos obligatorios")));
    }

    @Test
    void updateSubFamilia_notFoundAndDbError() throws Exception {
        AsuId id = new AsuId(2, "AF", "NX");
        when(asuRepository.findById(id)).thenReturn(Optional.empty());

        Map<String,Object> payload = Map.of("ASUDES", "n", "ASUECO", "E", "MTACOD", 1);
        mockMvc.perform(patch("/api/asu/update-subfamilia/2/AF/NX")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));

        when(asuRepository.findById(Mockito.<AsuId>any()))
            .thenThrow(new DataAccessResourceFailureException("DB down"));
        mockMvc.perform(patch("/api/asu/update-subfamilia/1/AF/S1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error:")));
    }

    @Test
    void insertSub_successValidationAndConflict() throws Exception {
        Map<String,Object> payload = Map.of("ent", 1, "afacod", "A", "asucod", "X", "asudes", "D", "asueco", "E", "mtacod", 1);
        when(asuRepository.findByENTAndAFACODAndASUCOD(1, "A", "X")).thenReturn(List.of());

        mockMvc.perform(post("/api/asu/Insert-Subfamilia")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isCreated());

        verify(asuRepository).save(Mockito.<Asu>any());

        Map<String,Object> missing = Map.of("ent", 1, "afacod", "A");
        mockMvc.perform(post("/api/asu/Insert-Subfamilia")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(missing)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Faltan datos obligatorios")));

        when(asuRepository.findByENTAndAFACODAndASUCOD(1, "A", "X")).thenReturn(List.of(new Asu()));
        mockMvc.perform(post("/api/asu/Insert-Subfamilia")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isConflict())
            .andExpect(content().string(containsString("Sin resultado")));
    }
}