package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.service.TerSearchOptions;
import com.example.backend.sqlserver2.model.Ter;
import com.example.backend.sqlserver2.model.TerId;
import com.example.backend.sqlserver2.repository.TerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print; 

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
public class TerControllerTest {

    private static final int TEST_ENT = 999999;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TerRepository terRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private com.example.backend.controller.TerController terController; 

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;
    private final List<TerId> createdIds = new ArrayList<>();

    @AfterEach
    void cleanupCreated() {
        transactionTemplate.execute(status -> {
            for (TerId id : createdIds) {
                terRepository.deleteById(id);
            }
            createdIds.clear();
            return null;
        });
    }

    @BeforeEach
    void ensureCleanTestEnt() {
        transactionTemplate.execute(status -> {
            List<Ter> existing = terRepository.findByENT(TEST_ENT);
            for (Ter t : existing) {
                terRepository.deleteById(new TerId(t.getENT(), t.getTERCOD()));
            }
            return null;
        });
        createdIds.clear();
    }

    private void saveCommitted(Ter... ters) {
        transactionTemplate.execute(status -> {
            for (Ter t : ters) {
                if (t.getENT() == null || t.getENT() <= 0) t.setENT(TEST_ENT);
                terRepository.save(t);
                createdIds.add(new TerId(t.getENT(), t.getTERCOD()));
            }
            return null;
        });
    }

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
        Ter c = createTer(TEST_ENT + 1, 102, "C", "333", 0);
        saveCommitted(a, b, c);

        mockMvc.perform(get("/api/ter/by-ent/" + TEST_ENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].ent", everyItem(is(TEST_ENT))));
    }

    @Test
    void shouldFilterByBloqueado() throws Exception {
        Ter bloqueado = createTer(TEST_ENT, 200, "Blocked", "555", 1);
        Ter noBloqueado = createTer(TEST_ENT, 201, "Unblocked", "556", 0);
        saveCommitted(bloqueado, noBloqueado);

        mockMvc.perform(get("/api/ter/filter/" + TEST_ENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tercod").value(200))
                .andExpect(jsonPath("$[0].terblo").value(1));
    }

    @Test
    void shouldFilterByNoBloqueado() throws Exception {
        Ter b = createTer(TEST_ENT, 300, "Un1", "777", 0);
        Ter c = createTer(TEST_ENT, 301, "Un2", "778", 0);
        Ter blk = createTer(TEST_ENT, 302, "Blk", "779", 1);
        saveCommitted(b, c, blk);

        mockMvc.perform(get("/api/ter/filter-no/" + TEST_ENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].terblo", everyItem(is(0))));
    }

    @Test
    void shouldFilterByTercodBloqueado() throws Exception {
        Ter b1 = createTer(TEST_ENT, 400, "B1", "101", 1);
        Ter ng = createTer(TEST_ENT, 401, "B2", "102", 0);
        saveCommitted(b1, ng);

        List<Ter> saved400 = terRepository.findByENT(TEST_ENT).stream()
            .filter(t -> t.getTERCOD() != null && t.getTERCOD().equals(400))
            .collect(Collectors.toList());

       assertEquals(1, saved400.size(), "Expected 1 saved row with tercod=400 for TEST_ENT");
        mockMvc.perform(get("/api/ter/by-tercod-bloqueado/" + TEST_ENT + "/tercod/400"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[*].terblo", everyItem(is(1))));
    }

    @Test
    void shouldFilterByTercodNoBloqueado() throws Exception {
        Ter nb = createTer(TEST_ENT, 500, "NB", "201", 0);
        Ter b = createTer(TEST_ENT, 501, "B", "202", 1);
        saveCommitted(nb, b);

        List<Ter> saved500 = terRepository.findByENT(TEST_ENT).stream()
            .filter(t -> t.getTERCOD() != null && t.getTERCOD().equals(500))
            .collect(Collectors.toList());
        assertEquals(1, saved500.size(), "Expected 1 saved row with tercod=500 for TEST_ENT");

        mockMvc.perform(get("/api/ter/by-tercod-no-bloqueado/" + TEST_ENT + "/tercod/500"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].terblo").value(0));
    }

    @Test
    void shouldFilterByTernifBloqueado() throws Exception {
        Ter b = createTer(TEST_ENT, 600, "B", "ABC123456", 1);
        Ter nb = createTer(TEST_ENT, 601, "NB", "XYZ999", 0);
        saveCommitted(b, nb);

        mockMvc.perform(get("/api/ter/by-ternif-bloquado/" + TEST_ENT + "/ternif/ABC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ternif", containsString("ABC")))
                .andExpect(jsonPath("$[0].terblo").value(1));
    }

    @Test
    void shouldFilterByTernifNoBloqueado() throws Exception {
        Ter b = createTer(TEST_ENT, 700, "B", "ZZZ111", 1);
        Ter nb = createTer(TEST_ENT, 701, "NB", "MATCHME", 0);
        saveCommitted(b, nb);

        mockMvc.perform(get("/api/ter/by-ternif-no-bloqueado/" + TEST_ENT + "/ternif/MATCH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ternif", containsString("MATCH")))
                .andExpect(jsonPath("$[0].terblo").value(0));
    }

    @Test
    void shouldSearchByTernifNomAliBloqueado() throws Exception {
        Ter t1 = createTer(TEST_ENT, 800, "ABC Company", "111ABC", 1);
        t1.setTERNIF("111ABC");
        Ter t2 = createTer(TEST_ENT, 801, "Other", "222", 1);
        t2.setTERNIF("222");
        saveCommitted(t1, t2);

        handlerMapping.getHandlerMethods().forEach((info, method) -> {
            var pc = info.getPatternsCondition();
            if (pc == null) {
                System.out.println("[no patterns] -> " + method);
            } else {
                pc.getPatterns().forEach(p -> System.out.println(p + " -> " + method));
            }
        });
        try {
            ResponseEntity<?> direct = terController.search(TEST_ENT, "ABC");
        } catch (Exception e) {
            e.printStackTrace();
        }
        // DEBUG: show DB and spec results before assertions
        List<Ter> all = terRepository.findByENT(TEST_ENT);

        Specification<Ter> spec = TerSearchOptions.searchFiltered(TEST_ENT, "ABC");
        List<Ter> specResults = terRepository.findAll(spec);

        // Stronger assertions that give useful failure info
        assertFalse(specResults.isEmpty(), "Specification returned no results; check TerSearchOptions.searchFiltered");
        assertTrue(specResults.stream().anyMatch(r -> Integer.valueOf(800).equals(r.getTERCOD())), "Expected tercod 800 in specResults: " + specResults);

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
        Ter blocked = createTer(TEST_ENT, 900, "ABC Company", "111ABC", 1);
        Ter good = createTer(TEST_ENT, 901, "ABC Corp", "222ABC", 0);
        good.setTERALI("ABC Alias");
        saveCommitted(blocked, good);

        List<Ter> matches = terRepository.findByENT(TEST_ENT).stream()
            .filter(t -> t.getTERBLO() != null && t.getTERBLO() == 0
                    && ((t.getTERNOM() != null && t.getTERNOM().contains("ABC"))
                        || (t.getTERNIF() != null && t.getTERNIF().contains("ABC"))
                        || (t.getTERALI() != null && t.getTERALI().contains("ABC"))))
            .collect(Collectors.toList());
        assertEquals(1, matches.size(), "Expected one non-blocked 'ABC' match in DB");

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
        Ter t2 = createTer(TEST_ENT, 1001, "Other", "22", 1);
        Ter t3 = createTer(TEST_ENT, 1002, "Tech Corp", "33", 0);
        saveCommitted(t1, t2, t3);

        List<Ter> matches = terRepository.findByENT(TEST_ENT).stream()
        .filter(t -> t.getTERBLO() != null && t.getTERBLO() == 1
                && ((t.getTERNOM() != null && t.getTERNOM().contains("Tech"))
                    || (t.getTERALI() != null && t.getTERALI().contains("Tech"))))
        .collect(Collectors.toList());
        assertEquals(1, matches.size(), "Expected one blocked 'Tech' match in DB");

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
        Ter t1 = createTer(TEST_ENT, 1100, "Tech Solutions", "11", 1);
        t1.setTERALI("TechSol");
        Ter t2 = createTer(TEST_ENT, 1101, "Tech Corp", "22", 0);
        t2.setTERALI("TechCorp");
        saveCommitted(t1, t2);

        List<Ter> matches = terRepository.findByENT(TEST_ENT).stream()
        .filter(t -> t.getTERBLO() != null && t.getTERBLO() == 0
                && ((t.getTERNOM() != null && t.getTERNOM().contains("Tech"))
                    || (t.getTERALI() != null && t.getTERALI().contains("Tech"))))
        .collect(Collectors.toList());
        assertEquals(1, matches.size(), "Expected one non-blocked 'Tech' match in DB");


        mockMvc.perform(get("/api/ter/by-nom-ali-no-bloquado/" + TEST_ENT + "/findMatchingNomOrAli")
                .param("term", "Tech"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tercod").value(1101))
                .andExpect(jsonPath("$[0].terblo").value(0));
    }

    @Test
    void shouldGetByEntAndTercod() throws Exception {
        Ter t = createTer(TEST_ENT, 1200, "Provider By Tercod", "NIF", 0);
        saveCommitted(t);

        assertEquals(1, terRepository.findByENT(TEST_ENT).stream()
                .filter(x -> x.getTERCOD() != null && x.getTERCOD().equals(1200)).count());

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
        Ter t2 = createTer(TEST_ENT, 1301, "P2", "OTHER", 0);
        saveCommitted(t1, t2);

        mockMvc.perform(get("/api/ter/by-ent/" + TEST_ENT + "/ternif/LOOK"))
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
        Ter c = createTer(TEST_ENT, 1402, "XYZ", "333", 1);
        saveCommitted(a, b, c);

        mockMvc.perform(get("/api/ter/by-ent/" + TEST_ENT + "/search-todos")
                .param("term", "ABC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].tercod", containsInAnyOrder(1400, 1401)));
    }

    @Test
    void shouldSaveProveedores() throws Exception {
        Integer next = terRepository.findNextTercodForEnt(TEST_ENT);
        assertTrue(next != null && next > 0, "Unexpected next tercod for ent: " + next);

        String json = String.format("[{\"ENT\":%d,\"TERCOD\":200,\"TERNOM\":\"New Provider A\",\"TERNIF\":\"11111111A\",\"TERBLO\":0,\"TERACU\":0},{\"ENT\":%d,\"TERCOD\":201,\"TERNOM\":\"New Provider B\",\"TERNIF\":\"22222222B\",\"TERBLO\":0,\"TERACU\":0}]", TEST_ENT, TEST_ENT);

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].tercod").value(next))
                .andExpect(jsonPath("$[1].tercod").value(next + 1));

        List<Ter> saved = terRepository.findByENT(TEST_ENT);
        for (Ter s : saved) {
            if ("New Provider A".equals(s.getTERNOM()) || "New Provider B".equals(s.getTERNOM())) {
                createdIds.add(new TerId(s.getENT(), s.getTERCOD()));
            }
        }
    }

    @Test
    void shouldReturn400OnSaveError() throws Exception {
        String json = "[{\"ENT\": " + TEST_ENT + "}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenProveedorNotFound() throws Exception {
        String json = "{ \"TERWEB\": \"web.com\", \"TEROBS\": \"obs\", \"TERBLO\": 1, \"TERACU\": 1 }";

        mockMvc.perform(put("/api/ter/updateFields/" + TEST_ENT + "/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Sin resultado"));
    }
}