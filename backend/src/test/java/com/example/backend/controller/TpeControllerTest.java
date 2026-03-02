package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.sqlserver2.model.Tpe;
import com.example.backend.sqlserver2.model.TpeId;
import com.example.backend.sqlserver2.repository.TpeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
public class TpeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TpeRepository tpeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Tpe createTpe(int ent, int tercod, int tpecod, String name) {
        Tpe t = new Tpe();
        t.setENT(ent);
        t.setTERCOD(tercod);
        t.setTPECOD(tpecod);
        t.setTPENOM(name);
        t.setTPETEL("100200300");
        t.setTPETMO("600700800");
        t.setTPECOE("COE");
        t.setTPEOBS("OBS");
        return t;
    }

    @Test
    void shouldGetByEntAndTercod_returns200() throws Exception {
        int ent = 1;
        int tercod = 50;
        Tpe a = createTpe(ent, tercod, 1, "Alice");
        Tpe b = createTpe(ent, tercod, 2, "Bob");
        tpeRepository.save(a);
        tpeRepository.save(b);

        mockMvc.perform(get("/api/more/by-tpe/" + ent + "/" + tercod)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].tpenom").exists());
    }

    @Test
    void shouldReturn404WhenNoTpeFound() throws Exception {
        mockMvc.perform(get("/api/more/by-tpe/999999/999999")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldAddTpe_returns201_and_createEntity() throws Exception {
        int ent = 2;
        int tercod = 77;
        var payload = new Object() {
            public final String tpenom = "NewContact";
            public final String tpetel = "111222333";
            public final String tpetmo = "999000111";
            public final String tpecoe = "COE";
            public final String tpeobs = "OBS";
        };

        mockMvc.perform(post("/api/more/add/" + ent + "/" + tercod)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Persona de contacto agregada")));

        List<Tpe> saved = tpeRepository.findByENTAndTERCOD(ent, tercod);
        assertThat(saved).anyMatch(t -> "NewContact".equals(t.getTPENOM()));
    }

    @Test
    void shouldNotAddDuplicateName_returns400() throws Exception {
        int ent = 3;
        int tercod = 88;
        Tpe existing = createTpe(ent, tercod, 1, "DupName");
        tpeRepository.save(existing);

        var payload = new Object() {
            public final String tpenom = "DupName";
            public final String tpetel = "X";
            public final String tpetmo = "Y";
            public final String tpecoe = "Z";
            public final String tpeobs = "OBS";
        };

        mockMvc.perform(post("/api/more/add/" + ent + "/" + tercod)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldModifyTpe_returns204_and_updateEntity() throws Exception {
        int ent = 4;
        int tercod = 99;
        int tpecod = 5;
        Tpe t = createTpe(ent, tercod, tpecod, "ToModify");
        tpeRepository.save(t);

        var payload = new Object() {
            public final String tpenom = "ModifiedName";
            public final String tpetel = "555";
            public final String tpetmo = "666";
            public final String tpecoe = "COE2";
            public final String tpeobs = "OBS2";
        };

        mockMvc.perform(put("/api/more/modify/" + ent + "/" + tercod + "/" + tpecod)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isNoContent());

        Optional<Tpe> updated = tpeRepository.findById(new TpeId(ent, tercod, tpecod));
        assertThat(updated).isPresent();
        assertThat(updated.get().getTPENOM()).isEqualTo("ModifiedName");
    }

    @Test
    void shouldDeleteTpe_returns200_and_deleteEntity() throws Exception {
        int ent = 5;
        int tercod = 101;
        int tpecod = 10;
        Tpe t = createTpe(ent, tercod, tpecod, "ToDelete");
        tpeRepository.save(t);

        mockMvc.perform(delete("/api/more/delete/" + ent + "/" + tercod + "/" + tpecod))
            .andExpect(status().isOk());

        boolean exists = tpeRepository.existsById(new TpeId(ent, tercod, tpecod));
        assertThat(exists).isFalse();
    }
}