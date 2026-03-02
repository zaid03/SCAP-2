package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.config.TestExceptionHandler;
import com.example.backend.sqlserver2.model.Afa;
import com.example.backend.sqlserver2.model.AfaId;
import com.example.backend.sqlserver2.repository.AfaRepository;
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

@WebMvcTest(controllers = AfaController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandler.class})
public class AfaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AfaRepository afaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getByEntAndAfacod_returns200WithList() throws Exception {
        Afa a = new Afa(); a.setAFACOD("AF1"); a.setAFADES("Desc");
        when(afaRepository.findByENTAndAFACOD(1, "AF1")).thenReturn(List.of(a));

        mockMvc.perform(get("/api/afa/by-ent/1/AF1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getByEntAndAfacod_returns404WhenEmpty() throws Exception {
        when(afaRepository.findByENTAndAFACOD(2, "X")).thenReturn(List.of());

        mockMvc.perform(get("/api/afa/by-ent/2/X"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void getByEntAndAfacod_returns400OnDataAccessException() throws Exception {
        when(afaRepository.findByENTAndAFACOD(anyInt(), anyString()))
            .thenThrow(new DataAccessResourceFailureException("DB down"));

        mockMvc.perform(get("/api/afa/by-ent/1/AF"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error")));
    }

    @Test
    void getByEntAndAfadesLike_returnsListOr404() throws Exception {
        Afa a = new Afa(); a.setAFACOD("AF"); a.setAFADES("Desc");
        when(afaRepository.findByENTAndAFADESContaining(1, "Desc")).thenReturn(List.of(a));

        mockMvc.perform(get("/api/afa/by-ent-like/1/Desc")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        when(afaRepository.findByENTAndAFADESContaining(2, "X")).thenReturn(List.of());
        mockMvc.perform(get("/api/afa/by-ent-like/2/X"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void getAfaByEnt_returnsListOr404() throws Exception {
        Afa a = new Afa();
        when(afaRepository.findByENT(1)).thenReturn(List.of(a));

        mockMvc.perform(get("/api/afa/by-ent/1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        when(afaRepository.findByENT(2)).thenReturn(List.of());
        mockMvc.perform(get("/api/afa/by-ent/2"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void updateFamilia_successAndValidation() throws Exception {
        Afa existing = new Afa();
        AfaId id = new AfaId(1, "AF1");
        existing.setAFADES("old");
        when(afaRepository.findById(id)).thenReturn(Optional.of(existing));

        Map<String,Object> payload = Map.of("AFADES", "new");

        mockMvc.perform(patch("/api/afa/update-familia/1/AF1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isNoContent());

        ArgumentCaptor<Afa> cap = ArgumentCaptor.forClass(Afa.class);
        verify(afaRepository).save(cap.capture());
        assertEquals("new", cap.getValue().getAFADES());

        // missing field -> bad request
        Map<String,Object> bad = Map.of();
        mockMvc.perform(patch("/api/afa/update-familia/1/AF1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Faltan datos obligatorios")));
    }

    @Test
    void updateFamilia_notFoundAndDbError() throws Exception {
        AfaId id = new AfaId(2, "NX");
        when(afaRepository.findById(id)).thenReturn(Optional.empty());

        Map<String,Object> payload = Map.of("AFADES", "n");
        mockMvc.perform(patch("/api/afa/update-familia/2/NX")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));

        when(afaRepository.findById(Mockito.<AfaId>any()))
            .thenThrow(new DataAccessResourceFailureException("DB down"));
        mockMvc.perform(patch("/api/afa/update-familia/1/AF1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Update failed")));
    }

    @Test
    void insertFamilia_successValidationAndConflict() throws Exception {
        Map<String,Object> payload = Map.of("ent", 1, "afacod", "A", "afades", "D");
        when(afaRepository.findByENTAndAFACOD(1, "A")).thenReturn(List.of());

        mockMvc.perform(post("/api/afa/Insert-familia")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isCreated());

        verify(afaRepository).save(Mockito.<Afa>any());

        Map<String,Object> missing = Map.of("ent", 1, "afacod", "A");
        mockMvc.perform(post("/api/afa/Insert-familia")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(missing)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Faltan datos obligatorios")));

        when(afaRepository.findByENTAndAFACOD(1, "A")).thenReturn(List.of(new Afa()));
        mockMvc.perform(post("/api/afa/Insert-familia")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isConflict())
            .andExpect(content().string(containsString("Sin resultado")));
    }
}