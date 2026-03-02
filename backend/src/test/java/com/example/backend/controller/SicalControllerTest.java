package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.backend.service.SicalService;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(controllers = SicalController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class SicalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SicalService sicalService;

    @Test
    void getTerceros_noParams_returnsEmptyList() throws Exception {
        when(sicalService.getTerceros(null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/sical/terceros")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(0)));

        verify(sicalService).getTerceros(null, null, null);
    }

    @Test
    void getTerceros_withParams_forwardsToService_andReturnsList() throws Exception {
        @SuppressWarnings("unchecked")
        List<Object> dummy = (List<Object>)(List<?>) List.of(Map.of("nif", "111X", "nombre", "Alice", "apell", "Smith"));
        when(sicalService.getTerceros("111X", "Alice", "Smith")).thenReturn((List) dummy);

        mockMvc.perform(get("/api/sical/terceros")
                .param("nif", "111X")
                .param("nom", "Alice")
                .param("apell", "Smith")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].nif").value("111X"))
            .andExpect(jsonPath("$[0].nombre").value("Alice"))
            .andExpect(jsonPath("$[0].apell").value("Smith"));

        verify(sicalService).getTerceros("111X", "Alice", "Smith");
    }

    @Test
    void getTerceros_serviceThrows_returns500() throws Exception {
        when(sicalService.getTerceros(any(), any(), any())).thenThrow(new RuntimeException("SICAL down"));

        mockMvc.perform(get("/api/sical/terceros")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(containsString("Error:")));
    }

    @Test
    void testCrypto_returnsStatusAndPreviews() throws Exception {
        mockMvc.perform(get("/api/sical/test-crypto")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Crypto working"))
            .andExpect(jsonPath("$.tokenPreview", containsString("...")))
            .andExpect(jsonPath("$.originPreview", containsString("...")));
    }
}