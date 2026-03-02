package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.config.TestExceptionHandler;
import com.example.backend.sqlserver2.model.Art;
import com.example.backend.sqlserver2.repository.AfaRepository;
import com.example.backend.sqlserver2.repository.ArtRepository;
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

@WebMvcTest(controllers = ArtController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandler.class})
public class ArtControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ArtRepository artRepository;
    @MockitoBean private AfaRepository afaRepository;
    @MockitoBean private AsuRepository asuRepository;

    @Test
    void getByEntAfacodAsucodArtcod_combinesResults() throws Exception {
        Art a1 = new Art(); a1.setARTCOD("X");
        Art a2 = new Art(); a2.setARTCOD("Y");
        Art a3 = new Art(); a3.setARTCOD("Z");
        when(artRepository.findByENTAndAFACOD(1, "AF")).thenReturn(List.of(a1));
        when(artRepository.findByENTAndASUCOD(1, "ASU")).thenReturn(List.of(a2));
        when(artRepository.findByENTAndARTCOD(1, "ART")).thenReturn(List.of(a3));

        mockMvc.perform(get("/api/art/by-ent/1/AF/ASU/ART").accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void getByEntAfacodAsucodArtcod_returnsBadRequestOnException() throws Exception {
        when(artRepository.findByENTAndAFACOD(anyInt(), anyString()))
            .thenThrow(new DataAccessResourceFailureException("boom"));

        mockMvc.perform(get("/api/art/by-ent/1/AF/ASU/ART"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error:")));
    }

    @Test
    void getByEntAndArtdesLike_returnsListOr404() throws Exception {
        Art a = new Art(); a.setARTCOD("A");
        when(artRepository.findByENTAndARTDESContaining(1, "term")).thenReturn(List.of(a));

        mockMvc.perform(get("/api/art/by-ent-like/1/term").accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        when(artRepository.findByENTAndARTDESContaining(2, "x")).thenReturn(List.of());
        mockMvc.perform(get("/api/art/by-ent-like/2/x"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void getArtName_returnsListOr404() throws Exception {
        Art a = new Art(); a.setARTCOD("A");
        when(artRepository.findByENTAndAFACODAndASUCODAndARTCOD(1, "AF", "ASU", "ART")).thenReturn(List.of(a));

        mockMvc.perform(get("/api/art/art-name/1/AF/ASU/ART").accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        when(artRepository.findByENTAndAFACODAndASUCODAndARTCOD(2, "AF", "ASU", "NX")).thenReturn(List.of());
        mockMvc.perform(get("/api/art/art-name/2/AF/ASU/NX"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void deleteFamilia_conflict_whenArticlesExist() throws Exception {
        when(artRepository.countByENTAndAFACOD(1, "AF")).thenReturn(5L);

        mockMvc.perform(delete("/api/art/delete-familia/1/AF"))
            .andDo(print())
            .andExpect(status().isConflict())
            .andExpect(content().string(containsString("No se puede borrar una familia")));
    }

    @Test
    void deleteFamilia_notFound_whenNoRemoved_and_noException() throws Exception {
        when(artRepository.countByENTAndAFACOD(1, "AF")).thenReturn(0L);
        when(afaRepository.deleteByENTAndAFACOD(1, "AF")).thenReturn(0);

        mockMvc.perform(delete("/api/art/delete-familia/1/AF"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void deleteFamilia_noContent_onSuccess() throws Exception {
        when(artRepository.countByENTAndAFACOD(1, "AF")).thenReturn(0L);
        when(afaRepository.deleteByENTAndAFACOD(1, "AF")).thenReturn(1);

        mockMvc.perform(delete("/api/art/delete-familia/1/AF"))
            .andDo(print())
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteSubFamilia_conflict_whenArticlesExist() throws Exception {
        when(artRepository.countByENTAndASUCOD(1, "ASU")).thenReturn(2L);

        mockMvc.perform(delete("/api/art/delete-sub-familia/1/AF/ASU"))
            .andDo(print())
            .andExpect(status().isConflict())
            .andExpect(content().string(containsString("No se puede borrar una subfamilia")));
    }

    @Test
    void deleteSubFamilia_notFound_and_noContent() throws Exception {
        when(artRepository.countByENTAndASUCOD(1, "ASU")).thenReturn(0L);
        when(asuRepository.deleteByENTAndAFACODAndASUCOD(1, "AF", "ASU")).thenReturn(0);

        mockMvc.perform(delete("/api/art/delete-sub-familia/1/AF/ASU"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));

        when(asuRepository.deleteByENTAndAFACODAndASUCOD(1, "AF", "ASU")).thenReturn(1);
        mockMvc.perform(delete("/api/art/delete-sub-familia/1/AF/ASU"))
            .andDo(print())
            .andExpect(status().isNoContent());
    }
}