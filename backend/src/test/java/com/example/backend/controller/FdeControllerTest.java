package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.config.TestExceptionHandler;
import com.example.backend.sqlserver2.model.Fde;
import com.example.backend.sqlserver2.repository.FdeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FdeController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandler.class})
public class FdeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FdeRepository fdeRepository;

    @Test
    void getFde_returnsListWhenFound() throws Exception {
        Fde f = new Fde();
        f.setFDEREF("REF1");
        f.setFDEECO("ECO1");
        f.setFDEIMP(55.5);
        f.setFDEDIF(5.0);
        when(fdeRepository.findByENTAndEJEAndFACNUM(1, "E1", 123)).thenReturn(List.of(f));

        mockMvc.perform(get("/api/fde/1/E1/123")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(content().string(containsString("REF1")))
            .andExpect(content().string(containsString("ECO1")))
            .andExpect(content().string(containsString("55.5")));
    }

    @Test
    void getFde_returns404WhenEmpty() throws Exception {
        when(fdeRepository.findByENTAndEJEAndFACNUM(2, "E2", 1)).thenReturn(List.of());

        mockMvc.perform(get("/api/fde/2/E2/1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void getFde_returns400OnDataAccessException() throws Exception {
        when(fdeRepository.findByENTAndEJEAndFACNUM(anyInt(), anyString(), anyInt()))
            .thenThrow(new DataAccessResourceFailureException("DB down"));

        mockMvc.perform(get("/api/fde/1/E1/1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error:")));
    }
}