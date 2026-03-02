package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.config.TestExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.backend.sqlserver1.repository.RpmRepository;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(controllers = RpmController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandler.class})
public class RpmControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RpmRepository rpmRepository;
    
    @Test
    void shouldReturnEmptyListWhenNoMnucods() throws Exception {
        when(rpmRepository.findMNUCODsByPERCOD("USER1")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/mnucods").param("PERCOD", "USER1")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(rpmRepository).findMNUCODsByPERCOD("USER1");
    }

    @Test
    void shouldReturn500WhenRepositoryThrows() throws Exception {
        when(rpmRepository.findMNUCODsByPERCOD(anyString())).thenThrow(new RuntimeException("DB down"));

        mockMvc.perform(get("/api/mnucods").param("PERCOD", "X")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error", containsString("DB down")));
    }

    @Test
    void shouldReturnMenuDtos_whenRepositoryReturnsMenuCodes() throws Exception {
        when(rpmRepository.findMNUCODsByPERCOD("USER1")).thenReturn(List.of("MENU1", "MENU2"));

        mockMvc.perform(get("/api/mnucods")
                .param("PERCOD", "USER1")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].mnucod").value("MENU1"))
            .andExpect(jsonPath("$[1].mnucod").value("MENU2"));

        verify(rpmRepository).findMNUCODsByPERCOD("USER1");
    }
}
