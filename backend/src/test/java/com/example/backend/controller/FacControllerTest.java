package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.dto.FacWithTerProjection;
import com.example.backend.config.TestExceptionHandler;
import com.example.backend.service.FacturaInsertService;
import com.example.backend.sqlserver2.model.Fac;
import com.example.backend.sqlserver2.model.FacId;
import com.example.backend.sqlserver2.model.Fde;
import com.example.backend.sqlserver2.model.Gbs;
import com.example.backend.sqlserver2.model.GbsId;
import com.example.backend.sqlserver2.model.Ter;
import com.example.backend.sqlserver2.repository.FacRepository;
import com.example.backend.sqlserver2.repository.TerRepository;
import com.example.backend.sqlserver2.repository.FdeRepository;
import com.example.backend.sqlserver2.repository.GbsRepository;
import com.example.backend.service.FacturaSearch;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FacController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandler.class})
public class FacControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FacRepository facRepository;

    @MockitoBean
    private TerRepository terRepository;

    @MockitoBean
    private FacturaInsertService facturaInsertService;

    @MockitoBean
    private FdeRepository fdeRepository;

    @MockitoBean
    private GbsRepository gbsRepository;

    @MockitoBean
    private FacturaSearch facturaSearch;

    static class FacWithTerProjectionImpl implements FacWithTerProjection {
        private Integer ent; private String eje; private Integer facnum; private Integer tercod;
        private String cgecod; private String facobs; private Double facimp; private Double faciec;
        private Double facidi; private String factdc; private Integer facann; private Integer facfac;
        private String facdoc; private LocalDateTime facdat; private LocalDateTime facfco; private String facado;
        private String factxt; private LocalDateTime facfre; private String conctp; private String concpr;
        private String conccr; private Integer facoct; private String facfpg; private String facopg;
        private String factpg; private Double facdto; private String terNom; private String terNif;

        public Integer getENT() { return ent; }
        public String getEJE() { return eje; }
        public Integer getFACNUM() { return facnum; }
        public Integer getTERCOD() { return tercod; }
        public String getCGECOD() { return cgecod; }
        public String getFACOBS() { return facobs; }
        public Double getFACIMP() { return facimp; }
        public Double getFACIEC() { return faciec; }
        public Double getFACIDI() { return facidi; }
        public String getFACTDC() { return factdc; }
        public Integer getFACANN() { return facann; }
        public Integer getFACFAC() { return facfac; }
        public String getFACDOC() { return facdoc; }
        public LocalDateTime getFACDAT() { return facdat; }
        public LocalDateTime getFACFCO() { return facfco; }
        public String getFACADO() { return facado; }
        public String getFACTXT() { return factxt; }
        public LocalDateTime getFACFRE() { return facfre; }
        public String getCONCTP() { return conctp; }
        public String getCONCPR() { return concpr; }
        public String getCONCCR() { return conccr; }
        public Integer getFACOCT() { return facoct; }
        public String getFACFPG() { return facfpg; }
        public String getFACOPG() { return facopg; }
        public String getFACTPG() { return factpg; }
        public Double getFACDTO() { return facdto; }
        public String getTer_TERNOM() { return terNom; }
        public String getTer_TERNIF() { return terNif; }
    }

    @Test
    void getFacturas_returns200WithDto() throws Exception {
        FacWithTerProjectionImpl projection = new FacWithTerProjectionImpl();
        projection.ent = 1;
        projection.eje = "E1";
        projection.facnum = 123;
        projection.tercod = 1;
        projection.cgecod = "C1";
        projection.facobs = "OBS1";
        projection.facimp = 100.5;
        projection.faciec = 0.0;
        projection.facidi = 0.0;
        projection.factdc = "TDC";
        projection.facann = 0;
        projection.facfac = 0;
        projection.facdoc = "DOC";
        projection.facdat = null;
        projection.facfco = null;
        projection.facado = "ADO";
        projection.factxt = "TXT";
        projection.facfre = null;
        projection.conctp = "CTP";
        projection.concpr = "CPR";
        projection.conccr = "CCR";
        projection.facoct = 0;
        projection.facfpg = "FPG";
        projection.facopg = "OPG";
        projection.factpg = "TPG";
        projection.facdto = 0.0;
        projection.terNom = "Cliente X";
        projection.terNif = "NIF123";

        when(facRepository.findByENTAndEJEAndCGECODOrderByFACFREAsc(1, "E1", "C1"))
            .thenReturn(List.of(projection));

        mockMvc.perform(get("/api/fac/1/E1/C1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].facnum", is(123)))
            .andExpect(jsonPath("$[0].ter_TERNOM", is("Cliente X")));
    }

    @Test
    void getFacturas_returnsNotFoundWhenEmpty() throws Exception {
        when(facRepository.findByENTAndEJEAndCGECODOrderByFACFREAsc(1, "E1", "C1"))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/fac/1/E1/C1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("Sin resultado")));
    }

    @Test
    void getFacturas_returnsBadRequestOnDataAccessException() throws Exception {
        when(facRepository.findByENTAndEJEAndCGECODOrderByFACFREAsc(anyInt(), anyString(), anyString()))
            .thenThrow(new DataAccessResourceFailureException("DB down"));

        mockMvc.perform(get("/api/fac/1/E1/C1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error :")));
    }

    @Test
    void addFacturas_returnsOkWithMessages() throws Exception {
        List<String> messages = List.of("Factura 1 insertada", "Factura 2 insertada");
        when(facturaInsertService.insertFacturas(any())).thenReturn(messages);

        List<Map<String, Object>> payload = List.of(new HashMap<>());

        mockMvc.perform(post("/api/fac/add-facturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        verify(facturaInsertService).insertFacturas(any());
    }

    @Test
    void addFacturas_returnsBadRequestOnException() throws Exception {
        when(facturaInsertService.insertFacturas(any()))
            .thenThrow(new RuntimeException("Insert failed"));

        mockMvc.perform(post("/api/fac/add-facturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of())))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error :")));
    }

    @Test
    void updateFactura_returnsNoContentOnSuccess() throws Exception {
        FacId id = new FacId(1, "E1", 100);
        Fac factura = new Fac();
        when(facRepository.findById(id)).thenReturn(Optional.of(factura));

        Map<String, Object> payload = Map.of(
            "FACOBS", "Observacion",
            "CONCTP", "TP",
            "CONCPR", "PR",
            "CONCCR", "CR",
            "FACFRE", "2026-01-22T12:00:00",
            "FACFPG", "FPG",
            "FACOPG", "OPG",
            "FACTPG", "TPG",
            "FACOCT", 5
        );

        mockMvc.perform(patch("/api/fac/update-factura/1/E1/100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isNoContent());

        verify(facRepository).save(any(Fac.class));
    }

    @Test
    void updateFactura_returnsNotFoundWhenMissing() throws Exception {
        FacId id = new FacId(9, "X", 999);
        when(facRepository.findById(id)).thenReturn(Optional.empty());

        Map<String, Object> payload = Map.of(
            "FACOBS", "Obs",
            "CONCTP", "TP",
            "CONCPR", "PR",
            "CONCCR", "CR",
            "FACFRE", "2026-01-22T12:00:00",
            "FACFPG", "FPG",
            "FACOPG", "OPG",
            "FACTPG", "TPG",
            "FACOCT", 0
        );

        mockMvc.perform(patch("/api/fac/update-factura/9/X/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void updateFactura_returnsNoContentWithMissingFields() throws Exception {
        FacId id = new FacId(1, "E1", 100);
        Fac factura = new Fac();
        when(facRepository.findById(id)).thenReturn(Optional.of(factura));

        Map<String, Object> payload = Map.of(
            "FACOBS", "Obs",
            "CONCTP", "TP"
        );

        mockMvc.perform(patch("/api/fac/update-factura/1/E1/100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isNoContent());

        verify(facRepository).save(any(Fac.class));
    }

    @Test
    void updateFactura_returnsBadRequestOnException() throws Exception {
        FacId id = new FacId(1, "E1", 100);
        when(facRepository.findById(id)).thenThrow(new DataAccessResourceFailureException("DB error"));

        Map<String, Object> payload = Map.of(
            "FACOBS", "Obs",
            "CONCTP", "TP",
            "CONCPR", "PR",
            "CONCCR", "CR",
            "FACFRE", "2026-01-22T12:00:00",
            "FACFPG", "FPG",
            "FACOPG", "OPG",
            "FACTPG", "TPG",
            "FACOCT", 0
        );

        mockMvc.perform(patch("/api/fac/update-factura/1/E1/100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error :")));
    }

    @Test
    void searchContabilizacion_returnsOkWithResults() throws Exception {
        Fac f = new Fac();
        f.setENT(1);
        f.setEJE("E1");
        f.setFACNUM(200);
        when(facRepository.findAll(any(Specification.class))).thenReturn(List.of(f));

        mockMvc.perform(get("/api/fac/contabilizacion/search")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void searchContabilizacion_returnsNotFoundWhenEmpty() throws Exception {
        when(facRepository.findAll(any(Specification.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/fac/contabilizacion/search")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void searchContabilizacion_returnsBadRequestOnException() throws Exception {
        when(facRepository.findAll(any(Specification.class)))
            .thenThrow(new DataAccessResourceFailureException("DB error"));

        mockMvc.perform(get("/api/fac/contabilizacion/search")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error :")));
    }

    @Test
    void contabilizarFactura_returnsBadRequestWhenPayloadNull() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ENT", 1);
        payload.put("EJE", null);
        payload.put("FACNUM", 100);
        payload.put("FACADO", "ADO");
        payload.put("FACFCO", "2026-01-22T12:00:00");
        payload.put("CGECOD", "C1");
        payload.put("ESCONTRATO", false);

        mockMvc.perform(patch("/api/fac/contabilizar-facturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Falta un dato obligatorio")));
    }

    @Test
    void contabilizarFactura_returnsNotFoundWhenFacturaMissing() throws Exception {
        when(facRepository.findById(any(FacId.class))).thenReturn(Optional.empty());

        Map<String, Object> payload = Map.of(
            "ENT", 1,
            "EJE", "E1",
            "FACNUM", 100,
            "FACADO", "ADO",
            "FACFCO", "2026-01-22T12:00:00",
            "CGECOD", "C1",
            "ESCONTRATO", false
        );

        mockMvc.perform(patch("/api/fac/contabilizar-facturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("Factura no encontrada")));
    }

    @Test
    void contabilizarFactura_returnsBadRequestWhenBolsaMissing() throws Exception {
        FacId facId = new FacId(1, "E1", 100);
        Fac factura = new Fac();
        when(facRepository.findById(facId)).thenReturn(Optional.of(factura));

        Fde fde = new Fde();
        fde.setFDEORG("ORG");
        fde.setFDEFUN("FUN");
        fde.setFDEECO("ECO");
        when(fdeRepository.findByENTAndEJEAndFACNUM(1, "E1", 100)).thenReturn(List.of(fde));
        when(gbsRepository.findByENTAndEJEAndCGECODAndGBSORGAndGBSFUNAndGBSECO(1, "E1", "C1", "ORG", "FUN", "ECO"))
            .thenReturn(Optional.empty());

        Map<String, Object> payload = Map.of(
            "ENT", 1,
            "EJE", "E1",
            "FACNUM", 100,
            "FACADO", "ADO",
            "FACFCO", "2026-01-22T12:00:00",
            "CGECOD", "C1",
            "ESCONTRATO", false
        );

        mockMvc.perform(patch("/api/fac/contabilizar-facturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("No existe bolsa para")));
    }

    @Test
    void contabilizarFactura_returnsNoContentOnSuccessWithContract() throws Exception {
        FacId facId = new FacId(1, "E1", 100);
        Fac factura = new Fac();
        when(facRepository.findById(facId)).thenReturn(Optional.of(factura));

        Map<String, Object> payload = Map.of(
            "ENT", 1,
            "EJE", "E1",
            "FACNUM", 100,
            "FACADO", "ADO",
            "FACFCO", "2026-01-22T12:00:00",
            "CGECOD", "C1",
            "ESCONTRATO", true
        );

        mockMvc.perform(patch("/api/fac/contabilizar-facturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isNoContent());

        verify(facRepository).save(any(Fac.class));
        verify(fdeRepository, never()).findByENTAndEJEAndFACNUM(anyInt(), anyString(), anyInt());
    }

    @Test
    void contabilizarFactura_returnsNoContentOnSuccessWithBolsas() throws Exception {
        FacId facId = new FacId(1, "E1", 100);
        Fac factura = new Fac();
        when(facRepository.findById(facId)).thenReturn(Optional.of(factura));

        Fde fde = new Fde();
        fde.setFDEORG("ORG");
        fde.setFDEFUN("FUN");
        fde.setFDEECO("ECO");
        fde.setFDEIMP(50.0);
        fde.setFDEDIF(10.0);
        when(fdeRepository.findByENTAndEJEAndFACNUM(1, "E1", 100)).thenReturn(List.of(fde));

        GbsId gbsId = new GbsId(1, "E1", "C1", "REF");
        Gbs bolsa = new Gbs();
        bolsa.setGBSIUS(100.0);
        bolsa.setGBSIUT(100.0);
        when(gbsRepository.findByENTAndEJEAndCGECODAndGBSORGAndGBSFUNAndGBSECO(1, "E1", "C1", "ORG", "FUN", "ECO"))
            .thenReturn(Optional.of(bolsa));

        Map<String, Object> payload = Map.of(
            "ENT", 1,
            "EJE", "E1",
            "FACNUM", 100,
            "FACADO", "ADO",
            "FACFCO", "2026-01-22T12:00:00",
            "CGECOD", "C1",
            "ESCONTRATO", false
        );

        mockMvc.perform(patch("/api/fac/contabilizar-facturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isNoContent());

        verify(facRepository).save(any(Fac.class));
        verify(gbsRepository).save(any(Gbs.class));
    }

    @Test
    void contabilizarFactura_returnsBadRequestOnException() throws Exception {
        when(facRepository.findById(any(FacId.class)))
            .thenThrow(new DataAccessResourceFailureException("DB error"));

        Map<String, Object> payload = Map.of(
            "ENT", 1,
            "EJE", "E1",
            "FACNUM", 100,
            "FACADO", "ADO",
            "FACFCO", "2026-01-22T12:00:00",
            "CGECOD", "C1",
            "ESCONTRATO", false
        );

        mockMvc.perform(patch("/api/fac/contabilizar-facturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error :")));
    }

    @Test
    void searchFacturas_returnsOkWithAllParameters() throws Exception {
        FacWithTerProjectionImpl projection = new FacWithTerProjectionImpl();
        projection.ent = 1;
        projection.eje = "E1";
        projection.facnum = 100;
        projection.tercod = 1;
        projection.cgecod = "C1";
        projection.facimp = 500.0;
        projection.terNom = "Proveedor A";
        projection.terNif = "NIF001";

        when(facturaSearch.searchFactura(any(FacturaSearch.FacturaSearchCriteria.class)))
            .thenReturn(List.of(projection));

        mockMvc.perform(get("/api/fac/search-factura")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C1")
                .param("main_filter", "search")
                .param("ej_factura", "2024")
                .param("estado", "CONT")
                .param("fecha", "REGISTRO")
                .param("fromDate", "2026-01-01")
                .param("toDate", "2026-12-31")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].facnum", is(100)))
            .andExpect(jsonPath("$[0].ter_TERNOM", is("Proveedor A")));

        verify(facturaSearch).searchFactura(any(FacturaSearch.FacturaSearchCriteria.class));
    }

    @Test
    void searchFacturas_returnsOkWithOnlyRequiredParameters() throws Exception {
        FacWithTerProjectionImpl projection = new FacWithTerProjectionImpl();
        projection.ent = 1;
        projection.eje = "E1";
        projection.facnum = 200;
        projection.tercod = 2;
        projection.cgecod = "C1";
        projection.facimp = 250.0;
        projection.terNom = "Proveedor B";

        when(facturaSearch.searchFactura(any(FacturaSearch.FacturaSearchCriteria.class)))
            .thenReturn(List.of(projection));

        mockMvc.perform(get("/api/fac/search-factura")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].facnum", is(200)));

        verify(facturaSearch).searchFactura(any(FacturaSearch.FacturaSearchCriteria.class));
    }

    @Test
    void searchFacturas_returnsOkWithPartialOptionalParameters() throws Exception {
        FacWithTerProjectionImpl projection = new FacWithTerProjectionImpl();
        projection.facnum = 300;
        projection.facimp = 750.50;
        projection.terNom = "Proveedor C";

        when(facturaSearch.searchFactura(any(FacturaSearch.FacturaSearchCriteria.class)))
            .thenReturn(List.of(projection));

        mockMvc.perform(get("/api/fac/search-factura")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C2")
                .param("main_filter", "supplier")
                .param("estado", "NO_CONT")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].facnum", is(300)));
    }

    @Test
    void searchFacturas_returnsOkWithDateRangeOnly() throws Exception {
        FacWithTerProjectionImpl projection = new FacWithTerProjectionImpl();
        projection.facnum = 400;
        projection.facimp = 1200.0;

        when(facturaSearch.searchFactura(any(FacturaSearch.FacturaSearchCriteria.class)))
            .thenReturn(List.of(projection));

        mockMvc.perform(get("/api/fac/search-factura")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C1")
                .param("fecha", "FACTURA")
                .param("fromDate", "2026-03-01")
                .param("toDate", "2026-03-31")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void searchFacturas_returnsOkWithFromDateOnly() throws Exception {
        FacWithTerProjectionImpl projection = new FacWithTerProjectionImpl();
        projection.facnum = 500;

        when(facturaSearch.searchFactura(any(FacturaSearch.FacturaSearchCriteria.class)))
            .thenReturn(List.of(projection));

        mockMvc.perform(get("/api/fac/search-factura")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C1")
                .param("fecha", "REGISTRO")
                .param("fromDate", "2026-01-15")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk());

        verify(facturaSearch).searchFactura(any(FacturaSearch.FacturaSearchCriteria.class));
    }

    @Test
    void searchFacturas_returnsOkWithToDateOnly() throws Exception {
        FacWithTerProjectionImpl projection = new FacWithTerProjectionImpl();
        projection.facnum = 600;

        when(facturaSearch.searchFactura(any(FacturaSearch.FacturaSearchCriteria.class)))
            .thenReturn(List.of(projection));

        mockMvc.perform(get("/api/fac/search-factura")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C1")
                .param("fecha", "CONTABLE")
                .param("toDate", "2026-06-30")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    void searchFacturas_returnsOkWithInvoiceYear() throws Exception {
        FacWithTerProjectionImpl projection = new FacWithTerProjectionImpl();
        projection.facnum = 700;
        projection.facann = 2024;

        when(facturaSearch.searchFactura(any(FacturaSearch.FacturaSearchCriteria.class)))
            .thenReturn(List.of(projection));

        mockMvc.perform(get("/api/fac/search-factura")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C1")
                .param("ej_factura", "2024")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].facann", is(2024)));
    }

    @Test
    void searchFacturas_returnsNotFoundWhenNoResults() throws Exception {
        when(facturaSearch.searchFactura(any(FacturaSearch.FacturaSearchCriteria.class)))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/fac/search-factura")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C1")
                .param("main_filter", "nonexistent")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void searchFacturas_returnsNotFoundWhenEmptyResult() throws Exception {
        when(facturaSearch.searchFactura(any(FacturaSearch.FacturaSearchCriteria.class)))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/fac/search-factura")
                .param("ent", "999")
                .param("eje", "E99")
                .param("cgecod", "C99")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void searchFacturas_returnsBadRequestOnServiceException() throws Exception {
        when(facturaSearch.searchFactura(any(FacturaSearch.FacturaSearchCriteria.class)))
            .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/api/fac/search-factura")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error :")));
    }

    @Test
    void searchFacturas_returnsBadRequestOnNullPointerException() throws Exception {
        when(facturaSearch.searchFactura(any(FacturaSearch.FacturaSearchCriteria.class)))
            .thenThrow(new NullPointerException("Filter parameter is null"));

        mockMvc.perform(get("/api/fac/search-factura")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Filter parameter is null")));
    }

    @Test
    void searchFacturas_returnsOkWithMultipleResults() throws Exception {
        FacWithTerProjectionImpl proj1 = new FacWithTerProjectionImpl();
        proj1.facnum = 800;
        proj1.facimp = 100.0;
        proj1.terNom = "Prov 1";

        FacWithTerProjectionImpl proj2 = new FacWithTerProjectionImpl();
        proj2.facnum = 801;
        proj2.facimp = 200.0;
        proj2.terNom = "Prov 2";

        FacWithTerProjectionImpl proj3 = new FacWithTerProjectionImpl();
        proj3.facnum = 802;
        proj3.facimp = 300.0;
        proj3.terNom = "Prov 3";

        when(facturaSearch.searchFactura(any(FacturaSearch.FacturaSearchCriteria.class)))
            .thenReturn(List.of(proj1, proj2, proj3));

        mockMvc.perform(get("/api/fac/search-factura")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C1")
                .param("estado", "CONT")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].facnum", is(800)))
            .andExpect(jsonPath("$[1].facnum", is(801)))
            .andExpect(jsonPath("$[2].facnum", is(802)));
    }

    @Test
    void searchFacturas_returnsOkWithAllFiltersAndMultipleResults() throws Exception {
        FacWithTerProjectionImpl projection = new FacWithTerProjectionImpl();
        projection.ent = 1;
        projection.eje = "E1";
        projection.facnum = 900;
        projection.tercod = 5;
        projection.cgecod = "C1";
        projection.facimp = 5000.0;
        projection.faciec = 2000.0;
        projection.facidi = 1000.0;
        projection.facann = 2024;
        projection.facado = null;
        projection.terNom = "Big Supplier";
        projection.terNif = "NIF999";

        when(facturaSearch.searchFactura(any(FacturaSearch.FacturaSearchCriteria.class)))
            .thenReturn(List.of(projection));

        mockMvc.perform(get("/api/fac/search-factura")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C1")
                .param("main_filter", "12345")
                .param("ej_factura", "2024")
                .param("estado", "PTE_APL")
                .param("fecha", "REGISTRO")
                .param("fromDate", "2025-01-01")
                .param("toDate", "2026-12-31")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].facnum", is(900)))
            .andExpect(jsonPath("$[0].facann", is(2024)))
            .andExpect(jsonPath("$[0].ter_TERNOM", is("Big Supplier")));
    }

    @Test
    void searchFacturas_returnsBadRequestOnDataAccessException() throws Exception {
        when(facturaSearch.searchFactura(any(FacturaSearch.FacturaSearchCriteria.class)))
            .thenThrow(new DataAccessResourceFailureException("Database connection failed"));

        mockMvc.perform(get("/api/fac/search-factura")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Error :")))
            .andExpect(content().string(containsString("Database connection failed")));
    }

    @Test
    void searchFacturas_returnsOkWithEmptyStringFilters() throws Exception {
        FacWithTerProjectionImpl projection = new FacWithTerProjectionImpl();
        projection.facnum = 1000;

        when(facturaSearch.searchFactura(any(FacturaSearch.FacturaSearchCriteria.class)))
            .thenReturn(List.of(projection));

        mockMvc.perform(get("/api/fac/search-factura")
                .param("ent", "1")
                .param("eje", "E1")
                .param("cgecod", "C1")
                .param("main_filter", "")
                .param("estado", "")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk());
    }
}