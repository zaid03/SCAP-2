package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.service.TerSearchOptions;
import com.example.backend.config.TestExceptionHandler;
import com.example.backend.sqlserver2.model.Ter;
import com.example.backend.sqlserver2.model.TerId;
import com.example.backend.sqlserver2.repository.TerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TerController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandler.class})
public class TerControllerTest {

    private static final int TEST_ENT = 999999;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TerRepository terRepository;

    private Ter createTer(Integer ent, Integer tercod, String nombre, String nif, Integer terblo) {
        Ter ter = new Ter();
        ter.setENT(ent);
        ter.setTERCOD(tercod);
        ter.setTERNOM(nombre);
        ter.setTERNIF(nif);
        ter.setTERBLO(terblo);
        ter.setTERALI("Alias " + nombre);
        ter.setTERWEB("www.example.com");
        ter.setTEROBS("Test observation");
        ter.setTERACU(0);
        return ter;
    }

    @Test
    void shouldGetAllByEnt() throws Exception {
        Ter a = createTer(TEST_ENT, 100, "A", "111", 0);
        Ter b = createTer(TEST_ENT, 101, "B", "222", 0);
        when(terRepository.findByENT(TEST_ENT)).thenReturn(List.of(a, b));

        mockMvc.perform(get("/api/ter/by-ent/" + TEST_ENT))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].ent", everyItem(is(TEST_ENT))));
    }

    @Test
    void shouldGetAllByEnt_returns404WhenEmpty() throws Exception {
        when(terRepository.findByENT(TEST_ENT)).thenReturn(List.of());

        mockMvc.perform(get("/api/ter/by-ent/" + TEST_ENT))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void shouldFilterByBloqueado() throws Exception {
        Ter bloqueado = createTer(TEST_ENT, 200, "Blocked", "555", 1);
        when(terRepository.findByENTAndTERBLO(TEST_ENT, 1)).thenReturn(List.of(bloqueado));

        mockMvc.perform(get("/api/ter/filter/" + TEST_ENT))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].tercod").value(200))
            .andExpect(jsonPath("$[0].terblo").value(1));
    }

    @Test
    void shouldFilterByNoBloqueado() throws Exception {
        Ter b = createTer(TEST_ENT, 300, "Un1", "777", 0);
        Ter c = createTer(TEST_ENT, 301, "Un2", "778", 0);
        when(terRepository.findByENTAndTERBLO(TEST_ENT, 0)).thenReturn(List.of(b, c));

        mockMvc.perform(get("/api/ter/filter-no/" + TEST_ENT))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].terblo", everyItem(is(0))));
    }

    @Test
    void shouldFilterByTercodBloqueado() throws Exception {
        Ter b1 = createTer(TEST_ENT, 400, "B1", "101", 1);
        when(terRepository.findByENTAndTERCODAndTERBLO(TEST_ENT, 400, 1)).thenReturn(List.of(b1));

        mockMvc.perform(get("/api/ter/by-tercod-bloqueado/" + TEST_ENT + "/tercod/400"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[*].terblo", everyItem(is(1))));
    }

    @Test
    void shouldFilterByTercodNoBloqueado() throws Exception {
        Ter nb = createTer(TEST_ENT, 500, "NB", "201", 0);
        when(terRepository.findByENTAndTERCODAndTERBLO(TEST_ENT, 500, 0)).thenReturn(List.of(nb));

        mockMvc.perform(get("/api/ter/by-tercod-no-bloqueado/" + TEST_ENT + "/tercod/500"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].terblo").value(0));
    }

    @Test
    void shouldFilterByTernifBloqueado() throws Exception {
        Ter b = createTer(TEST_ENT, 600, "B", "ABC123456", 1);
        when(terRepository.findByENTAndTERNIFContainingAndTERBLO(TEST_ENT, "ABC", 1)).thenReturn(List.of(b));

        mockMvc.perform(get("/api/ter/by-ternif-bloquado/" + TEST_ENT + "/ternif/ABC"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].ternif", containsString("ABC")))
            .andExpect(jsonPath("$[0].terblo").value(1));
    }

    @Test
    void shouldFilterByTernifNoBloqueado() throws Exception {
        Ter nb = createTer(TEST_ENT, 701, "NB", "MATCHME", 0);
        when(terRepository.findByENTAndTERNIFContainingAndTERBLO(TEST_ENT, "MATCH", 0)).thenReturn(List.of(nb));

        mockMvc.perform(get("/api/ter/by-ternif-no-bloqueado/" + TEST_ENT + "/ternif/MATCH"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].ternif", containsString("MATCH")))
            .andExpect(jsonPath("$[0].terblo").value(0));
    }

    @Test
    void shouldSearchByTernifNomAliBloqueado() throws Exception {
        Ter t1 = createTer(TEST_ENT, 800, "ABC Company", "111ABC", 1);
        when(terRepository.findAll(any(Specification.class))).thenReturn(List.of(t1));

        mockMvc.perform(get("/api/ter/by-ternif-nom-ali-bloquado/" + TEST_ENT + "/search")
                .param("term", "ABC")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].tercod").value(800))
            .andExpect(jsonPath("$[0].terblo").value(1));
    }

    @Test
    void shouldSearchByNifNomAliNoBloqueado() throws Exception {
        Ter good = createTer(TEST_ENT, 901, "ABC Corp", "222ABC", 0);
        good.setTERALI("ABC Alias");
        when(terRepository.findAll(any(Specification.class))).thenReturn(List.of(good));

        mockMvc.perform(get("/api/ter/by-nif-nom-ali-no-bloquado/" + TEST_ENT + "/search-by-term")
                .param("term", "ABC"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].tercod").value(901))
            .andExpect(jsonPath("$[0].terblo").value(0));
    }

    @Test
    void shouldSearchByNomOrAliBloqueado() throws Exception {
        Ter t1 = createTer(TEST_ENT, 1000, "Tech Solutions", "11", 1);
        t1.setTERALI("TechSol");
        when(terRepository.findAll(any(Specification.class))).thenReturn(List.of(t1));

        mockMvc.perform(get("/api/ter/by-nom-ali-bloquado/" + TEST_ENT + "/searchByNomOrAli")
                .param("term", "Tech")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].tercod").value(1000));
    }

    @Test
    void shouldFindMatchingNomOrAliNoBloqueado() throws Exception {
        Ter t2 = createTer(TEST_ENT, 1101, "Tech Corp", "22", 0);
        t2.setTERALI("TechCorp");
        when(terRepository.findAll(any(Specification.class))).thenReturn(List.of(t2));

        mockMvc.perform(get("/api/ter/by-nom-ali-no-bloquado/" + TEST_ENT + "/findMatchingNomOrAli")
                .param("term", "Tech"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].tercod").value(1101))
            .andExpect(jsonPath("$[0].terblo").value(0));
    }

    @Test
    void shouldGetByEntAndTercod() throws Exception {
        Ter t = createTer(TEST_ENT, 1200, "Provider By Tercod", "NIF", 0);
        when(terRepository.findAllByENTAndTERCOD(TEST_ENT, 1200)).thenReturn(List.of(t));

        mockMvc.perform(get("/api/ter/by-ent/" + TEST_ENT + "/tercod/1200"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].tercod").value(1200))
            .andExpect(jsonPath("$[0].ternom").value("Provider By Tercod"));
    }

    @Test
    void shouldGetByEntAndTernif() throws Exception {
        Ter t1 = createTer(TEST_ENT, 1300, "P1", "LOOKME", 0);
        when(terRepository.findByENTAndTERNIFContaining(TEST_ENT, "LOOK")).thenReturn(List.of(t1));

        mockMvc.perform(get("/api/ter/by-ent/" + TEST_ENT + "/ternif/LOOK"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].ternif", containsString("LOOK")));
    }

    @Test
    void shouldSearchTodos() throws Exception {
        Ter a = createTer(TEST_ENT, 1400, "ABC Company", "111", 1);
        a.setTERALI("ABC Alias");
        Ter b = createTer(TEST_ENT, 1401, "ABC Corp", "222", 0);
        b.setTERALI("ABC Corp Alias");
        when(terRepository.findAll(any(Specification.class))).thenReturn(List.of(a, b));

        mockMvc.perform(get("/api/ter/by-ent/" + TEST_ENT + "/search-todos")
                .param("term", "ABC"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].tercod", containsInAnyOrder(1400, 1401)));
    }

    @Test
    void shouldSaveProveedores() throws Exception {
        when(terRepository.findNextTercodForEnt(TEST_ENT)).thenReturn(1);
        when(terRepository.save(any(Ter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String json = String.format(
            "[{\"TERNOM\":\"New Provider A\",\"TERNIF\":\"11111111A\",\"TERBLO\":0,\"TERACU\":0},"
            + "{\"TERNOM\":\"New Provider B\",\"TERNIF\":\"22222222B\",\"TERBLO\":0,\"TERACU\":0}]"
        );

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].tercod").value(1))
            .andExpect(jsonPath("$[1].tercod").value(2));

        verify(terRepository, times(2)).save(any(Ter.class));
    }

    @Test
    void shouldReturn400OnSaveError() throws Exception {
        String json = "[{}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenProveedorNotFound() throws Exception {
        when(terRepository.findById(new TerId(TEST_ENT, 99999))).thenReturn(Optional.empty());

        String json = "{ \"TERWEB\": \"web.com\", \"TEROBS\": \"obs\", \"TERBLO\": 1, \"TERACU\": 1 }";

        mockMvc.perform(put("/api/ter/updateFields/" + TEST_ENT + "/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void shouldUpdateFieldsSuccessfully() throws Exception {
        Ter existing = createTer(TEST_ENT, 1500, "Existing", "NIF1", 0);
        when(terRepository.findById(new TerId(TEST_ENT, 1500))).thenReturn(Optional.of(existing));
        when(terRepository.save(any(Ter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String json = "{ \"TERWEB\": \"newweb.com\", \"TEROBS\": \"newobs\", \"TERBLO\": 1, \"TERACU\": 1 }";

        mockMvc.perform(put("/api/ter/updateFields/" + TEST_ENT + "/1500")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isNoContent());

        verify(terRepository).save(any(Ter.class));
    }

    @Test
    void shouldReturn400OnDataAccessException_getByEnt() throws Exception {
        when(terRepository.findByENT(anyInt()))
            .thenThrow(new DataAccessResourceFailureException("DB down"));

        mockMvc.perform(get("/api/ter/by-ent/" + TEST_ENT))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error")));
    }
}