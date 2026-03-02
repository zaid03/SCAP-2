package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.config.TestExceptionHandler;
import com.example.backend.dto.GbsWithCgeDto;
import com.example.backend.sqlserver2.model.Cge;
import com.example.backend.sqlserver2.model.CgeId;
import com.example.backend.sqlserver2.model.Gbs;
import com.example.backend.sqlserver2.model.GbsId;
import com.example.backend.sqlserver2.repository.CgeRepository;
import com.example.backend.sqlserver2.repository.GbsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GbsController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandler.class})
public class GbsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GbsRepository gbsRepository;

    @MockitoBean
    private CgeRepository cgeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getBolsas_returns200WithList() throws Exception {
        Gbs g = new Gbs();
        g.setGBSREF("REF1");
        g.setGBSIMP(123.45);
        g.setGBSFOP(LocalDateTime.of(2026, 1, 22, 12, 0));
        when(gbsRepository.findByENTAndEJEAndCGECOD(1, "E1", "C1")).thenReturn(List.of(g));

        Cge cge = new Cge();
        cge.setCGECOD("C1");
        cge.setCGEDES("Desc Cge");
        when(cgeRepository.findById(new CgeId(1, "E1", "C1"))).thenReturn(Optional.of(cge));

        mockMvc.perform(get("/api/gbs/fetch-all/1/E1/C1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].cgecod").value("C1"))
            .andExpect(jsonPath("$[0].cgedes").value("Desc Cge"))
            .andExpect(jsonPath("$[0].gbsref").value("REF1"))
            .andExpect(jsonPath("$[0].gbsimp").value(123.45));
    }

    @Test
    void getBolsas_returns404WhenCgeMissing() throws Exception {
        when(gbsRepository.findByENTAndEJEAndCGECOD(2, "E2", "C2")).thenReturn(List.of());
        when(cgeRepository.findById(new CgeId(2, "E2", "C2"))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/gbs/fetch-all/2/E2/C2")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void getBolsas_returnsBadRequestOnDataAccessException() throws Exception {
        when(gbsRepository.findByENTAndEJEAndCGECOD(anyInt(), anyString(), anyString()))
            .thenThrow(new DataAccessResourceFailureException("DB down"));

        when(cgeRepository.findById(any())).thenReturn(Optional.of(new Cge()));

        mockMvc.perform(get("/api/gbs/fetch-all/1/E1/C1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error:")));
    }

    @Test
    void updateBolsa_returnsNoContentOnSuccess() throws Exception {
        GbsId id = new GbsId(1, "E1", "C1", "REF1");
        Gbs existing = new Gbs();
        existing.setGBSREF("REF1");
        when(gbsRepository.findById(id)).thenReturn(Optional.of(existing));

        Map<String, Object> payload = Map.of(
            "GBSIMP", 200.5,
            "GBSIUS", 5.0,
            "GBSICO", 10,
            "GBSFOP", "2026-01-22T12:00:00"
        );

        mockMvc.perform(patch("/api/gbs/1/E1/C1/REF1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isNoContent());

        verify(gbsRepository).save(Mockito.<Gbs>any());
    }

    @Test
    void updateBolsa_returnsNotFoundWhenMissing() throws Exception {
        GbsId id = new GbsId(9, "X", "Y", "Z");
        when(gbsRepository.findById(id)).thenReturn(Optional.empty());

        Map<String, Object> payload = Map.of(
            "GBSIMP", 1.0,
            "GBSIUS", 1.0,
            "GBSICO", 1.0,
            "GBSFOP", "2026-01-22T12:00:00"
        );

        mockMvc.perform(patch("/api/gbs/9/X/Y/Z")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void updateBolsa_returnsBadRequestOnMissingFields() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("GBSIMP", 1.0);
        payload.put("GBSIUS", null);
        payload.put("GBSICO", 1.0);
        payload.put("GBSFOP", "2026-01-22T12:00:00");

        mockMvc.perform(patch("/api/gbs/1/E1/C1/REF1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Faltan datos obligatorios")));
    }

    @Test
    void updateBolsa_returnsBadRequestOnDataAccessException() throws Exception {
        GbsId id = new GbsId(1, "E1", "C1", "REF1");
        when(gbsRepository.findById(id)).thenThrow(new DataAccessResourceFailureException("DB down"));

        Map<String, Object> payload = Map.of(
            "GBSIMP", 1.0,
            "GBSIUS", 1.0,
            "GBSICO", 1.0,
            "GBSFOP", "2026-01-22T12:00:00"
        );

        mockMvc.perform(patch("/api/gbs/1/E1/C1/REF1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error:")));
    }
}