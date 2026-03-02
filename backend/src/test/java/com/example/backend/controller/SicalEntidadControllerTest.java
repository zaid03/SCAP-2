package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.service.SicalEntidadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(controllers = SicalEntidadController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class SicalEntidadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SicalEntidadService sicalEntidadService;

    @Test
    void shouldReturnEmptyListWhenNoEntidades() throws Exception {
        when(sicalEntidadService.getEntidades()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/sical/entidades")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturnListWhenServiceProvidesEntidades() throws Exception {
        // return a simple object via cast to avoid depending on Entidad constructor
        @SuppressWarnings("unchecked")
        List<Object> dummy = (List<Object>)(List<?>) List.of(Map.of("codigo", 1, "nombre", "Entidad A"));
        when(sicalEntidadService.getEntidades()).thenReturn((List) dummy);

        mockMvc.perform(get("/api/sical/entidades")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].codigo").value(1))
            .andExpect(jsonPath("$[0].nombre").value("Entidad A"));
    }

    @Test
    void shouldReturn500WhenServiceThrows() throws Exception {
        when(sicalEntidadService.getEntidades()).thenThrow(new RuntimeException("SICAL down"));

        mockMvc.perform(get("/api/sical/entidades")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error", containsString("SICAL service error")));
    }
}