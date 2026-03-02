package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.config.TestExceptionHandler;
import com.example.backend.dto.PersonaServiceRequest;
import com.example.backend.dto.ServicePersonaRequest;
import com.example.backend.sqlserver2.model.Dep;
import com.example.backend.sqlserver2.model.Dpe;
import com.example.backend.sqlserver2.model.Per;
import com.example.backend.sqlserver2.repository.DepRepository;
import com.example.backend.sqlserver2.repository.DpeRepository;
import com.example.backend.sqlserver2.repository.PerRepository;
import com.example.backend.service.DpePersonasForService;
import com.example.backend.service.DpeService;
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
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DpeController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandler.class})
public class DpeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DpeRepository dpeRepository;

    @MockitoBean
    private PerRepository perRepository;

    @MockitoBean
    private DepRepository depRepository;

    @MockitoBean
    private DpeService dpeService;

    @MockitoBean
    private DpePersonasForService dpePersonasForService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fetchServicePersonas_returnsList() throws Exception {
        Dpe d1 = new Dpe(); d1.setPERCOD("U1");
        Dpe d2 = new Dpe(); d2.setPERCOD("U2");
        when(dpeRepository.findByENTAndEJEAndDEPCOD(1, "E1", "D1")).thenReturn(List.of(d1, d2));

        Per p1 = new Per(); p1.setPERCOD("U1"); p1.setPERNOM("User One");
        Per p2 = new Per(); p2.setPERCOD("U2"); p2.setPERNOM("User Two");
        when(perRepository.findByPERCODIn(List.of("U1", "U2"))).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/api/depe/fetch-service-personas/1/E1/D1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].percod", anyOf(is("U1"), is("U2"))));
    }

    @Test
    void fetchServicePersonas_returns500OnDataAccessException() throws Exception {
        when(dpeRepository.findByENTAndEJEAndDEPCOD(anyInt(), anyString(), anyString()))
            .thenThrow(new DataAccessResourceFailureException("DB down"));

        mockMvc.perform(get("/api/depe/fetch-service-personas/1/E1/D1"))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(containsString("Error:")));
    }

    @Test
    void deletePersonaService_returnsNoContentWhenExists() throws Exception {
        when(dpeRepository.existsById(any())).thenReturn(true);

        mockMvc.perform(delete("/api/depe/delete-persona-service/1/E1/D1/U1"))
            .andDo(print())
            .andExpect(status().isNoContent());

        verify(dpeRepository).deleteById(any());
    }

    @Test
    void deletePersonaService_returnsNotFoundWhenMissing() throws Exception {
        when(dpeRepository.existsById(any())).thenReturn(false);

        mockMvc.perform(delete("/api/depe/delete-persona-service/1/E1/D1/U1"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void fetchPersonaService_returnsDeps() throws Exception {
        Dpe d1 = new Dpe(); d1.setDEPCOD("D1");
        when(dpeRepository.findByENTAndEJEAndPERCOD(1, "E1", "U1")).thenReturn(List.of(d1));

        Dep dep = new Dep(); dep.setDEPCOD("D1"); dep.setDEPDES("Service Desc");
        when(depRepository.findByENTAndEJEAndDEPCODIn(1, "E1", List.of("D1"))).thenReturn(List.of(dep));

        mockMvc.perform(get("/api/depe/fetch-persona-service/1/E1/U1")
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].depcod").value("D1"))
            .andExpect(jsonPath("$[0].depdes").value("Service Desc"));
    }

    @Test
    void fetchPersonaService_returnsNotFoundWhenEmpty() throws Exception {
        when(dpeRepository.findByENTAndEJEAndPERCOD(1, "E1", "U2")).thenReturn(List.of());

        mockMvc.perform(get("/api/depe/fetch-persona-service/1/E1/U2"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
     void addPersonaServices_validRequest_returnsCreated() throws Exception {
         PersonaServiceRequest req = new PersonaServiceRequest();
         req.setPercod("U1");
         req.setServices(List.of("S1", "S2"));
 
         mockMvc.perform(post("/api/depe/add-persona-services")
                 .contentType(MediaType.APPLICATION_JSON)
                 .content(objectMapper.writeValueAsString(req)))
             .andDo(print())
             .andExpect(status().isCreated());
 
        ArgumentCaptor<PersonaServiceRequest> cap = ArgumentCaptor.forClass(PersonaServiceRequest.class);
        verify(dpeService).savePersonaServices(cap.capture());
        PersonaServiceRequest passed = cap.getValue();
        assertEquals(req.getPercod(), passed.getPercod());
        assertEquals(req.getServices(), passed.getServices());
    }

    @Test
    void addPersonaServices_validationFails() throws Exception {
        PersonaServiceRequest missingUser = new PersonaServiceRequest();
        missingUser.setServices(List.of("S1"));
        mockMvc.perform(post("/api/depe/add-persona-services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(missingUser)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Falta un dato obligatorio")));

        PersonaServiceRequest missingServices = new PersonaServiceRequest();
        missingServices.setPercod("U1");
        mockMvc.perform(post("/api/depe/add-persona-services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(missingServices)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Debe seleccionar al menos un servicio.")));
    }

    @Test
    void deleteAllPersonaServices_returnsNoContentWhenDeleted() throws Exception {
        when(dpeRepository.findByENTAndEJEAndPERCOD(1, "E1", "U1")).thenReturn(List.of(new Dpe()));
        when(dpeRepository.deleteByENTAndEJEAndPERCOD(1, "E1", "U1")).thenReturn(1);

        mockMvc.perform(delete("/api/depe/delete-persona-Allservice/1/E1/U1"))
            .andDo(print())
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteAllPersonaServices_returnsNotFoundWhenMissing() throws Exception {
        when(dpeRepository.findByENTAndEJEAndPERCOD(1, "E1", "U1")).thenReturn(List.of());

        mockMvc.perform(delete("/api/depe/delete-persona-Allservice/1/E1/U1"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("Sin resultado"));
    }

    @Test
    void addServicesPersona_validationAndSuccess() throws Exception {
        ServicePersonaRequest req = new ServicePersonaRequest();
        req.setDepcod("S1");
        req.setPersonas(List.of("U1", "U2"));

        mockMvc.perform(post("/api/depe/add-services-persona")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andDo(print())
            .andExpect(status().isCreated());

       ArgumentCaptor<ServicePersonaRequest> cap = ArgumentCaptor.forClass(ServicePersonaRequest.class);
       verify(dpePersonasForService).saveServicePersonas(cap.capture());
       ServicePersonaRequest passed = cap.getValue();
       assertEquals(req.getDepcod(), passed.getDepcod());
       assertEquals(req.getPersonas(), passed.getPersonas());
    }

    @Test
    void addServicesPersona_validationFails() throws Exception {
        ServicePersonaRequest missingDep = new ServicePersonaRequest();
        missingDep.setPersonas(List.of("U1"));
        mockMvc.perform(post("/api/depe/add-services-persona")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(missingDep)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("El código de servicio es obligatorio.")));

        ServicePersonaRequest missingPersons = new ServicePersonaRequest();
        missingPersons.setDepcod("S1");
        mockMvc.perform(post("/api/depe/add-services-persona")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(missingPersons)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Debe seleccionar al menos un persona.")));
    }

    @Test
    void endpoints_return500OnDataAccessException() throws Exception {
        when(dpeRepository.findByENTAndEJEAndDEPCOD(anyInt(), anyString(), anyString()))
            .thenThrow(new DataAccessResourceFailureException("boom"));

        mockMvc.perform(get("/api/depe/fetch-service-personas/1/E1/D1"))
            .andDo(print())
            .andExpect(status().isInternalServerError());
    }
}