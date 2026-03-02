package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.dto.Operaciones;
import com.example.backend.service.OperacionesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OperacionesController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class OperacionesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OperacionesService operacionesService;

    @Test
    void shouldReturnOperacionesWithParams() throws Exception {
        Operaciones o = new Operaciones();
        when(operacionesService.getOperaciones("1", "2", "cod", "org", "fun", "eco", "exp", "grp", "ofi"))
            .thenReturn(List.of(o));

        mockMvc.perform(get("/api/sical/operaciones")
                .param("numeroOperDesde", "1")
                .param("numeroOperHasta", "2")
                .param("codigoOperacion", "cod")
                .param("clorg", "org")
                .param("clfun", "fun")
                .param("cleco", "eco")
                .param("expediente", "exp")
                .param("grupoApunte", "grp")
                .param("oficina", "ofi")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        verify(operacionesService).getOperaciones("1", "2", "cod", "org", "fun", "eco", "exp", "grp", "ofi");
    }

    @Test
    void shouldReturnEmptyListWhenNoResults() throws Exception {
        when(operacionesService.getOperaciones(null, null, null, null, null, null, null, null, null))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/sical/operaciones")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(operacionesService).getOperaciones(null, null, null, null, null, null, null, null, null);
    }

    @Test
    void shouldReturn500WhenServiceThrows() throws Exception {
        when(operacionesService.getOperaciones(any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(new RuntimeException("sical fail"));

        mockMvc.perform(get("/api/sical/operaciones")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}