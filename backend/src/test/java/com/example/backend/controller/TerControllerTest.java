package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.config.TestExceptionHandler;
import com.example.backend.dto.TerDto;
import com.example.backend.sqlserver2.model.Ter;
import com.example.backend.sqlserver2.model.TerId;
import com.example.backend.sqlserver2.repository.TerRepository;
import com.example.backend.service.ProveedoresSearch;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TerController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandler.class})
public class TerControllerTest {

    private static final int TEST_ENT = 999999;
    private static final String SIN_RESULTADO = "Sin resultado";
    private static final String ERROR = "Error :";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TerRepository terRepository;

    @MockitoBean
    private ProveedoresSearch proveedoresSearch;

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

    private TerDto createTerDto(String nombre, String nif, Integer terblo, Integer teracu) {
        TerDto dto = new TerDto();
        dto.setTERNOM(nombre);
        dto.setTERNIF(nif);
        dto.setTERBLO(terblo != null ? terblo : 0);
        dto.setTERACU(teracu != null ? teracu : 0);
        dto.setTERALI("Alias");
        dto.setTERWEB("www.test.com");
        return dto;
    }

    // ==================== getByEnt Tests ====================

    @Test
    @DisplayName("getByEnt: should return single result successfully")
    void getByEnt_shouldReturnSingleResult() throws Exception {
        Ter ter = createTer(TEST_ENT, 100, "Provider A", "111", 0);
        when(terRepository.findByENT(TEST_ENT)).thenReturn(List.of(ter));

        mockMvc.perform(get("/api/ter/by-ent/" + TEST_ENT))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].tercod").value(100))
            .andExpect(jsonPath("$[0].ternom").value("Provider A"));
    }

    @Test
    @DisplayName("getByEnt: should return multiple results successfully")
    void getByEnt_shouldReturnMultipleResults() throws Exception {
        Ter ter1 = createTer(TEST_ENT, 100, "Provider A", "111", 0);
        Ter ter2 = createTer(TEST_ENT, 101, "Provider B", "222", 1);
        Ter ter3 = createTer(TEST_ENT, 102, "Provider C", "333", 0);
        when(terRepository.findByENT(TEST_ENT)).thenReturn(List.of(ter1, ter2, ter3));

        mockMvc.perform(get("/api/ter/by-ent/" + TEST_ENT))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[*].tercod", containsInAnyOrder(100, 101, 102)))
            .andExpect(jsonPath("$[1].terblo").value(1));
    }

    @Test
    @DisplayName("getByEnt: should return 404 when no results")
    void getByEnt_shouldReturn404WhenEmpty() throws Exception {
        when(terRepository.findByENT(TEST_ENT)).thenReturn(List.of());

        mockMvc.perform(get("/api/ter/by-ent/" + TEST_ENT))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string(SIN_RESULTADO));
    }

    @Test
    @DisplayName("getByEnt: should handle DataAccessException")
    void getByEnt_shouldHandleDataAccessException() throws Exception {
        when(terRepository.findByENT(anyInt()))
            .thenThrow(new DataAccessResourceFailureException("Database connection failed"));

        mockMvc.perform(get("/api/ter/by-ent/" + TEST_ENT))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error :")));
    }

    @Test
    @DisplayName("getByEnt: should handle different ENT values")
    void getByEnt_shouldHandleDifferentEntValues() throws Exception {
        int differentEnt = 888888;
        Ter ter = createTer(differentEnt, 200, "Provider X", "999", 0);
        when(terRepository.findByENT(differentEnt)).thenReturn(List.of(ter));

        mockMvc.perform(get("/api/ter/by-ent/" + differentEnt))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].ent").value(differentEnt));
    }

    // ==================== searchProveedores Tests ====================

    @Test
    @DisplayName("searchProveedores: should search todos mode successfully")
    void searchProveedores_shouldSearchTodosMode() throws Exception {
        Ter ter1 = createTer(TEST_ENT, 100, "ABC Company", "111ABC", 0);
        Ter ter2 = createTer(TEST_ENT, 101, "ABC Corp", "222ABC", 1);
        when(proveedoresSearch.searchProveedoers(TEST_ENT, "todos", "ABC"))
            .thenReturn(List.of(ter1, ter2));

        mockMvc.perform(get("/api/ter/search-proveedores")
                .param("ent", String.valueOf(TEST_ENT))
                .param("searchMode", "todos")
                .param("term", "ABC"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].tercod", containsInAnyOrder(100, 101)));
    }

    @Test
    @DisplayName("searchProveedores: should search Nobloqueado mode successfully")
    void searchProveedores_shouldSearchNobloqueadoMode() throws Exception {
        Ter ter = createTer(TEST_ENT, 100, "ABC Company", "111ABC", 0);
        when(proveedoresSearch.searchProveedoers(TEST_ENT, "Nobloqueado", "ABC"))
            .thenReturn(List.of(ter));

        mockMvc.perform(get("/api/ter/search-proveedores")
                .param("ent", String.valueOf(TEST_ENT))
                .param("searchMode", "Nobloqueado")
                .param("term", "ABC"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].terblo").value(0));
    }

    @Test
    @DisplayName("searchProveedores: should search bloqueado mode successfully")
    void searchProveedores_shouldSearchBloqueadoMode() throws Exception {
        Ter ter = createTer(TEST_ENT, 100, "ABC Company", "111ABC", 1);
        when(proveedoresSearch.searchProveedoers(TEST_ENT, "bloqueado", "ABC"))
            .thenReturn(List.of(ter));

        mockMvc.perform(get("/api/ter/search-proveedores")
                .param("ent", String.valueOf(TEST_ENT))
                .param("searchMode", "bloqueado")
                .param("term", "ABC"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].terblo").value(1));
    }

    @Test
    @DisplayName("searchProveedores: should search by numeric term")
    void searchProveedores_shouldSearchByNumericTerm() throws Exception {
        Ter ter = createTer(TEST_ENT, 100, "Provider", "12345", 0);
        when(proveedoresSearch.searchProveedoers(TEST_ENT, "todos", "12345"))
            .thenReturn(List.of(ter));

        mockMvc.perform(get("/api/ter/search-proveedores")
                .param("ent", String.valueOf(TEST_ENT))
                .param("searchMode", "todos")
                .param("term", "12345"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("searchProveedores: should handle empty search results")
    void searchProveedores_shouldReturn404WhenEmpty() throws Exception {
        when(proveedoresSearch.searchProveedoers(TEST_ENT, "todos", "XYZ"))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/ter/search-proveedores")
                .param("ent", String.valueOf(TEST_ENT))
                .param("searchMode", "todos")
                .param("term", "XYZ"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string(SIN_RESULTADO));
    }

    @Test
    @DisplayName("searchProveedores: should handle ProveedoresSearch exception")
    void searchProveedores_shouldHandleException() throws Exception {
        when(proveedoresSearch.searchProveedoers(anyInt(), anyString(), anyString()))
            .thenThrow(new RuntimeException("Search error"));

        mockMvc.perform(get("/api/ter/search-proveedores")
                .param("ent", String.valueOf(TEST_ENT))
                .param("searchMode", "todos")
                .param("term", "ABC"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error :")));
    }

    @Test
    @DisplayName("searchProveedores: should handle mixed case search term")
    void searchProveedores_shouldHandleMixedCaseTerm() throws Exception {
        Ter ter = createTer(TEST_ENT, 100, "AbC CoMpAnY", "111ABC", 0);
        when(proveedoresSearch.searchProveedoers(TEST_ENT, "todos", "AbC"))
            .thenReturn(List.of(ter));

        mockMvc.perform(get("/api/ter/search-proveedores")
                .param("ent", String.valueOf(TEST_ENT))
                .param("searchMode", "todos")
                .param("term", "AbC"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("searchProveedores: should handle special characters in term")
    void searchProveedores_shouldHandleSpecialCharacters() throws Exception {
        Ter ter = createTer(TEST_ENT, 100, "Company & Co.", "111", 0);
        when(proveedoresSearch.searchProveedoers(TEST_ENT, "todos", "&"))
            .thenReturn(List.of(ter));

        mockMvc.perform(get("/api/ter/search-proveedores")
                .param("ent", String.valueOf(TEST_ENT))
                .param("searchMode", "todos")
                .param("term", "&"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("searchProveedores: should return multiple results")
    void searchProveedores_shouldReturnMultipleResults() throws Exception {
        Ter ter1 = createTer(TEST_ENT, 100, "ABC A", "111", 0);
        Ter ter2 = createTer(TEST_ENT, 101, "ABC B", "222", 0);
        Ter ter3 = createTer(TEST_ENT, 102, "ABC C", "333", 0);
        when(proveedoresSearch.searchProveedoers(TEST_ENT, "Nobloqueado", "ABC"))
            .thenReturn(List.of(ter1, ter2, ter3));

        mockMvc.perform(get("/api/ter/search-proveedores")
                .param("ent", String.valueOf(TEST_ENT))
                .param("searchMode", "Nobloqueado")
                .param("term", "ABC"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)));

        verify(proveedoresSearch).searchProveedoers(TEST_ENT, "Nobloqueado", "ABC");
    }

    // ==================== updateTerFields Tests ====================

    @Test
    @DisplayName("updateTerFields: should update successfully with full payload")
    void updateTerFields_shouldUpdateSuccessfully() throws Exception {
        Ter existing = createTer(TEST_ENT, 1500, "Existing", "NIF1", 0);
        when(terRepository.findById(new TerId(TEST_ENT, 1500))).thenReturn(Optional.of(existing));
        when(terRepository.save(any(Ter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String json = "{ \"TERWEB\": \"newweb.com\", \"TEROBS\": \"newobs\", \"TERBLO\": 1, \"TERACU\": 1 }";

        mockMvc.perform(put("/api/ter/updateFields/" + TEST_ENT + "/1500")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isNoContent());

        verify(terRepository).findById(new TerId(TEST_ENT, 1500));
        verify(terRepository).save(any(Ter.class));
    }

    @Test
    @DisplayName("updateTerFields: should update with null values")
    void updateTerFields_shouldUpdateWithNullValues() throws Exception {
        Ter existing = createTer(TEST_ENT, 1500, "Existing", "NIF1", 0);
        when(terRepository.findById(new TerId(TEST_ENT, 1500))).thenReturn(Optional.of(existing));
        when(terRepository.save(any(Ter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String json = "{ \"TERWEB\": null, \"TEROBS\": null, \"TERBLO\": 0, \"TERACU\": 0 }";

        mockMvc.perform(put("/api/ter/updateFields/" + TEST_ENT + "/1500")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isNoContent());

        verify(terRepository).save(any(Ter.class));
    }

    @Test
    @DisplayName("updateTerFields: should update TERBLO to 1")
    void updateTerFields_shouldUpdateTerbloTo1() throws Exception {
        Ter existing = createTer(TEST_ENT, 1500, "Provider", "NIF", 0);
        when(terRepository.findById(new TerId(TEST_ENT, 1500))).thenReturn(Optional.of(existing));
        when(terRepository.save(any(Ter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String json = "{ \"TERWEB\": \"web.com\", \"TEROBS\": \"obs\", \"TERBLO\": 1, \"TERACU\": 0 }";

        mockMvc.perform(put("/api/ter/updateFields/" + TEST_ENT + "/1500")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isNoContent());

        verify(terRepository).save(any(Ter.class));
    }

    @Test
    @DisplayName("updateTerFields: should return 404 when record not found")
    void updateTerFields_shouldReturn404WhenNotFound() throws Exception {
        when(terRepository.findById(new TerId(TEST_ENT, 99999))).thenReturn(Optional.empty());

        String json = "{ \"TERWEB\": \"web.com\", \"TEROBS\": \"obs\", \"TERBLO\": 1, \"TERACU\": 1 }";

        mockMvc.perform(put("/api/ter/updateFields/" + TEST_ENT + "/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string(SIN_RESULTADO));

        verify(terRepository, never()).save(any(Ter.class));
    }

    @Test
    @DisplayName("updateTerFields: should handle DataAccessException on find")
    void updateTerFields_shouldHandleDataAccessExceptionOnFind() throws Exception {
        when(terRepository.findById(any()))
            .thenThrow(new DataAccessResourceFailureException("DB down"));

        String json = "{ \"TERWEB\": \"web.com\", \"TEROBS\": \"obs\", \"TERBLO\": 1, \"TERACU\": 1 }";

        mockMvc.perform(put("/api/ter/updateFields/" + TEST_ENT + "/1500")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(containsString("Error :")));
    }

    @Test
    @DisplayName("updateTerFields: should handle DataAccessException on save")
    void updateTerFields_shouldHandleDataAccessExceptionOnSave() throws Exception {
        Ter existing = createTer(TEST_ENT, 1500, "Existing", "NIF1", 0);
        when(terRepository.findById(new TerId(TEST_ENT, 1500))).thenReturn(Optional.of(existing));
        when(terRepository.save(any(Ter.class)))
            .thenThrow(new DataAccessResourceFailureException("DB error"));

        String json = "{ \"TERWEB\": \"newweb.com\", \"TEROBS\": \"newobs\", \"TERBLO\": 1, \"TERACU\": 1 }";

        mockMvc.perform(put("/api/ter/updateFields/" + TEST_ENT + "/1500")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(containsString("Error :")));
    }

    @Test
    @DisplayName("updateTerFields: should update multiple fields")
    void updateTerFields_shouldUpdateMultipleFields() throws Exception {
        Ter existing = createTer(TEST_ENT, 1500, "Old Name", "OldNIF", 0);
        when(terRepository.findById(new TerId(TEST_ENT, 1500))).thenReturn(Optional.of(existing));
        when(terRepository.save(any(Ter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String json = "{ \"TERWEB\": \"www.new.com\", \"TEROBS\": \"Updated observation\", \"TERBLO\": 1, \"TERACU\": 1 }";

        mockMvc.perform(put("/api/ter/updateFields/" + TEST_ENT + "/1500")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("updateTerFields: should work with different tercod values")
    void updateTerFields_shouldWorkWithDifferentTercod() throws Exception {
        Ter existing = createTer(TEST_ENT, 2000, "Provider", "NIF", 0);
        when(terRepository.findById(new TerId(TEST_ENT, 2000))).thenReturn(Optional.of(existing));
        when(terRepository.save(any(Ter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String json = "{ \"TERWEB\": \"web.com\", \"TEROBS\": \"obs\", \"TERBLO\": 0, \"TERACU\": 0 }";

        mockMvc.perform(put("/api/ter/updateFields/" + TEST_ENT + "/2000")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isNoContent());
    }

    // ==================== createMultipleForEnt Tests ====================

    @Test
    @DisplayName("createMultipleForEnt: should create single provider successfully")
    void createMultipleForEnt_shouldCreateSingleProvider() throws Exception {
        when(terRepository.findNextTercodForEnt(TEST_ENT)).thenReturn(1);
        when(terRepository.save(any(Ter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String json = "[{\"TERNOM\":\"New Provider\",\"TERNIF\":\"11111111A\",\"TERBLO\":0,\"TERACU\":0}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].tercod").value(1));

        verify(terRepository).findNextTercodForEnt(TEST_ENT);
        verify(terRepository).save(any(Ter.class));
    }

    @Test
    @DisplayName("createMultipleForEnt: should create multiple providers")
    void createMultipleForEnt_shouldCreateMultipleProviders() throws Exception {
        when(terRepository.findNextTercodForEnt(TEST_ENT)).thenReturn(100);
        when(terRepository.save(any(Ter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String json = "[{\"TERNOM\":\"Provider A\",\"TERNIF\":\"11111111A\",\"TERBLO\":0,\"TERACU\":0},"
                    + "{\"TERNOM\":\"Provider B\",\"TERNIF\":\"22222222B\",\"TERBLO\":1,\"TERACU\":1},"
                    + "{\"TERNOM\":\"Provider C\",\"TERNIF\":\"33333333C\",\"TERBLO\":0,\"TERACU\":0}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].tercod").value(100))
            .andExpect(jsonPath("$[1].tercod").value(101))
            .andExpect(jsonPath("$[2].tercod").value(102));

        verify(terRepository, times(3)).save(any(Ter.class));
    }

    @Test
    @DisplayName("createMultipleForEnt: should return 400 when dtos is null")
    void createMultipleForEnt_shouldReturn400WhenNull() throws Exception {
        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Faltan datos obligatorios"));

        verify(terRepository, never()).save(any(Ter.class));
    }

    @Test
    @DisplayName("createMultipleForEnt: should return 400 when dtos is empty")
    void createMultipleForEnt_shouldReturn400WhenEmpty() throws Exception {
        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Faltan datos obligatorios"));

        verify(terRepository, never()).save(any(Ter.class));
    }

    @Test
    @DisplayName("createMultipleForEnt: should return 400 when TERNOM is missing")
    void createMultipleForEnt_shouldReturn400WhenTernomMissing() throws Exception {
        when(terRepository.findNextTercodForEnt(TEST_ENT)).thenReturn(1);

        String json = "[{\"TERNIF\":\"11111111A\",\"TERBLO\":0,\"TERACU\":0}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Datos incompletos"));

        verify(terRepository, never()).save(any(Ter.class));
    }

    @Test
    @DisplayName("createMultipleForEnt: should return 400 when TERNOM is empty")
    void createMultipleForEnt_shouldReturn400WhenTernomEmpty() throws Exception {
        when(terRepository.findNextTercodForEnt(TEST_ENT)).thenReturn(1);

        String json = "[{\"TERNOM\":\"\",\"TERNIF\":\"11111111A\",\"TERBLO\":0,\"TERACU\":0}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Datos incompletos"));

        verify(terRepository, never()).save(any(Ter.class));
    }

    @Test
    @DisplayName("createMultipleForEnt: should return 400 when TERNOM is whitespace only")
    void createMultipleForEnt_shouldReturn400WhenTernomWhitespace() throws Exception {
        when(terRepository.findNextTercodForEnt(TEST_ENT)).thenReturn(1);

        String json = "[{\"TERNOM\":\"   \",\"TERNIF\":\"11111111A\",\"TERBLO\":0,\"TERACU\":0}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Datos incompletos"));

        verify(terRepository, never()).save(any(Ter.class));
    }

    @Test
    @DisplayName("createMultipleForEnt: should return 400 when TERNIF is null")
    void createMultipleForEnt_shouldReturn400WhenTernifNull() throws Exception {
        when(terRepository.findNextTercodForEnt(TEST_ENT)).thenReturn(1);

        String json = "[{\"TERNOM\":\"Provider\",\"TERNIF\":null,\"TERBLO\":0,\"TERACU\":0}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Datos incompletos"));

        verify(terRepository, never()).save(any(Ter.class));
    }

    @Test
    @DisplayName("createMultipleForEnt: should return 400 when TERNIF is empty")
    void createMultipleForEnt_shouldReturn400WhenTernifEmpty() throws Exception {
        when(terRepository.findNextTercodForEnt(TEST_ENT)).thenReturn(1);

        String json = "[{\"TERNOM\":\"Provider\",\"TERNIF\":\"\",\"TERBLO\":0,\"TERACU\":0}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Datos incompletos"));

        verify(terRepository, never()).save(any(Ter.class));
    }

    @Test
    @DisplayName("createMultipleForEnt: should return 400 when TERNIF is missing")
    void createMultipleForEnt_shouldReturn400WhenTernifMissing() throws Exception {
        when(terRepository.findNextTercodForEnt(TEST_ENT)).thenReturn(1);

        String json = "[{\"TERNOM\":\"Provider\",\"TERBLO\":0,\"TERACU\":0}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Datos incompletos"));

        verify(terRepository, never()).save(any(Ter.class));
    }

    @Test
    @DisplayName("createMultipleForEnt: should return 500 when findNextTercodForEnt returns null")
    void createMultipleForEnt_shouldReturn500WhenNextTercodNull() throws Exception {
        when(terRepository.findNextTercodForEnt(TEST_ENT)).thenReturn(null);

        String json = "[{\"TERNOM\":\"Provider\",\"TERNIF\":\"11111111A\",\"TERBLO\":0,\"TERACU\":0}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isInternalServerError());

        verify(terRepository, never()).save(any(Ter.class));
    }

    @Test
    @DisplayName("createMultipleForEnt: should increment tercod for multiple providers")
    void createMultipleForEnt_shouldIncrementTercod() throws Exception {
        when(terRepository.findNextTercodForEnt(TEST_ENT)).thenReturn(50);
        when(terRepository.save(any(Ter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String json = "[{\"TERNOM\":\"A\",\"TERNIF\":\"111\",\"TERBLO\":0,\"TERACU\":0},"
                    + "{\"TERNOM\":\"B\",\"TERNIF\":\"222\",\"TERBLO\":0,\"TERACU\":0},"
                    + "{\"TERNOM\":\"C\",\"TERNIF\":\"333\",\"TERBLO\":0,\"TERACU\":0}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$[0].tercod").value(50))
            .andExpect(jsonPath("$[1].tercod").value(51))
            .andExpect(jsonPath("$[2].tercod").value(52));
    }

    @Test
    @DisplayName("createMultipleForEnt: should set correct ENT for all providers")
    void createMultipleForEnt_shouldSetCorrectEnt() throws Exception {
        when(terRepository.findNextTercodForEnt(TEST_ENT)).thenReturn(1);
        when(terRepository.save(any(Ter.class))).thenAnswer(invocation -> {
            Ter t = invocation.getArgument(0);
            if (t.getENT() != TEST_ENT) {
                throw new IllegalArgumentException("ENT mismatch");
            }
            return t;
        });

        String json = "[{\"TERNOM\":\"Provider\",\"TERNIF\":\"NIF\",\"TERBLO\":0,\"TERACU\":0}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("createMultipleForEnt: should set TERBLO and TERACU to 0")
    void createMultipleForEnt_shouldSetTerbloAndTeracu() throws Exception {
        when(terRepository.findNextTercodForEnt(TEST_ENT)).thenReturn(1);
        when(terRepository.save(any(Ter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String json = "[{\"TERNOM\":\"Provider A\",\"TERNIF\":\"111\",\"TERBLO\":0,\"TERACU\":0},"
                    + "{\"TERNOM\":\"Provider B\",\"TERNIF\":\"222\",\"TERBLO\":1,\"TERACU\":1}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$[0].terblo").value(0))
            .andExpect(jsonPath("$[0].teracu").value(0))
            .andExpect(jsonPath("$[1].terblo").value(0))
            .andExpect(jsonPath("$[1].teracu").value(0));
    }

    @Test
    @DisplayName("createMultipleForEnt: should handle optional fields")
    void createMultipleForEnt_shouldHandleOptionalFields() throws Exception {
        when(terRepository.findNextTercodForEnt(TEST_ENT)).thenReturn(1);
        when(terRepository.save(any(Ter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String json = "[{\"TERNOM\":\"Provider\",\"TERNIF\":\"NIF\",\"TERDOM\":\"Address\","
                    + "\"TERCPO\":\"12345\",\"TERTЕЛ\":\"555-1234\",\"TERFAX\":\"555-5678\","
                    + "\"TERWEB\":\"www.test.com\",\"TERACU\":0,\"TERBLO\":0}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$", hasSize(1)));

        verify(terRepository).save(any(Ter.class));
    }

    @Test
    @DisplayName("createMultipleForEnt: should handle mixed valid and invalid dtos")
    void createMultipleForEnt_shouldReturn400OnFirstInvalidDto() throws Exception {
        when(terRepository.findNextTercodForEnt(TEST_ENT)).thenReturn(1);

        String json = "[{\"TERNOM\":\"Valid\",\"TERNIF\":\"111\",\"TERBLO\":0,\"TERACU\":0},"
                    + "{\"TERNOM\":\"\",\"TERNIF\":\"222\",\"TERBLO\":0,\"TERACU\":0}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isBadRequest());

        verify(terRepository, never()).save(any(Ter.class));
    }

    @Test
    @DisplayName("createMultipleForEnt: should return 400 on TERNIF whitespace")
    void createMultipleForEnt_shouldReturn400WhenTernifWhitespace() throws Exception {
        when(terRepository.findNextTercodForEnt(TEST_ENT)).thenReturn(1);

        String json = "[{\"TERNOM\":\"Provider\",\"TERNIF\":\"   \",\"TERBLO\":0,\"TERACU\":0}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isBadRequest());

        verify(terRepository, never()).save(any(Ter.class));
    }

    @Test
    @DisplayName("createMultipleForEnt: should handle DataAccessException during save")
    void createMultipleForEnt_shouldHandleDataAccessExceptionOnSave() throws Exception {
        when(terRepository.findNextTercodForEnt(TEST_ENT)).thenReturn(1);
        when(terRepository.save(any(Ter.class)))
            .thenThrow(new DataAccessResourceFailureException("DB error"));

        String json = "[{\"TERNOM\":\"Provider\",\"TERNIF\":\"NIF\",\"TERBLO\":0,\"TERACU\":0}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Server error")));
    }

    @Test
    @DisplayName("createMultipleForEnt: should handle generic exception")
    void createMultipleForEnt_shouldHandleGenericException() throws Exception {
        when(terRepository.findNextTercodForEnt(TEST_ENT)).thenReturn(1);
        when(terRepository.save(any(Ter.class)))
            .thenThrow(new RuntimeException("Unexpected error"));

        String json = "[{\"TERNOM\":\"Provider\",\"TERNIF\":\"NIF\",\"TERBLO\":0,\"TERACU\":0}]";

        mockMvc.perform(post("/api/ter/save-proveedores/" + TEST_ENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Server error")));
    }
}