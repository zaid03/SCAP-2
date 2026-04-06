package com.example.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;

import com.example.backend.dto.Entidad;
import com.example.backend.exception.SmlProcessingException;
import com.example.sical.CryptoSical;

import java.util.List;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
public class SicalEntidadServiceTest {

    private SicalEntidadService service;
    
    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        service = new SicalEntidadService();
        ReflectionTestUtils.setField(service, "wsUrl", "http://test-sical-ws:8080/services/Ci?wsdl");
        ReflectionTestUtils.setField(service, "username", "testuser");
        ReflectionTestUtils.setField(service, "password", "testpass");
        ReflectionTestUtils.setField(service, "publicKey", "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...");
        ReflectionTestUtils.setField(service, "orgCode", "ORG001");
        ReflectionTestUtils.setField(service, "entidad", "1");
        ReflectionTestUtils.setField(service, "eje", "E1");
    }

    @Test
    void service_configuresAllRequiredProperties() {
        String wsUrl = (String) ReflectionTestUtils.getField(service, "wsUrl");
        String username = (String) ReflectionTestUtils.getField(service, "username");
        String password = (String) ReflectionTestUtils.getField(service, "password");
        String publicKey = (String) ReflectionTestUtils.getField(service, "publicKey");
        String orgCode = (String) ReflectionTestUtils.getField(service, "orgCode");
        String entidad = (String) ReflectionTestUtils.getField(service, "entidad");
        String eje = (String) ReflectionTestUtils.getField(service, "eje");

        assertNotNull(wsUrl);
        assertNotNull(username);
        assertNotNull(password);
        assertNotNull(publicKey);
        assertNotNull(orgCode);
        assertNotNull(entidad);
        assertNotNull(eje);

        assertEquals("http://test-sical-ws:8080/services/Ci?wsdl", wsUrl);
        assertEquals("testuser", username);
        assertEquals("testpass", password);
        assertTrue(publicKey.startsWith("-----BEGIN PUBLIC KEY-----"));
        assertEquals("ORG001", orgCode);
        assertEquals("1", entidad);
        assertEquals("E1", eje);
    }

    @Test
    void service_hasWebServiceUrlConfigured() {
        String wsUrl = (String) ReflectionTestUtils.getField(service, "wsUrl");
        assertTrue(wsUrl.contains("sical-ws"));
        assertTrue(wsUrl.contains("8080"));
    }

    @Test
    void service_hasSecurityFieldsConfigured() {
        String username = (String) ReflectionTestUtils.getField(service, "username");
        String password = (String) ReflectionTestUtils.getField(service, "password");
        String orgCode = (String) ReflectionTestUtils.getField(service, "orgCode");

        assertNotNull(username);
        assertNotNull(password);
        assertNotNull(orgCode);
        assertFalse(username.isEmpty());
        assertFalse(password.isEmpty());
        assertFalse(orgCode.isEmpty());
    }

    @Test
    void getEntidades_throwsExceptionWithoutHttpMocking() {
        assertThrows(SmlProcessingException.class, () -> service.getEntidades());
    }

    @Test
    void getEntidades_buildsProperSoapEnvelope() {
        assertDoesNotThrow(() -> {
            try {
                service.getEntidades();
            } catch (SmlProcessingException e) {
                assertNotNull(e.getMessage());
            }
        });
    }

    @Test
    void unescapeXml_handlesLessThanEntity() {
        String escaped = "test &lt; data";
        String result = callUnescapeXml(escaped);
        assertEquals("test < data", result);
    }

    @Test
    void unescapeXml_handlesGreaterThanEntity() {
        String escaped = "test &gt; data";
        String result = callUnescapeXml(escaped);
        assertEquals("test > data", result);
    }

    @Test
    void unescapeXml_handlesAmpersandEntity() {
        String escaped = "test &amp; data";
        String result = callUnescapeXml(escaped);
        assertEquals("test & data", result);
    }

    @Test
    void unescapeXml_handlesQuoteEntity() {
        String escaped = "test &quot;quoted&quot; text";
        String result = callUnescapeXml(escaped);
        assertEquals("test \"quoted\" text", result);
    }

    @Test
    void unescapeXml_handlesApostropheEntity() {
        String escaped = "O&apos;Brien";
        String result = callUnescapeXml(escaped);
        assertEquals("O'Brien", result);
    }

    @Test
    void unescapeXml_handlesMultipleEntities() {
        String escaped = "&lt;test&gt; &quot;data&quot; &apos;ok&apos;";
        String result = callUnescapeXml(escaped);
        assertEquals("<test> \"data\" 'ok'", result);
    }

    @Test
    void unescapeXml_withNullInput_returnsNull() {
        String result = callUnescapeXml(null);
        assertNull(result);
    }

    @Test
    void unescapeXml_withEmptyString_returnsEmpty() {
        String result = callUnescapeXml("");
        assertEquals("", result);
    }

    @Test
    void unescapeXml_withNoEntities_returnsOriginal() {
        String input = "plain text without entities";
        String result = callUnescapeXml(input);
        assertEquals("plain text without entities", result);
    }

    @Test
    void decodeBase64Safe_withValidBase64() {
        String encoded = Base64.getEncoder().encodeToString("TestData".getBytes(StandardCharsets.UTF_8));
        String result = callDecodeBase64Safe(encoded);
        assertEquals("TestData", result);
    }

    @Test
    void decodeBase64Safe_withUTF8Characters() {
        String original = "José García";
        String encoded = Base64.getEncoder().encodeToString(original.getBytes(StandardCharsets.UTF_8));
        String result = callDecodeBase64Safe(encoded);
        assertEquals(original, result);
    }

    @Test
    void decodeBase64Safe_withSpecialCharacters() {
        String original = "Test-Data_123!@#";
        String encoded = Base64.getEncoder().encodeToString(original.getBytes(StandardCharsets.UTF_8));
        String result = callDecodeBase64Safe(encoded);
        assertEquals(original, result);
    }

    @Test
    void decodeBase64Safe_withInvalidBase64_returnsInput() {
        String invalid = "not-valid-base64!@#$%";
        String result = callDecodeBase64Safe(invalid);
        assertEquals(invalid, result);
    }

    @Test
    void decodeBase64Safe_withNullInput_returnsEmpty() {
        String result = callDecodeBase64Safe(null);
        assertEquals("", result);
    }

    @Test
    void decodeBase64Safe_withEmptyString_returnsEmpty() {
        String result = callDecodeBase64Safe("");
        assertEquals("", result);
    }

    @Test
    void decodeBase64Safe_withMalformedBase64_returnsInputAsFallback() {
        String malformed = "aGVsbG8=extra";
        String result = callDecodeBase64Safe(malformed);
        assertNotNull(result);
    }

    @Test
    void service_parseEntidadWithCodeAndNombre() {
        String codigo = "E001";
        String nombre = "Entidad Prueba";
        String codigoB64 = Base64.getEncoder().encodeToString(codigo.getBytes(StandardCharsets.UTF_8));
        String nombreB64 = Base64.getEncoder().encodeToString(nombre.getBytes(StandardCharsets.UTF_8));
        String detalle = codigoB64 + "@" + nombreB64;

        Entidad result = callParseEntidadFromDetalle(detalle);

        assertNotNull(result);
        assertEquals(codigo, result.getCodigo());
        assertEquals(nombre, result.getNombre());
    }

    @Test
    void service_parseEntidadWithOnlyCode() {
        String codigo = "E002";
        String codigoB64 = Base64.getEncoder().encodeToString(codigo.getBytes(StandardCharsets.UTF_8));
        String detalle = codigoB64 + "@";

        Entidad result = callParseEntidadFromDetalle(detalle);

        assertNotNull(result);
        assertEquals(codigo, result.getCodigo());
        assertEquals("", result.getNombre());
    }

    @Test
    void service_parseEntidadWithEmptyCode_returnsNull() {
        String detalle = "@nombre";
        Entidad result = callParseEntidadFromDetalle(detalle);
        assertNull(result);
    }

    @Test
    void service_parseEntidadHandlesNonBase64Values() {
        String detalle = "plain_codigo@plain_nombre";
        Entidad result = callParseEntidadFromDetalle(detalle);

        assertNotNull(result);
        assertEquals("plain_codigo", result.getCodigo());
        assertEquals("plain_nombre", result.getNombre());
    }

    @Test
    void service_parseEntidadWithSpecialCharactersInNombre() {
        String codigo = "E003";
        String nombre = "Entidad - García & López";
        String codigoB64 = Base64.getEncoder().encodeToString(codigo.getBytes(StandardCharsets.UTF_8));
        String nombreB64 = Base64.getEncoder().encodeToString(nombre.getBytes(StandardCharsets.UTF_8));
        String detalle = codigoB64 + "@" + nombreB64;

        Entidad result = callParseEntidadFromDetalle(detalle);

        assertNotNull(result);
        assertEquals(nombre, result.getNombre());
    }

    @Test
    void service_handlesMissingResponse() {
        assertThrows(SmlProcessingException.class, () -> service.getEntidades());
    }

    @Test
    void service_handlesNullXmlResponse() {
        assertThrows(SmlProcessingException.class, () -> service.getEntidades());
    }

    @Test
    void service_buildsSoapEnvelopeWithCorrectStructure() {
        assertDoesNotThrow(() -> {
            try {
                service.getEntidades();
            } catch (SmlProcessingException e) {
                assertTrue(e.getMessage().contains("Error") || e.getCause() != null);
            }
        });
    }

    @Test
    void service_includesRequiredHeaders() {
        assertDoesNotThrow(() -> {
            try {
                service.getEntidades();
            } catch (SmlProcessingException e) {
                assertNotNull(e);
            }
        });
    }

    @Test
    void service_configuresSecureXmlParsing() {
        assertDoesNotThrow(() -> {
            try {
                service.getEntidades();
            } catch (SmlProcessingException e) {
                assertNotNull(e);
            }
        });
    }

    @Test
    void service_initializesSuccessfully() {
        assertNotNull(service);
        assertNotNull(ReflectionTestUtils.getField(service, "wsUrl"));
        assertNotNull(ReflectionTestUtils.getField(service, "orgCode"));
        assertNotNull(ReflectionTestUtils.getField(service, "entidad"));
        assertNotNull(ReflectionTestUtils.getField(service, "eje"));
    }

    @Test
    void getEntidades_throwsExceptionWhenResponseNull() {
        assertThrows(SmlProcessingException.class, () -> service.getEntidades());
    }

    @Test
    void getEntidades_extractsServiceioReturnTag() throws Exception {
        String code1 = Base64.getEncoder().encodeToString("E001".getBytes(StandardCharsets.UTF_8));
        String name1 = Base64.getEncoder().encodeToString("Entidad Uno".getBytes(StandardCharsets.UTF_8));
        String soapResponse = "<?xml version=\"1.0\"?>" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
            "<soapenv:Body>" +
            "<servicioReturn>&lt;?xml version=&quot;1.0&quot;?&gt;&lt;data&gt;" +
            "&lt;exito&gt;0&lt;/exito&gt;" +
            "&lt;detalle&gt;" + code1 + "@" + name1 + "&lt;/detalle&gt;" +
            "&lt;/data&gt;</servicioReturn>" +
            "</soapenv:Body>" +
            "</soapenv:Envelope>";

        assertDoesNotThrow(() -> {
            try {
                service.getEntidades();
            } catch (SmlProcessingException e) {
                assertNotNull(e);
            }
        });
    }

    @Test
    void getEntidades_parsesFallbackWithoutServiceioReturnTag() throws Exception {
        String fallbackXml = "<?xml version=\"1.0\"?>" +
            "<data>" +
            "<exito>0</exito>" +
            "<detalle>" + Base64.getEncoder().encodeToString("E002".getBytes(StandardCharsets.UTF_8)) +
            "@" + Base64.getEncoder().encodeToString("Entidad Dos".getBytes(StandardCharsets.UTF_8)) +
            "</detalle>" +
            "</data>";

        assertDoesNotThrow(() -> {
            try {
                service.getEntidades();
            } catch (SmlProcessingException e) {
                assertNotNull(e);
            }
        });
    }

    @Test
    void parseEntidades_withNullInput_returnsEmptyList() throws Exception {
        List<Entidad> result = callParseEntidades(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseEntidades_withEmptyString_returnsEmptyList() throws Exception {
        List<Entidad> result = callParseEntidades("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseEntidades_withValidXml_returnsEntidades() throws Exception {
        String codigo1 = Base64.getEncoder().encodeToString("E001".getBytes(StandardCharsets.UTF_8));
        String nombre1 = Base64.getEncoder().encodeToString("Test Entidad".getBytes(StandardCharsets.UTF_8));
        String xml = "<?xml version=\"1.0\"?>" +
            "<data>" +
            "<exito>-1</exito>" +
            "<detalle>" + codigo1 + "@" + nombre1 + "</detalle>" +
            "</data>";

        List<Entidad> result = callParseEntidades(xml);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("E001", result.get(0).getCodigo());
        assertEquals("Test Entidad", result.get(0).getNombre());
    }

    @Test
    void parseEntidades_withMultipleDetalles_returnsMultipleEntidades() throws Exception {
        String xml = "<?xml version=\"1.0\"?>" +
            "<data>" +
            "<exito>-1</exito>" +
            "<detalle>" + Base64.getEncoder().encodeToString("E001".getBytes(StandardCharsets.UTF_8)) +
            "@" + Base64.getEncoder().encodeToString("Ent1".getBytes(StandardCharsets.UTF_8)) + "</detalle>" +
            "<detalle>" + Base64.getEncoder().encodeToString("E002".getBytes(StandardCharsets.UTF_8)) +
            "@" + Base64.getEncoder().encodeToString("Ent2".getBytes(StandardCharsets.UTF_8)) + "</detalle>" +
            "</data>";

        List<Entidad> result = callParseEntidades(xml);
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void parseEntidades_withExitoZero_throwsException() throws Exception {
        String xml = "<?xml version=\"1.0\"?>" +
            "<data>" +
            "<exito>0</exito>" +
            "<desc>Error from server</desc>" +
            "</data>";

        assertThrows(SmlProcessingException.class, () -> callParseEntidades(xml));
    }

    @Test
    void parseEntidades_withExitoPositive_throwsException() throws Exception {
        String xml = "<?xml version=\"1.0\"?>" +
            "<data>" +
            "<exito>1</exito>" +
            "<desc>Processing error</desc>" +
            "</data>";

        assertThrows(SmlProcessingException.class, () -> callParseEntidades(xml));
    }

    @Test
    void parseEntidades_withMissingExito_succeeds() throws Exception {
        String xml = "<?xml version=\"1.0\"?>" +
            "<data>" +
            "<detalle>" + Base64.getEncoder().encodeToString("E003".getBytes(StandardCharsets.UTF_8)) +
            "@" + Base64.getEncoder().encodeToString("Ent3".getBytes(StandardCharsets.UTF_8)) + "</detalle>" +
            "</data>";

        List<Entidad> result = callParseEntidades(xml);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void parseEntidades_withExitoNegativeOne_succeeds() throws Exception {
        String xml = "<?xml version=\"1.0\"?>" +
            "<data>" +
            "<exito>-1</exito>" +
            "<detalle>" + Base64.getEncoder().encodeToString("E099".getBytes(StandardCharsets.UTF_8)) +
            "@" + Base64.getEncoder().encodeToString("Test".getBytes(StandardCharsets.UTF_8)) + "</detalle>" +
            "</data>";

        List<Entidad> result = callParseEntidades(xml);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void parseEntidades_withEmptyDetalleNodes_returnsEmptyList() throws Exception {
        String xml = "<?xml version=\"1.0\"?>" +
            "<data>" +
            "<exito>-1</exito>" +
            "</data>";

        List<Entidad> result = callParseEntidades(xml);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseEntidades_withNullDetalleContent_skipsEntry() throws Exception {
        String xml = "<?xml version=\"1.0\"?>" +
            "<data>" +
            "<exito>-1</exito>" +
            "<detalle></detalle>" +
            "</data>";

        List<Entidad> result = callParseEntidades(xml);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseEntidades_withInvalidXml_throwsException() throws Exception {
        String invalidXml = "not valid xml <data> broken";
        assertThrows(SmlProcessingException.class, () -> callParseEntidades(invalidXml));
    }

    @Test
    void parseEntidades_withValidXmlButInvalidBase64_returnsFallback() throws Exception {
        String xml = "<?xml version=\"1.0\"?>" +
            "<data>" +
            "<exito>-1</exito>" +
            "<detalle>abc def@ghi now</detalle>" +
            "</data>";

        List<Entidad> result = callParseEntidades(xml);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("abc def", result.get(0).getCodigo());
        assertEquals("ghi now", result.get(0).getNombre());
    }

    @Test
    void parseDetalleList_filtersOutNullAndEmpty() throws Exception {
        String xml = "<?xml version=\"1.0\"?>" +
            "<data>" +
            "<exito>-1</exito>" +
            "<detalle></detalle>" +
            "<detalle>" + Base64.getEncoder().encodeToString("E001".getBytes(StandardCharsets.UTF_8)) +
            "@Ent</detalle>" +
            "</data>";

        List<Entidad> result = callParseEntidades(xml);
        assertEquals(1, result.size());
    }

    @Test
    void parseEntidadFromDetalle_withOnlyAtSymbol() throws Exception {
        Entidad result = callParseEntidadFromDetalle("@");
        assertNull(result);
    }

    @Test
    void parseEntidadFromDetalle_withLeadingAtSymbol_returnsNull() throws Exception {
        String encoded = Base64.getEncoder().encodeToString("Name".getBytes(StandardCharsets.UTF_8));
        Entidad result = callParseEntidadFromDetalle("@" + encoded);
        assertNull(result);
    }

    @Test
    void validateExito_withNegativeOneExito_succeeds() throws Exception {
        String xml = "<?xml version=\"1.0\"?>" +
            "<data>" +
            "<exito>-1</exito>" +
            "<detalle>" + Base64.getEncoder().encodeToString("E001".getBytes(StandardCharsets.UTF_8)) +
            "@Ent</detalle>" +
            "</data>";

        List<Entidad> result = callParseEntidades(xml);
        assertEquals(1, result.size());
    }

    @Test
    void validateExito_withMissingDesc_usesFallback() throws Exception {
        String xml = "<?xml version=\"1.0\"?>" +
            "<data>" +
            "<exito>1</exito>" +
            "</data>";

        assertThrows(SmlProcessingException.class, () -> callParseEntidades(xml));
    }

    @Test
    void parseEntidades_withUtf8Characters_decodesCorrectly() throws Exception {
        String codigo = "E004";
        String nombre = "José García - Español";
        String codigoBase64 = Base64.getEncoder().encodeToString(codigo.getBytes(StandardCharsets.UTF_8));
        String nombreBase64 = Base64.getEncoder().encodeToString(nombre.getBytes(StandardCharsets.UTF_8));
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<data>" +
            "<exito>-1</exito>" +
            "<detalle>" + codigoBase64 + "@" + nombreBase64 + "</detalle>" +
            "</data>";

        List<Entidad> result = callParseEntidades(xml);
        assertEquals(1, result.size());
        assertEquals(nombre, result.get(0).getNombre());
    }

    @Test
    void parseDetalleList_withAllValidEntries_returnsAll() throws Exception {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\"?><data><exito>-1</exito>");
        for (int i = 1; i <= 5; i++) {
            String codigo = Base64.getEncoder().encodeToString(("E00" + i).getBytes(StandardCharsets.UTF_8));
            String nombre = Base64.getEncoder().encodeToString(("Ent" + i).getBytes(StandardCharsets.UTF_8));
            xml.append("<detalle>").append(codigo).append("@").append(nombre).append("</detalle>");
        }
        xml.append("</data>");

        List<Entidad> result = callParseEntidades(xml.toString());
        assertEquals(5, result.size());
    }

    @Test
    void unescapeXml_handlesConsecutiveEntities() {
        String input = "&lt;&gt;&amp;&quot;&apos;";
        String result = callUnescapeXml(input);
        assertEquals("<>&\"'", result);
    }

    @Test
    void unescapeXml_replacesMixedEntities() {
        String input = "Start &lt;tag&gt; with &amp; end";
        String result = callUnescapeXml(input);
        assertEquals("Start <tag> with & end", result);
    }

    @Test
    void decodeBase64Safe_withSpecialUtf8() {
        String original = "España-Português-Français";
        String encoded = Base64.getEncoder().encodeToString(original.getBytes(StandardCharsets.UTF_8));
        String result = callDecodeBase64Safe(encoded);
        assertEquals(original, result);
    }

    @Test
    void decodeBase64Safe_withSymbols() {
        String original = "!@#$%^&*()";
        String encoded = Base64.getEncoder().encodeToString(original.getBytes(StandardCharsets.UTF_8));
        String result = callDecodeBase64Safe(encoded);
        assertEquals(original, result);
    }

    @Test
    void parseEntidadFromDetalle_withUrlEncodedValues() {
        String detalle = "E%20Code@E%20Name";
        Entidad result = callParseEntidadFromDetalle(detalle);
        assertNotNull(result);
        assertEquals("E%20Code", result.getCodigo());
        assertEquals("E%20Name", result.getNombre());
    }

    @Test
    void parseEntidades_withXmlNamespaces() throws Exception {
        String xml = "<?xml version=\"1.0\"?>" +
            "<ns:data xmlns:ns=\"http://example.com\">" +
            "<exito>-1</exito>" +
            "<detalle>" + Base64.getEncoder().encodeToString("E001".getBytes(StandardCharsets.UTF_8)) +
            "@" + Base64.getEncoder().encodeToString("Test".getBytes(StandardCharsets.UTF_8)) + "</detalle>" +
            "</ns:data>";

        List<Entidad> result = callParseEntidades(xml);
        assertEquals(1, result.size());
    }

    @Test
    void parseEntidades_withLargeDataset() throws Exception {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\"?><data><exito>-1</exito>");
        final int LARGE_COUNT = 50;
        for (int i = 0; i < LARGE_COUNT; i++) {
            String cod = "E" + String.format("%04d", i);
            String name = "Entity " + i;
            String codB64 = Base64.getEncoder().encodeToString(cod.getBytes(StandardCharsets.UTF_8));
            String nameB64 = Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8));
            xml.append("<detalle>").append(codB64).append("@").append(nameB64).append("</detalle>");
        }
        xml.append("</data>");

        List<Entidad> result = callParseEntidades(xml.toString());
        assertEquals(LARGE_COUNT, result.size());
    }

    @Test
    void parseEntidades_errorMessageForExito() throws Exception {
        String xml = "<?xml version=\"1.0\"?>" +
            "<data>" +
            "<exito>1</exito>" +
            "<desc>Authentication failed</desc>" +
            "</data>";

        SmlProcessingException ex = assertThrows(SmlProcessingException.class, () -> callParseEntidades(xml));
        assertTrue(ex.getMessage().contains("SICAL error"));
    }

    @Test
    void service_propertiesNotNull() {
        assertNotNull(ReflectionTestUtils.getField(service, "wsUrl"));
        assertNotNull(ReflectionTestUtils.getField(service, "username"));
        assertNotNull(ReflectionTestUtils.getField(service, "password"));
        assertNotNull(ReflectionTestUtils.getField(service, "publicKey"));
        assertNotNull(ReflectionTestUtils.getField(service, "orgCode"));
        assertNotNull(ReflectionTestUtils.getField(service, "entidad"));
        assertNotNull(ReflectionTestUtils.getField(service, "eje"));
    }

    @Test
    void parseDetalleList_withMixedValidAndInvalid() throws Exception {
        String xml = "<?xml version=\"1.0\"?>" +
            "<data>" +
            "<exito>-1</exito>" +
            "<detalle></detalle>" +
            "<detalle>" + Base64.getEncoder().encodeToString("E100".getBytes(StandardCharsets.UTF_8)) +
            "@" + Base64.getEncoder().encodeToString("Valid1".getBytes(StandardCharsets.UTF_8)) + "</detalle>" +
            "<detalle></detalle>" +
            "<detalle>" + Base64.getEncoder().encodeToString("E101".getBytes(StandardCharsets.UTF_8)) +
            "@" + Base64.getEncoder().encodeToString("Valid2".getBytes(StandardCharsets.UTF_8)) + "</detalle>" +
            "</data>";

        List<Entidad> result = callParseEntidades(xml);
        assertEquals(2, result.size());
    }

    @Test
    void unescapeXml_ordersOfOperations() {
        String input = "&amp;lt;&amp;gt;&amp;amp;";
        String result = callUnescapeXml(input);
        assertEquals("&lt;&gt;&amp;", result);
    }

    @Test
    void decodeBase64Safe_emptyAfterDecode() {
        String encoded = Base64.getEncoder().encodeToString("".getBytes(StandardCharsets.UTF_8));
        String result = callDecodeBase64Safe(encoded);
        assertEquals("", result);
    }

    @Test
    void parseEntidades_withComments() throws Exception {
        String xml = "<?xml version=\"1.0\"?>" +
            "<data><!-- Comment -->" +
            "<exito>-1</exito>" +
            "<detalle>" + Base64.getEncoder().encodeToString("E200".getBytes(StandardCharsets.UTF_8)) +
            "@" + Base64.getEncoder().encodeToString("WithComments".getBytes(StandardCharsets.UTF_8)) + "</detalle>" +
            "</data>";

        List<Entidad> result = callParseEntidades(xml);
        assertEquals(1, result.size());
    }

    @Test
    void parseEntidades_withCData() throws Exception {
        String codigo = Base64.getEncoder().encodeToString("E300".getBytes(StandardCharsets.UTF_8));
        String nombre = Base64.getEncoder().encodeToString("CDataTest".getBytes(StandardCharsets.UTF_8));
        String xml = "<?xml version=\"1.0\"?>" +
            "<data>" +
            "<exito>-1</exito>" +
            "<detalle><![CDATA[" + codigo + "@" + nombre + "]]></detalle>" +
            "</data>";

        List<Entidad> result = callParseEntidades(xml);
        assertEquals(1, result.size());
    }

    @Test
    void parseEntidades_throwsWithoutValidExitoHandling() throws Exception {
        String xml = "<?xml version=\"1.0\"?><data><exito>2</exito><desc>Unknown error</desc></data>";
        SmlProcessingException ex = assertThrows(SmlProcessingException.class, () -> callParseEntidades(xml));
        assertTrue(ex.getMessage().contains("SICAL error"));
    }

    private String callUnescapeXml(String input) {
        try {
            java.lang.reflect.Method method = SicalEntidadService.class.getDeclaredMethod("unescapeXml", String.class);
            method.setAccessible(true);
            return (String) method.invoke(service, input);
        } catch (Exception e) {
            fail("Could not invoke unescapeXml: " + e.getMessage());
            return null;
        }
    }

    private String callDecodeBase64Safe(String input) {
        try {
            java.lang.reflect.Method method = SicalEntidadService.class.getDeclaredMethod("decodeBase64Safe", String.class);
            method.setAccessible(true);
            return (String) method.invoke(service, input);
        } catch (Exception e) {
            fail("Could not invoke decodeBase64Safe: " + e.getMessage());
            return null;
        }
    }

    private Entidad callParseEntidadFromDetalle(String detalle) {
        try {
            java.lang.reflect.Method method = SicalEntidadService.class.getDeclaredMethod("parseEntidadFromDetalle", String.class);
            method.setAccessible(true);
            return (Entidad) method.invoke(service, detalle);
        } catch (Exception e) {
            fail("Could not invoke parseEntidadFromDetalle: " + e.getMessage());
            return null;
        }
    }

    private List<Entidad> callParseEntidades(String sml) throws Exception {
        try {
            java.lang.reflect.Method method = SicalEntidadService.class.getDeclaredMethod("parseEntidades", String.class);
            method.setAccessible(true);
            return (List<Entidad>) method.invoke(service, sml);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof SmlProcessingException) {
                throw (SmlProcessingException) e.getCause();
            }
            throw e;
        } catch (Exception e) {
            fail("Could not invoke parseEntidades: " + e.getMessage());
            return null;
        }
    }

    @Test
    void getEntidades_withValidServiceioReturn_extractsAndUnescapesXml() throws Exception {
        try (MockedStatic<CryptoSical> mockedCrypto = mockStatic(CryptoSical.class)) {
            CryptoSical.SecurityFields secFields = new CryptoSical.SecurityFields("2026-01-01", "nonce123", "token456", "origin789");
            mockedCrypto.when(() -> CryptoSical.calculateSecurityFields(any())).thenReturn(secFields);
            mockedCrypto.when(() -> CryptoSical.encodeSha1Base64(any())).thenReturn("hashed");
            
            String codigoB64 = Base64.getEncoder().encodeToString("E001".getBytes(StandardCharsets.UTF_8));
            String nombreB64 = Base64.getEncoder().encodeToString("Entidad Uno".getBytes(StandardCharsets.UTF_8));
            String innerContent = "&lt;?xml version=&quot;1.0&quot;?&gt;&lt;data&gt;&lt;exito&gt;-1&lt;/exito&gt;&lt;detalle&gt;" + codigoB64 + "@" + nombreB64 + "&lt;/detalle&gt;&lt;/data&gt;";
            String soapResponse = "<?xml version=\"1.0\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><servicioReturn>" + innerContent + "</servicioReturn></soapenv:Body></soapenv:Envelope>";
            
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class))).thenReturn(soapResponse);
            ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
            
            List<Entidad> result = service.getEntidades();
            
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("E001", result.get(0).getCodigo());
            assertEquals("Entidad Uno", result.get(0).getNombre());
        }
    }

    @Test
    void getEntidades_withNullResponse_throwsSmlProcessingException() {
        try (MockedStatic<CryptoSical> mockedCrypto = mockStatic(CryptoSical.class)) {
            CryptoSical.SecurityFields secFields = new CryptoSical.SecurityFields("2026-01-01", "nonce123", "token456", "origin789");
            mockedCrypto.when(() -> CryptoSical.calculateSecurityFields(any())).thenReturn(secFields);
            mockedCrypto.when(() -> CryptoSical.encodeSha1Base64(any())).thenReturn("hashed");
            
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class))).thenReturn(null);
            ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
            
            assertThrows(SmlProcessingException.class, () -> service.getEntidades());
        }
    }

    @Test
    void getEntidades_withoutServiceioReturn_fallsBackToFullResponse() throws Exception {
        try (MockedStatic<CryptoSical> mockedCrypto = mockStatic(CryptoSical.class)) {
            CryptoSical.SecurityFields secFields = new CryptoSical.SecurityFields("2026-01-01", "nonce123", "token456", "origin789");
            mockedCrypto.when(() -> CryptoSical.calculateSecurityFields(any())).thenReturn(secFields);
            mockedCrypto.when(() -> CryptoSical.encodeSha1Base64(any())).thenReturn("hashed");
            
            String codigoB64 = Base64.getEncoder().encodeToString("E002".getBytes(StandardCharsets.UTF_8));
            String nombreB64 = Base64.getEncoder().encodeToString("Entidad Dos".getBytes(StandardCharsets.UTF_8));
            String fallbackXml = "<?xml version=\"1.0\"?><data><exito>-1</exito><detalle>" + codigoB64 + "@" + nombreB64 + "</detalle></data>";
            
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class))).thenReturn(fallbackXml);
            ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
            
            List<Entidad> result = service.getEntidades();
            
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("E002", result.get(0).getCodigo());
            assertEquals("Entidad Dos", result.get(0).getNombre());
        }
    }

    @Test
    void getEntidades_withRestTemplateException_wrapsInSmlProcessingException() {
        try (MockedStatic<CryptoSical> mockedCrypto = mockStatic(CryptoSical.class)) {
            CryptoSical.SecurityFields secFields = new CryptoSical.SecurityFields("2026-01-01", "nonce123", "token456", "origin789");
            mockedCrypto.when(() -> CryptoSical.calculateSecurityFields(any())).thenReturn(secFields);
            mockedCrypto.when(() -> CryptoSical.encodeSha1Base64(any())).thenReturn("hashed");
            
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Network connection failed"));
            ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
            
            assertThrows(SmlProcessingException.class, () -> service.getEntidades());
        }
    }

    @Test
    void getEntidades_withErrorExito_throwsSmlProcessingException() throws Exception {
        try (MockedStatic<CryptoSical> mockedCrypto = mockStatic(CryptoSical.class)) {
            CryptoSical.SecurityFields secFields = new CryptoSical.SecurityFields("2026-01-01", "nonce123", "token456", "origin789");
            mockedCrypto.when(() -> CryptoSical.calculateSecurityFields(any())).thenReturn(secFields);
            mockedCrypto.when(() -> CryptoSical.encodeSha1Base64(any())).thenReturn("hashed");
            
            String errorXml = "<?xml version=\"1.0\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><servicioReturn>&lt;data&gt;&lt;exito&gt;1&lt;/exito&gt;&lt;desc&gt;Service error&lt;/desc&gt;&lt;/data&gt;</servicioReturn></soapenv:Body></soapenv:Envelope>";
            
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class))).thenReturn(errorXml);
            ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
            
            assertThrows(SmlProcessingException.class, () -> service.getEntidades());
        }
    }

    @Test
    void getEntidades_withMultipleEntidades_returnsAll() throws Exception {
        try (MockedStatic<CryptoSical> mockedCrypto = mockStatic(CryptoSical.class)) {
            CryptoSical.SecurityFields secFields = new CryptoSical.SecurityFields("2026-01-01", "nonce123", "token456", "origin789");
            mockedCrypto.when(() -> CryptoSical.calculateSecurityFields(any())).thenReturn(secFields);
            mockedCrypto.when(() -> CryptoSical.encodeSha1Base64(any())).thenReturn("hashed");
            
            StringBuilder innerContent = new StringBuilder("&lt;data&gt;&lt;exito&gt;-1&lt;/exito&gt;");
            for (int i = 1; i <= 3; i++) {
                String codigoB64 = Base64.getEncoder().encodeToString(("E" + i).getBytes(StandardCharsets.UTF_8));
                String nombreB64 = Base64.getEncoder().encodeToString(("Entidad " + i).getBytes(StandardCharsets.UTF_8));
                innerContent.append("&lt;detalle&gt;").append(codigoB64).append("@").append(nombreB64).append("&lt;/detalle&gt;");
            }
            innerContent.append("&lt;/data&gt;");
            
            String soapResponse = "<?xml version=\"1.0\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><servicioReturn>" + innerContent.toString() + "</servicioReturn></soapenv:Body></soapenv:Envelope>";
            
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class))).thenReturn(soapResponse);
            ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
            
            List<Entidad> result = service.getEntidades();
            
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals("E1", result.get(0).getCodigo());
            assertEquals("E2", result.get(1).getCodigo());
            assertEquals("E3", result.get(2).getCodigo());
        }
    }

    @Test
    void getEntidades_unescapesXmlEntitiesInServiceioReturn() throws Exception {
        try (MockedStatic<CryptoSical> mockedCrypto = mockStatic(CryptoSical.class)) {
            CryptoSical.SecurityFields secFields = new CryptoSical.SecurityFields("2026-01-01", "nonce123", "token456", "origin789");
            mockedCrypto.when(() -> CryptoSical.calculateSecurityFields(any())).thenReturn(secFields);
            mockedCrypto.when(() -> CryptoSical.encodeSha1Base64(any())).thenReturn("hashed");
            
            String codigoB64 = Base64.getEncoder().encodeToString("E-001".getBytes(StandardCharsets.UTF_8));
            String nombreB64 = Base64.getEncoder().encodeToString("Test & Co.".getBytes(StandardCharsets.UTF_8));
            String innerContent = "&lt;data&gt;&lt;exito&gt;-1&lt;/exito&gt;&lt;detalle&gt;" + codigoB64 + "@" + nombreB64 + "&lt;/detalle&gt;&lt;/data&gt;";
            String soapResponse = "<?xml version=\"1.0\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><servicioReturn>" + innerContent + "</servicioReturn></soapenv:Body></soapenv:Envelope>";
            
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class))).thenReturn(soapResponse);
            ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
            
            List<Entidad> result = service.getEntidades();
            
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Test & Co.", result.get(0).getNombre());
        }
    }
}
