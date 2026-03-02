package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.config.TestExceptionHandler;
import com.example.backend.sqlserver2.model.Fac;
import com.example.backend.sqlserver2.model.Ter;
import com.example.backend.sqlserver2.repository.FacRepository;
import com.example.backend.sqlserver2.repository.TerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FacController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandler.class})
public class FacControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FacRepository facRepository;

    @MockitoBean
    private TerRepository terRepository;

    @Test
    void getFacturas_returns200WithDto() throws Exception {
        Fac f = new Fac();
        f.setENT(1);
        f.setEJE("E1");
        f.setFACNUM(123);
       f.setTERCOD(1);
        f.setCGECOD("C1");
        f.setFACIMP(100.5);
        when(facRepository.findByENTAndEJEAndCGECODOrderByFACFREAsc(1, "E1", "C1"))
             .thenReturn(List.of(f));
 
         Ter ter = new Ter();
        ter.setTERCOD(1);
         ter.setTERNOM("Cliente X");
         ter.setTERNIF("NIFX");
        when(terRepository.findByENTAndTERCOD(1, 1)).thenReturn(Optional.of(ter));

        mockMvc.perform(get("/api/fac/1/E1/C1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].facnum").value(123))
            .andExpect(jsonPath("$[0].tercod").value(1))
            .andExpect(jsonPath("$[0].ternom").value("Cliente X"))
            .andExpect(jsonPath("$[0].facimp").value(100.5));
    }

    @Test
    void getFacturas_returnsBadRequestOnDataAccessException() throws Exception {
        when(facRepository.findByENTAndEJEAndCGECODOrderByFACFREAsc(anyInt(), anyString(), anyString()))
            .thenThrow(new DataAccessResourceFailureException("DB down"));

        mockMvc.perform(get("/api/fac/1/E1/C1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error:")));
    }

    @Test
    void searchFacturas_returns200WithResults() throws Exception {
        Fac f = new Fac();
        f.setENT(1);
        f.setEJE("E1");
        f.setFACNUM(321);
        Ter t = new Ter();
        t.setTERNOM("Cliente Y");
        f.setTer(t); // controller uses f.getTer()
        when(facRepository.findAll(any(Specification.class))).thenReturn(List.of(f));

        mockMvc.perform(get("/api/fac/search")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C1")
                .param("estado", "TODAS")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].facnum").value(321))
            .andExpect(jsonPath("$[0].ternom").value("Cliente Y"));
    }

    @Test
    void searchFacturas_returnsBadRequestOnException() throws Exception {
        when(facRepository.findAll(any(Specification.class))).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/fac/search")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error:")));
    }
}