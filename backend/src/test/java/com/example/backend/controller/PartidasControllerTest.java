package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.dto.Partida;
import com.example.backend.service.PartidasService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PartidasController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class PartidasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PartidasService partidasService;

    @Test
    void shouldReturnPartidasWithParams() throws Exception {
        Partida p = new Partida();
        when(partidasService.getPartidas("cenges1", "alias1", "org1", "fun1", "eco1", "cte1", "pam1", "user1"))
            .thenReturn(List.of(p));

        mockMvc.perform(get("/api/sical/partidas")
                .param("cenges", "cenges1")
                .param("alias", "alias1")
                .param("clorg", "org1")
                .param("clfun", "fun1")
                .param("cleco", "eco1")
                .param("clcte", "cte1")
                .param("clpam", "pam1")
                .param("usucenges", "user1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        verify(partidasService).getPartidas("cenges1", "alias1", "org1", "fun1", "eco1", "cte1", "pam1", "user1");
    }

    @Test
    void shouldReturnInternalServerErrorWithEmptyListOnException() throws Exception {
        when(partidasService.getPartidas(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(new RuntimeException("sical fail"));

        mockMvc.perform(get("/api/sical/partidas")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}