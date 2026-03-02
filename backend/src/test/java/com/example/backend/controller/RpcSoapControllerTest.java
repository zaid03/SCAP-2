package com.example.backend.controller;

import com.example.backend.config.TestSecurityConfig;
import com.example.backend.config.TestExceptionHandler;
import com.example.backend.dto.RpcResult;
import com.example.backend.service.RpcSoapService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(controllers = RpcSoapController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandler.class})
public class RpcSoapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RpcSoapService rpcSoapService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCallRpcWithPayloadAndReturn200() throws Exception {
        RpcResult fake = mock(RpcResult.class);
        when(rpcSoapService.callRpc(eq("http://e"), eq("op"), eq("ns"), eq("action"), eq("payload"),
                eq("rpc"), eq("encoded"), eq("http://schemas.xmlsoap.org/soap/encoding/"), isNull()))
            .thenReturn(fake);

        Map<String, Object> request = Map.of(
            "endpoint", "http://e",
            "operation", "op",
            "namespace", "ns",
            "soapAction", "action",
            "payload", "payload"
        );

        mockMvc.perform(post("/api/rpc/call")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(rpcSoapService).callRpc(eq("http://e"), eq("op"), eq("ns"), eq("action"),
            eq("payload"), eq("rpc"), eq("encoded"),
            eq("http://schemas.xmlsoap.org/soap/encoding/"), isNull());
    }

    @Test
    void shouldConvertParamsMapToStringMapAndPassToService() throws Exception {
        RpcResult fake = mock(RpcResult.class);
        when(rpcSoapService.callRpc(anyString(), anyString(), anyString(), anyString(),
                any(), anyString(), anyString(), anyString(), isNull()))
             .thenThrow(new RuntimeException("rpc failure"));

        Map<String, Object> paramsMap = Map.of("p1", "one", "p2", 2);
        Map<String, Object> request = Map.of(
            "endpoint", "e",
            "operation", "op",
            "namespace", "ns",
            "paramsMap", paramsMap
        );

        mockMvc.perform(post("/api/rpc/call")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(rpcSoapService).callRpc(eq("e"), eq("op"), eq("ns"), eq(""),
            isNull(), eq("rpc"), eq("encoded"),
            eq("http://schemas.xmlsoap.org/soap/encoding/"), captor.capture());

        Map<String, String> passed = captor.getValue();
        assertThat(passed.get("p1"), equalTo("one"));
        assertThat(passed.get("p2"), equalTo("2")); // numeric converted to string
    }

    @Test
    void shouldReturn500WhenServiceThrows() throws Exception {
        when(rpcSoapService.callRpc(anyString(), anyString(), anyString(), anyString(),
                any(), anyString(), anyString(), anyString(), isNull()))
            .thenThrow(new RuntimeException("rpc failure"));

        Map<String, Object> request = Map.of("endpoint", "e", "operation", "op", "namespace", "ns");

        mockMvc.perform(post("/api/rpc/call")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error", containsString("rpc failure")));

        verify(rpcSoapService).callRpc(eq("e"), eq("op"), eq("ns"), eq(""),
            isNull(), eq("rpc"), eq("encoded"),
            eq("http://schemas.xmlsoap.org/soap/encoding/"), isNull());
    }
}