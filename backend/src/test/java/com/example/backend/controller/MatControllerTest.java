package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.sqlserver2.model.Mat;
import com.example.backend.sqlserver2.model.Mta;
import com.example.backend.sqlserver2.model.Mag;
import com.example.backend.sqlserver2.repository.MatRepository;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MatController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class MatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatRepository matRepository;

    @Test
    void shouldReturnDistinctAlmacen_whenRecordsMatch() throws Exception {
        Mta mta = new Mta();
        mta.setMTACOD(11);
        mta.setMTADES("Almacen A");

        Mag mag1 = new Mag();
        mag1.setDEPCOD("D1");
        Mat mat1 = new Mat();
        mat1.setMag(mag1);
        mat1.setMta(mta);

        Mag mag2 = new Mag();
        mag2.setDEPCOD("D1");
        Mat mat2 = new Mat();
        mat2.setMag(mag2);
        mat2.setMta(mta); // duplicate Mta should be deduped

        when(matRepository.findByENT(1)).thenReturn(List.of(mat1, mat2));

        mockMvc.perform(get("/api/mat/fetch-almacen/1/D1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].mtacod").value(11))
            .andExpect(jsonPath("$[0].mtades").value("Almacen A"));

        verify(matRepository).findByENT(1);
    }

    @Test
    void shouldReturnNotFoundWhenNoMatchingRecords() throws Exception {
        // repository returns some records but none match depcod
        Mag mag = new Mag();
        mag.setDEPCOD("X");
        Mat m = new Mat();
        m.setMag(mag);
        m.setMta(new Mta());
        when(matRepository.findByENT(2)).thenReturn(List.of(m));

        mockMvc.perform(get("/api/mat/fetch-almacen/2/D1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("No resultado"));

        verify(matRepository).findByENT(2);
    }

    @Test
    void shouldReturnBadRequestOnDataAccessException() throws Exception {
        when(matRepository.findByENT(anyInt())).thenThrow(new DataAccessResourceFailureException("DB down"));

        mockMvc.perform(get("/api/mat/fetch-almacen/1/D1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error:")));
    }
}