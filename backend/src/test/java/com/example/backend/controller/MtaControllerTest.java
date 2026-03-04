package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.config.TestExceptionHandler;
import com.example.backend.sqlserver2.model.Mta;
import com.example.backend.sqlserver2.repository.AsuRepository;
import com.example.backend.sqlserver2.repository.MtaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(controllers = MtaController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandler.class})
public class MtaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MtaRepository mtaRepository;

    @MockitoBean
    private AsuRepository asuRepository;

    @Test
    void shouldReturnAllMtaForEnt() throws Exception {
        Mta m = new Mta();
        m.setMTACOD(10);
        when(mtaRepository.findByENT(1)).thenReturn(List.of(m));

        mockMvc.perform(get("/api/mta/all-mta/1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        verify(mtaRepository).findByENT(1);
    }

    @Test
    void shouldFilterAlmacenaje_returns404WhenEmpty() throws Exception {
        when(mtaRepository.findByENTAndMTACOD(2, 99)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/mta/mta-filter/2/99")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));

        verify(mtaRepository).findByENTAndMTACOD(2, 99);
    }

    @Test
    void shouldFilterAlmacenaje_returns200WithResults() throws Exception {
        Mta m = new Mta();
        m.setMTACOD(5);
        when(mtaRepository.findByENTAndMTACOD(3, 5)).thenReturn(List.of(m));

        mockMvc.perform(get("/api/mta/mta-filter/3/5")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        verify(mtaRepository).findByENTAndMTACOD(3, 5);
    }

    @Test
    void shouldFilterAlmacenaje_returns500OnDataAccessException() throws Exception {
        when(mtaRepository.findByENTAndMTACOD(anyInt(), anyInt()))
            .thenThrow(new DataAccessResourceFailureException("DB down"));

        mockMvc.perform(get("/api/mta/mta-filter/1/1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(containsString("Error:")));
    }

    @Test
    void shouldSearchAlmacenaje_returns404WhenEmpty() throws Exception {
        when(mtaRepository.findByENTAndMTADESContaining(1, "desc")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/mta/search-almacenaje/1/desc")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));

        verify(mtaRepository).findByENTAndMTADESContaining(1, "desc");
    }

    @Test
    void shouldSearchAlmacenaje_returns200WithResults() throws Exception {
        Mta m = new Mta();
        m.setMTADES("desc");
        when(mtaRepository.findByENTAndMTADESContaining(2, "desc")).thenReturn(List.of(m));

        mockMvc.perform(get("/api/mta/search-almacenaje/2/desc")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        verify(mtaRepository).findByENTAndMTADESContaining(2, "desc");
    }

    @Test
    void shouldUpdateAlmacenaje_returns400OnMissingPayload() throws Exception {
        mockMvc.perform(
                patch("/api/mta/update-almacenaje/1/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
            )
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Faltan datos obligatorios"));
    }

    @Test
    void shouldUpdateAlmacenaje_returns404IfNotFound() throws Exception {
        when(mtaRepository.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(
                patch("/api/mta/update-almacenaje/1/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"MTADES\":\"newdesc\"}")
            )
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void shouldUpdateAlmacenaje_returns204OnSuccess() throws Exception {
        Mta m = new Mta();
        when(mtaRepository.findById(any())).thenReturn(Optional.of(m));
        when(mtaRepository.save(any())).thenReturn(m);

        mockMvc.perform(
                patch("/api/mta/update-almacenaje/1/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"MTADES\":\"newdesc\"}")
            )
            .andDo(print())
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldAddAlmacenaje_returns400OnMissingPayload() throws Exception {
        mockMvc.perform(
                post("/api/mta/add-almacenaje")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
            )
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Faltan datos obligatorios"));
    }

    @Test
    void shouldAddAlmacenaje_returns204OnSuccess() throws Exception {
        when(mtaRepository.findDtoByENT(anyInt())).thenReturn(Collections.emptyList());
        when(mtaRepository.save(any())).thenReturn(new Mta());

        mockMvc.perform(
                post("/api/mta/add-almacenaje")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ENT\":1,\"MTADES\":\"desc\"}")
            )
            .andDo(print())
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldDeleteAlmacenaje_returns400IfAssociatedToSubfamilia() throws Exception {
        when(asuRepository.countByENTAndMTACOD(1, 1)).thenReturn(1);

        mockMvc.perform(
                delete("/api/mta/delete-almacenaje/1/1")
            )
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string("No se puede borrar porque está asociado a una subfamilia"));
    }

    @Test
    void shouldDeleteAlmacenaje_returns404IfNotFound() throws Exception {
        when(asuRepository.countByENTAndMTACOD(1, 1)).thenReturn(0);
        when(mtaRepository.existsById(any())).thenReturn(false);

        mockMvc.perform(
                delete("/api/mta/delete-almacenaje/1/1")
            )
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void shouldDeleteAlmacenaje_returns204OnSuccess() throws Exception {
        when(asuRepository.countByENTAndMTACOD(1, 1)).thenReturn(0);
        when(mtaRepository.existsById(any())).thenReturn(true);
        doNothing().when(mtaRepository).deleteById(any());

        mockMvc.perform(
                delete("/api/mta/delete-almacenaje/1/1")
            )
            .andDo(print())
            .andExpect(status().isNoContent());
    }
}