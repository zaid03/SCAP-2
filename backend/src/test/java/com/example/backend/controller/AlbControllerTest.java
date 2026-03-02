package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.config.TestExceptionHandler;
import com.example.backend.sqlserver2.model.Alb;
import com.example.backend.sqlserver2.repository.AlbRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AlbController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandler.class})
public class AlbControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlbRepository albRepository;

    @Test
    void getAlbaranesByFactura_returns200WithList() throws Exception {
        Alb a = new Alb();
        a.setALBNUM(1);
        a.setALBREF("REF123");
        when(albRepository.findByENTAndEJEAndFACNUM(1, "E1", 100)).thenReturn(List.of(a));

        mockMvc.perform(get("/api/alb/albaranes/1/E1/100")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getAlbaranesByFactura_returns404WhenEmpty() throws Exception {
        when(albRepository.findByENTAndEJEAndFACNUM(2, "E2", 200)).thenReturn(List.of());

        mockMvc.perform(get("/api/alb/albaranes/2/E2/200"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void getAlbaranesByFactura_returns400OnDataAccessException() throws Exception {
        when(albRepository.findByENTAndEJEAndFACNUM(anyInt(), anyString(), anyInt()))
            .thenThrow(new DataAccessResourceFailureException("DB down"));

        mockMvc.perform(get("/api/alb/albaranes/1/E1/100"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Error:")));
    }
}