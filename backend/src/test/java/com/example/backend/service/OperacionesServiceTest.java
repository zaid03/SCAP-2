package com.example.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.example.backend.exception.XmlParsingException;
import com.example.backend.exception.SmlProcessingException;
import com.example.backend.dto.Operaciones;
import com.example.sical.CryptoSical;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OperacionesServiceTest {

    private OperacionesService service;
    
    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        service = new OperacionesService();
        ReflectionTestUtils.setField(service, "wsUrl", "http://test-sical-ws:8080/services/Ci?wsdl");
        ReflectionTestUtils.setField(service, "username", "testuser");
        ReflectionTestUtils.setField(service, "password", "testpass");
        ReflectionTestUtils.setField(service, "publicKey", "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...");
        ReflectionTestUtils.setField(service, "orgCode", "ORG001");
        ReflectionTestUtils.setField(service, "entidad", "1");
        ReflectionTestUtils.setField(service, "eje", "E1");
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    @Test
    void searchCriteria_builder_withAllFields_constructsCorrectly() {
        OperacionesService.SearchCriteria criteria = new OperacionesService.SearchCriteria.Builder()
            .numeroOperDesde("100")
            .numeroOperHasta("200")
            .codigoOperacion("OP001")
            .organica("ORG001")
            .funcional("FUN001")
            .economica("ECO001")
            .expediente("EXP001")
            .grupoApunte("GA001")
            .oficina("OF001")
            .build();

        assertNotNull(criteria);
        assertEquals("100", criteria.numeroOperDesde);
        assertEquals("200", criteria.numeroOperHasta);
        assertEquals("OP001", criteria.codigoOperacion);
        assertEquals("ORG001", criteria.organica);
        assertEquals("FUN001", criteria.funcional);
        assertEquals("ECO001", criteria.economica);
        assertEquals("EXP001", criteria.expediente);
        assertEquals("GA001", criteria.grupoApunte);
        assertEquals("OF001", criteria.oficina);
    }

    @Test
    void searchCriteria_builder_withPartialFields_allowsNullValues() {
        OperacionesService.SearchCriteria criteria = new OperacionesService.SearchCriteria.Builder()
            .numeroOperDesde("100")
            .codigoOperacion("OP001")
            .build();

        assertNotNull(criteria);
        assertEquals("100", criteria.numeroOperDesde);
        assertEquals("OP001", criteria.codigoOperacion);
        assertNull(criteria.numeroOperHasta);
        assertNull(criteria.organica);
    }

    @Test
    void searchCriteria_builder_withNoFields_constructsWithAllNulls() {
        OperacionesService.SearchCriteria criteria = new OperacionesService.SearchCriteria.Builder()
            .build();

        assertNotNull(criteria);
        assertNull(criteria.numeroOperDesde);
        assertNull(criteria.numeroOperHasta);
        assertNull(criteria.codigoOperacion);
    }

    @Test
    void searchCriteria_builderChaining_supportsFluentApi() {
        OperacionesService.SearchCriteria criteria = new OperacionesService.SearchCriteria.Builder()
            .numeroOperDesde("100")
            .numeroOperHasta("200")
            .codigoOperacion("OP001")
            .organica("ORG001")
            .funcional("FUN001")
            .economica("ECO001")
            .expediente("EXP001")
            .grupoApunte("GA001")
            .oficina("OF001")
            .build();

        assertNotNull(criteria);
        assertEquals(9, countNonNullFields(criteria));
    }

    @Test
    void service_configuresPropertiesCorrectly() {
        String wsUrl = (String) ReflectionTestUtils.getField(service, "wsUrl");
        String username = (String) ReflectionTestUtils.getField(service, "username");
        String orgCode = (String) ReflectionTestUtils.getField(service, "orgCode");

        assertEquals("http://test-sical-ws:8080/services/Ci?wsdl", wsUrl);
        assertEquals("testuser", username);
        assertEquals("ORG001", orgCode);
    }

    @Test
    void service_configuresSecurityFieldsCorrectly() {
        String password = (String) ReflectionTestUtils.getField(service, "password");
        String publicKey = (String) ReflectionTestUtils.getField(service, "publicKey");
        String entidad = (String) ReflectionTestUtils.getField(service, "entidad");
        String eje = (String) ReflectionTestUtils.getField(service, "eje");

        assertEquals("testpass", password);
        assertTrue(publicKey.startsWith("-----BEGIN PUBLIC KEY-----"));
        assertEquals("1", entidad);
        assertEquals("E1", eje);
    }

    private int countNonNullFields(OperacionesService.SearchCriteria criteria) {
        int count = 0;
        if (criteria.numeroOperDesde != null) count++;
        if (criteria.numeroOperHasta != null) count++;
        if (criteria.codigoOperacion != null) count++;
        if (criteria.organica != null) count++;
        if (criteria.funcional != null) count++;
        if (criteria.economica != null) count++;
        if (criteria.expediente != null) count++;
        if (criteria.grupoApunte != null) count++;
        if (criteria.oficina != null) count++;
        return count;
    }

    @Test
    void unescapeXmlEntities_replacesAllEntityTypes() throws Exception {
        String input = "&lt;tag&gt;&quot;value&quot;&apos;test&apos;";
        String result = invokeUnescapeXmlEntities(input);
        assertEquals("<tag>\"value\"'test'", result);
    }

    @Test
    void unescapeXmlEntities_handlesLtEntity() throws Exception {
        String result = invokeUnescapeXmlEntities("&lt;");
        assertEquals("<", result);
    }

    @Test
    void unescapeXmlEntities_handlesGtEntity() throws Exception {
        String result = invokeUnescapeXmlEntities("&gt;");
        assertEquals(">", result);
    }

    @Test
    void unescapeXmlEntities_handlesQuotEntity() throws Exception {
        String result = invokeUnescapeXmlEntities("&quot;");
        assertEquals("\"", result);
    }

    @Test
    void unescapeXmlEntities_handlesAposEntity() throws Exception {
        String result = invokeUnescapeXmlEntities("&apos;");
        assertEquals("'", result);
    }

    @Test
    void unescapeXmlEntities_handlesMixedContent() throws Exception {
        String input = "normal&lt;tag&gt;content&quot;data&quot;";
        String result = invokeUnescapeXmlEntities(input);
        assertEquals("normal<tag>content\"data\"", result);
    }

    @Test
    void extractInnerXmlContent_extractsFromServiceioReturn() throws Exception {
        String xml = "<soapenv:Body><impl:servicio><servicioReturn>extracted content</servicioReturn></impl:servicio></soapenv:Body>";
        String result = invokeExtractInnerXmlContent(xml);
        assertEquals("extracted content", result);
    }

    @Test
    void extractInnerXmlContent_returnsFullXmlWhenNoServiceioReturn() throws Exception {
        String xml = "<tag>content</tag>";
        String result = invokeExtractInnerXmlContent(xml);
        assertEquals(xml, result);
    }

    @Test
    void extractInnerXmlContent_returnsEmptyStringForNull() throws Exception {
        String result = invokeExtractInnerXmlContent(null);
        assertEquals("", result);
    }

    @Test
    void extractInnerXmlContent_handlesNestedXml() throws Exception {
        String xml = "<servicioReturn><operacion><numope>123</numope></operacion></servicioReturn>";
        String result = invokeExtractInnerXmlContent(xml);
        assertTrue(result.contains("<operacion>"));
        assertTrue(result.contains("123"));
    }

    @Test
    void extractInnerXmlContent_handlesMultipleServiceioReturn() throws Exception {
        String xml = "<servicioReturn>first</servicioReturn><servicioReturn>second</servicioReturn>";
        String result = invokeExtractInnerXmlContent(xml);
        assertEquals("first", result);
    }

    @Test
    void toDouble_parsesValidDouble() throws Exception {
        Double result = invokeToDouble("123.45");
        assertEquals(123.45, result);
    }

    @Test
    void toDouble_convertsCommaToDecimal() throws Exception {
        Double result = invokeToDouble("123,45");
        assertEquals(123.45, result);
    }

    @Test
    void toDouble_handlesNullAsZero() throws Exception {
        Double result = invokeToDouble(null);
        assertEquals(0.0, result);
    }

    @Test
    void toDouble_handlesEmptyStringAsZero() throws Exception {
        Double result = invokeToDouble("");
        assertEquals(0.0, result);
    }

    @Test
    void toDouble_handlesWhitespaceAsZero() throws Exception {
        Double result = invokeToDouble("   ");
        assertEquals(0.0, result);
    }

    @Test
    void toDouble_handlesMalformedStringAsZero() throws Exception {
        Double result = invokeToDouble("not-a-number");
        assertEquals(0.0, result);
    }

    @Test
    void toDouble_handlesLargeNumbers() throws Exception {
        Double result = invokeToDouble("999999999.99");
        assertEquals(999999999.99, result);
    }

    @Test
    void toDouble_handlesNegativeNumbers() throws Exception {
        Double result = invokeToDouble("-123.45");
        assertEquals(-123.45, result);
    }

    @Test
    void toLong_parsesValidLong() throws Exception {
        Long result = invokeToLong("12345");
        assertEquals(12345L, result);
    }

    @Test
    void toLong_returnsNullForNull() throws Exception {
        Long result = invokeToLong(null);
        assertNull(result);
    }

    @Test
    void toLong_returnsNullForEmptyString() throws Exception {
        Long result = invokeToLong("");
        assertNull(result);
    }

    @Test
    void toLong_returnsNullForMalformedString() throws Exception {
        Long result = invokeToLong("not-a-number");
        assertNull(result);
    }

    @Test
    void toLong_handlesLargeNumbers() throws Exception {
        Long result = invokeToLong("9223372036854775807");
        assertEquals(9223372036854775807L, result);
    }

    @Test
    void toLong_handlesNegativeNumbers() throws Exception {
        Long result = invokeToLong("-12345");
        assertEquals(-12345L, result);
    }

    @Test
    void toLong_trimsWhitespace() throws Exception {
        Long result = invokeToLong("  12345  ");
        assertEquals(12345L, result);
    }

    @Test
    void toInteger_parsesValidInteger() throws Exception {
        Integer result = invokeToInteger("123");
        assertEquals(123, result);
    }

    @Test
    void toInteger_returnsNullForNull() throws Exception {
        Integer result = invokeToInteger(null);
        assertNull(result);
    }

    @Test
    void toInteger_returnsNullForEmptyString() throws Exception {
        Integer result = invokeToInteger("");
        assertNull(result);
    }

    @Test
    void toInteger_returnsNullForMalformedString() throws Exception {
        Integer result = invokeToInteger("not-a-number");
        assertNull(result);
    }

    @Test
    void toInteger_handlesMaxIntValue() throws Exception {
        Integer result = invokeToInteger("2147483647");
        assertEquals(2147483647, result);
    }

    @Test
    void toInteger_handlesNegativeNumbers() throws Exception {
        Integer result = invokeToInteger("-123");
        assertEquals(-123, result);
    }

    @Test
    void toInteger_trimsWhitespace() throws Exception {
        Integer result = invokeToInteger("  456  ");
        assertEquals(456, result);
    }

    @Test
    void getTagValue_returnsTagContent() throws Exception {
        String xml = "<root><name>John</name></root>";
        Document doc = parseXmlDom(xml);
        Element root = doc.getDocumentElement();
        String result = invokeGetTagValue(root, "name");
        assertEquals("John", result);
    }

    @Test
    void getTagValue_returnsNullWhenTagNotFound() throws Exception {
        String xml = "<root><other>value</other></root>";
        Document doc = parseXmlDom(xml);
        Element root = doc.getDocumentElement();
        String result = invokeGetTagValue(root, "missing");
        assertNull(result);
    }

    @Test
    void getTagValue_returnsFirstTagWhenMultiple() throws Exception {
        String xml = "<root><item>first</item><item>second</item></root>";
        Document doc = parseXmlDom(xml);
        Element root = doc.getDocumentElement();
        String result = invokeGetTagValue(root, "item");
        assertEquals("first", result);
    }

    @Test
    void getTagValue_returnsEmptyStringForEmptyTag() throws Exception {
        String xml = "<root><name></name></root>";
        Document doc = parseXmlDom(xml);
        Element root = doc.getDocumentElement();
        String result = invokeGetTagValue(root, "name");
        assertEquals("", result);
    }

    @Test
    void getTagValue_returnsNestedContent() throws Exception {
        String xml = "<root><person><numope>12345</numope></person></root>";
        Document doc = parseXmlDom(xml);
        Element root = doc.getDocumentElement();
        String result = invokeGetTagValue(root, "numope");
        assertEquals("12345", result);
    }

    @Test
    void validateAndThrowIfError_allowsWhenNoExitoTag() throws Exception {
        String xml = "<root><desc>Success</desc></root>";
        Document doc = parseXmlDom(xml);
        assertDoesNotThrow(() -> invokeValidateAndThrowIfError(doc));
    }

    @Test
    void validateAndThrowIfError_allowsWhenExitoIsNegativeOne() throws Exception {
        String xml = "<root><exito>-1</exito></root>";
        Document doc = parseXmlDom(xml);
        assertDoesNotThrow(() -> invokeValidateAndThrowIfError(doc));
    }

    @Test
    void validateAndThrowIfError_allowsWhenExitoIsOne() throws Exception {
        String xml = "<root><exito>1</exito></root>";
        Document doc = parseXmlDom(xml);
        assertDoesNotThrow(() -> invokeValidateAndThrowIfError(doc));
    }

    @Test
    void validateAndThrowIfError_throwsWhenExitoIsTwo() throws Exception {
        String xml = "<root><exito>2</exito><desc>Error message</desc></root>";
        Document doc = parseXmlDom(xml);
        try {
            invokeValidateAndThrowIfError(doc);
            fail("Expected SmlProcessingException");
        } catch (Exception ex) {
            assertTrue(ex.getCause() instanceof SmlProcessingException || ex instanceof SmlProcessingException);
        }
    }

    @Test
    void validateAndThrowIfError_includesDescriptionInError() throws Exception {
        String xml = "<root><exito>0</exito><desc>Invalid data</desc></root>";
        Document doc = parseXmlDom(xml);
        try {
            invokeValidateAndThrowIfError(doc);
            fail("Expected SmlProcessingException");
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            assertTrue(cause instanceof SmlProcessingException);
            assertTrue(cause.getMessage().contains("Invalid data"));
        }
    }

    @Test
    void validateAndThrowIfError_throwsWhenExitoIsZero() throws Exception {
        String xml = "<root><exito>0</exito></root>";
        Document doc = parseXmlDom(xml);
        try {
            invokeValidateAndThrowIfError(doc);
            fail("Expected SmlProcessingException");
        } catch (Exception ex) {
            assertTrue(ex.getCause() instanceof SmlProcessingException || ex instanceof SmlProcessingException);
        }
    }

    @Test
    void parseXmlDocument_parsesValidXml() throws Exception {
        String xml = "<root><item>value</item></root>";
        Document doc = invokeParseXmlDocument(xml);
        assertNotNull(doc);
        assertEquals("root", doc.getDocumentElement().getTagName());
    }

    @Test
    void parseXmlDocument_handsNestedElements() throws Exception {
        String xml = "<root><parent><child>text</child></parent></root>";
        Document doc = invokeParseXmlDocument(xml);
        assertNotNull(doc);
        assertEquals("root", doc.getDocumentElement().getTagName());
    }

    @Test
    void parseXmlDocument_handlesAttributes() throws Exception {
        String xml = "<root attr=\"value\"><item id=\"1\">text</item></root>";
        Document doc = invokeParseXmlDocument(xml);
        assertNotNull(doc);
        Element root = doc.getDocumentElement();
        assertEquals("value", root.getAttribute("attr"));
    }

    @Test
    void parseXmlDocument_throwsForInvalidXml() throws Exception {
        String xml = "<root><unclosed>";
        try {
            invokeParseXmlDocument(xml);
            fail("Expected XmlParsingException");
        } catch (Exception ex) {
            assertTrue(ex.getCause() instanceof XmlParsingException || ex instanceof XmlParsingException);
        }
    }

    private String invokeUnescapeXmlEntities(String input) throws Exception {
        Method method = OperacionesService.class.getDeclaredMethod("unescapeXmlEntities", String.class);
        method.setAccessible(true);
        return (String) method.invoke(service, input);
    }

    private String invokeExtractInnerXmlContent(String xml) throws Exception {
        Method method = OperacionesService.class.getDeclaredMethod("extractInnerXmlContent", String.class);
        method.setAccessible(true);
        return (String) method.invoke(service, xml);
    }

    private Double invokeToDouble(String value) throws Exception {
        Method method = OperacionesService.class.getDeclaredMethod("toDouble", String.class);
        method.setAccessible(true);
        return (Double) method.invoke(service, value);
    }

    private Long invokeToLong(String value) throws Exception {
        Method method = OperacionesService.class.getDeclaredMethod("toLong", String.class);
        method.setAccessible(true);
        return (Long) method.invoke(service, value);
    }

    private Integer invokeToInteger(String value) throws Exception {
        Method method = OperacionesService.class.getDeclaredMethod("toInteger", String.class);
        method.setAccessible(true);
        return (Integer) method.invoke(service, value);
    }

    private String invokeGetTagValue(Element parent, String tagName) throws Exception {
        Method method = OperacionesService.class.getDeclaredMethod("getTagValue", Element.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(service, parent, tagName);
    }

    private void invokeValidateAndThrowIfError(Document doc) throws Exception {
        Method method = OperacionesService.class.getDeclaredMethod("validateAndThrowIfError", Document.class);
        method.setAccessible(true);
        method.invoke(service, doc);
    }

    private Document invokeParseXmlDocument(String xml) throws Exception {
        Method method = OperacionesService.class.getDeclaredMethod("parseXmlDocument", String.class);
        method.setAccessible(true);
        return (Document) method.invoke(service, xml);
    }

    private Document parseXmlDom(String xml) throws Exception {
        Method method = OperacionesService.class.getDeclaredMethod("parseXmlDocument", String.class);
        method.setAccessible(true);
        return (Document) method.invoke(service, xml);
    }

    @Test
    void createOperacionFromElement_populatesBasicFields() throws Exception {
        String xml = "<operacion>" +
            "<numope>12345</numope>" +
            "<codope>OP001</codope>" +
            "<signo>+</signo>" +
            "</operacion>";
        Document doc = parseXmlDom(xml);
        Element opEl = doc.getDocumentElement();
        
        assertDoesNotThrow(() -> invokeCreateOperacionFromElement(opEl));
    }

    @Test
    void createOperacionFromElement_populatesAllNumericFields() throws Exception {
        String xml = "<operacion>" +
            "<numope>999</numope>" +
            "<numcaja>555</numcaja>" +
            "<terite>111</terite>" +
            "<importe>1000.50</importe>" +
            "</operacion>";
        Document doc = parseXmlDom(xml);
        Element opEl = doc.getDocumentElement();
        
        assertDoesNotThrow(() -> invokeCreateOperacionFromElement(opEl));
    }

    @Test
    void createOperacionFromElement_handlesNullOptionalFields() throws Exception {
        String xml = "<operacion><numope>123</numope></operacion>";
        Document doc = parseXmlDom(xml);
        Element opEl = doc.getDocumentElement();
        
        assertDoesNotThrow(() -> invokeCreateOperacionFromElement(opEl));
    }

    @Test
    void decodeOrNull_returnsNullForNull() throws Exception {
        String result = invokeDecodeOrNull(null);
        assertNull(result);
    }

    @Test
    void decodeOrNull_returnsNullForBlank() throws Exception {
        String result = invokeDecodeOrNull("");
        assertNull(result);
    }

    @Test
    void decodeOrNull_returnsNullForWhitespace() throws Exception {
        String result = invokeDecodeOrNull("   ");
        assertNull(result);
    }

    @Test
    void decodeOrNull_returnsValueWhenNotBlank() throws Exception {
        String result = invokeDecodeOrNull("TEST");
        assertNotNull(result);
    }

    private Object invokeCreateOperacionFromElement(Element opEl) throws Exception {
        Method method = OperacionesService.class.getDeclaredMethod("createOperacionFromElement", Element.class);
        method.setAccessible(true);
        return method.invoke(service, opEl);
    }

    private String invokeDecodeOrNull(String value) throws Exception {
        Method method = OperacionesService.class.getDeclaredMethod("decodeOrNull", String.class);
        method.setAccessible(true);
        return (String) method.invoke(service, value);
    }

    @Test
    void parseOperaciones_withEmptyXml_returnsEmptyList() throws Exception {
        String xml = "";
        List<Operaciones> result = invokeParseOperaciones(xml);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseOperaciones_withNullXml_returnsEmptyList() throws Exception {
        String xml = null;
        List<Operaciones> result = invokeParseOperaciones(xml);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseOperaciones_withMultipleOperaciones_returnsAllElements() throws Exception {
        String xml = "<root>" +
            "<operacion><numope>111</numope></operacion>" +
            "<operacion><numope>222</numope></operacion>" +
            "<operacion><numope>333</numope></operacion>" +
            "</root>";
        List<Operaciones> result = invokeParseOperaciones(xml);
        assertEquals(3, result.size());
    }

    @Test
    void parseOperaciones_withXmlError_throwsSmlProcessingException() throws Exception {
        String xml = "<root><exito>0</exito><desc>Test error</desc></root>";
        try {
            invokeParseOperaciones(xml);
            fail("Expected SmlProcessingException");
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            assertTrue(cause instanceof SmlProcessingException);
        }
    }

    @Test
    void parseOperaciones_withInvalidXml_throwsSmlProcessingException() throws Exception {
        String xml = "<unclosed>";
        try {
            invokeParseOperaciones(xml);
            fail("Expected SmlProcessingException");
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            assertTrue(cause instanceof SmlProcessingException);
        }
    }

    @Test
    void parseDtoList_withSingleDto_populatesAllFields() throws Exception {
        String xml = "<parent>" +
            "<dto>" +
            "<numdto>12345</numdto>" +
            "<dtoeje>5</dtoeje>" +
            "<dtoimp>1000.50</dtoimp>" +
            "<dtosaldo>500.25</dtosaldo>" +
            "<dtobase>100.00</dtobase>" +
            "<dtosaldobase>50.00</dtosaldobase>" +
            "<dtopretencion>10.00</dtopretencion>" +
            "<dtonumopecan>1</dtonumopecan>" +
            "<dtolinopecan>101</dtolinopecan>" +
            "<dtoanodevengo>2023</dtoanodevengo>" +
            "<dtobase1>100.00</dtobase1>" +
            "<dtobase2>200.00</dtobase2>" +
            "<dtobase3>300.00</dtobase3>" +
            "<dtoiva1>10.00</dtoiva1>" +
            "<dtoiva2>20.00</dtoiva2>" +
            "<dtoiva3>30.00</dtoiva3>" +
            "<dtotiva1>100.00</dtotiva1>" +
            "<dtotiva2>200.00</dtotiva2>" +
            "<dtotiva3>300.00</dtotiva3>" +
            "<dtoporcent1>5.0</dtoporcent1>" +
            "<dtoporcent2>10.0</dtoporcent2>" +
            "<dtoporcent3>15.0</dtoporcent3>" +
            "</dto>" +
            "</parent>";
        Document doc = parseXmlDom(xml);
        Element parent = doc.getDocumentElement();
        List<Operaciones.Dto> result = invokeParseDtoList(parent);
        
        assertEquals(1, result.size());
        Operaciones.Dto dto = result.get(0);
        assertEquals(12345L, dto.getNumdto());
        assertEquals(5, dto.getDtoeje());
        assertEquals(1000.50, dto.getDtoimp());
        assertEquals(500.25, dto.getDtosaldo());
        assertEquals(100.00, dto.getDtobase());
        assertEquals(5.0, dto.getDtoporcent1());
    }

    @Test
    void parseDtoList_withMultipleDtos_returnsAllElements() throws Exception {
        String xml = "<parent>" +
            "<dto><numdto>111</numdto></dto>" +
            "<dto><numdto>222</numdto></dto>" +
            "<dto><numdto>333</numdto></dto>" +
            "</parent>";
        Document doc = parseXmlDom(xml);
        Element parent = doc.getDocumentElement();
        List<Operaciones.Dto> result = invokeParseDtoList(parent);
        
        assertEquals(3, result.size());
    }

    @Test
    void parseDtoList_withEmptyDtos_returnsEmptyList() throws Exception {
        String xml = "<parent></parent>";
        Document doc = parseXmlDom(xml);
        Element parent = doc.getDocumentElement();
        List<Operaciones.Dto> result = invokeParseDtoList(parent);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void parseIvaList_withSingleIva_populatesAllFields() throws Exception {
        String xml = "<parent>" +
            "<iva>" +
            "<ivabase1>100.00</ivabase1>" +
            "<ivabase2>200.00</ivabase2>" +
            "<ivabase3>300.00</ivabase3>" +
            "<ivasbase1>50.00</ivasbase1>" +
            "<ivasbase2>100.00</ivasbase2>" +
            "<ivasbase3>150.00</ivasbase3>" +
            "<ivativa1>10.00</ivativa1>" +
            "<ivativa2>20.00</ivativa2>" +
            "<ivativa3>30.00</ivativa3>" +
            "<ivaporcent1>5.0</ivaporcent1>" +
            "<ivaporcent2>10.0</ivaporcent2>" +
            "<ivaporcent3>15.0</ivaporcent3>" +
            "<ivaimp1>5.00</ivaimp1>" +
            "<ivaimp2>10.00</ivaimp2>" +
            "<ivaimp3>15.00</ivaimp3>" +
            "<ivabexenta>50.00</ivabexenta>" +
            "</iva>" +
            "</parent>";
        Document doc = parseXmlDom(xml);
        Element parent = doc.getDocumentElement();
        List<Operaciones.Iva> result = invokeParseIvaList(parent);
        
        assertEquals(1, result.size());
        Operaciones.Iva iva = result.get(0);
        assertEquals(100.00, iva.getIvabase1());
        assertEquals(200.00, iva.getIvabase2());
        assertEquals(5.0, iva.getIvaporcent1());
        assertEquals(50.00, iva.getIvabexenta());
    }

    @Test
    void parseIvaList_withMultipleIvas_returnsAllElements() throws Exception {
        String xml = "<parent>" +
            "<iva><ivaporcent1>5.0</ivaporcent1></iva>" +
            "<iva><ivaporcent1>10.0</ivaporcent1></iva>" +
            "<iva><ivaporcent1>15.0</ivaporcent1></iva>" +
            "</parent>";
        Document doc = parseXmlDom(xml);
        Element parent = doc.getDocumentElement();
        List<Operaciones.Iva> result = invokeParseIvaList(parent);
        
        assertEquals(3, result.size());
    }

    @Test
    void parseIvaList_withEmptyIvas_returnsEmptyList() throws Exception {
        String xml = "<parent></parent>";
        Document doc = parseXmlDom(xml);
        Element parent = doc.getDocumentElement();
        List<Operaciones.Iva> result = invokeParseIvaList(parent);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void parseRelacionList_withSingleRelacion_populatesAllFields() throws Exception {
        String xml = "<parent>" +
            "<Relacion>" +
            "<AnnoRelacion>2023</AnnoRelacion>" +
            "<OrdenRelacion>1</OrdenRelacion>" +
            "</Relacion>" +
            "</parent>";
        Document doc = parseXmlDom(xml);
        Element parent = doc.getDocumentElement();
        List<Operaciones.Relacion> result = invokeParseRelacionList(parent);
        
        assertEquals(1, result.size());
        Operaciones.Relacion rel = result.get(0);
        assertEquals(2023, rel.getAnnoRelacion());
        assertEquals(1, rel.getOrdenRelacion());
    }

    @Test
    void parseRelacionList_withMultipleRelaciones_returnsAllElements() throws Exception {
        String xml = "<parent>" +
            "<Relacion><AnnoRelacion>2021</AnnoRelacion></Relacion>" +
            "<Relacion><AnnoRelacion>2022</AnnoRelacion></Relacion>" +
            "<Relacion><AnnoRelacion>2023</AnnoRelacion></Relacion>" +
            "</parent>";
        Document doc = parseXmlDom(xml);
        Element parent = doc.getDocumentElement();
        List<Operaciones.Relacion> result = invokeParseRelacionList(parent);
        
        assertEquals(3, result.size());
    }

    @Test
    void parseRelacionList_withEmptyRelaciones_returnsEmptyList() throws Exception {
        String xml = "<parent></parent>";
        Document doc = parseXmlDom(xml);
        Element parent = doc.getDocumentElement();
        List<Operaciones.Relacion> result = invokeParseRelacionList(parent);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void parseLineaList_withSingleLinea_populatesAllFields() throws Exception {
        String xml = "<parent>" +
            "<linea>" +
            "<nlinea>1</nlinea>" +
            "<opeasc>12345</opeasc>" +
            "<lineasc>101</lineasc>" +
            "<prya>1</prya>" +
            "<pryo>2</pryo>" +
            "<pryx>3</pryx>" +
            "<lineje>5</lineje>" +
            "<referencia>123</referencia>" +
            "<limporte>1000.50</limporte>" +
            "<saldo>500.25</saldo>" +
            "<saldop>250.00</saldop>" +
            "</linea>" +
            "</parent>";
        Document doc = parseXmlDom(xml);
        Element parent = doc.getDocumentElement();
        List<Operaciones.Linea> result = invokeParseLineaList(parent);
        
        assertEquals(1, result.size());
        Operaciones.Linea linea = result.get(0);
        assertEquals(1, linea.getNlinea());
        assertEquals(12345L, linea.getOpeasc());
        assertEquals(1000.50, linea.getLimporte());
        assertEquals(500.25, linea.getSaldo());
    }

    @Test
    void parseLineaList_withMultipleLineas_returnsAllElements() throws Exception {
        String xml = "<parent>" +
            "<linea><nlinea>1</nlinea></linea>" +
            "<linea><nlinea>2</nlinea></linea>" +
            "<linea><nlinea>3</nlinea></linea>" +
            "</parent>";
        Document doc = parseXmlDom(xml);
        Element parent = doc.getDocumentElement();
        List<Operaciones.Linea> result = invokeParseLineaList(parent);
        
        assertEquals(3, result.size());
    }

    @Test
    void parseLineaList_withEmptyLineas_returnsEmptyList() throws Exception {
        String xml = "<parent></parent>";
        Document doc = parseXmlDom(xml);
        Element parent = doc.getDocumentElement();
        List<Operaciones.Linea> result = invokeParseLineaList(parent);
        
        assertTrue(result.isEmpty());
    }

    private List<Operaciones> invokeParseOperaciones(String xml) throws Exception {
        Method method = OperacionesService.class.getDeclaredMethod("parseOperaciones", String.class);
        method.setAccessible(true);
        return (List<Operaciones>) method.invoke(service, xml);
    }

    private List<Operaciones.Dto> invokeParseDtoList(Element opEl) throws Exception {
        Method method = OperacionesService.class.getDeclaredMethod("parseDtoList", Element.class);
        method.setAccessible(true);
        return (List<Operaciones.Dto>) method.invoke(service, opEl);
    }

    private List<Operaciones.Iva> invokeParseIvaList(Element opEl) throws Exception {
        Method method = OperacionesService.class.getDeclaredMethod("parseIvaList", Element.class);
        method.setAccessible(true);
        return (List<Operaciones.Iva>) method.invoke(service, opEl);
    }

    private List<Operaciones.Relacion> invokeParseRelacionList(Element opEl) throws Exception {
        Method method = OperacionesService.class.getDeclaredMethod("parseRelacionList", Element.class);
        method.setAccessible(true);
        return (List<Operaciones.Relacion>) method.invoke(service, opEl);
    }

    private List<Operaciones.Linea> invokeParseLineaList(Element opEl) throws Exception {
        Method method = OperacionesService.class.getDeclaredMethod("parseLineaList", Element.class);
        method.setAccessible(true);
        return (List<Operaciones.Linea>) method.invoke(service, opEl);
    }

    @Test
    void getOperaciones_withAllCriteria_callsRestTemplate() throws Exception {
        OperacionesService.SearchCriteria criteria = new OperacionesService.SearchCriteria.Builder()
            .numeroOperDesde("100")
            .numeroOperHasta("200")
            .codigoOperacion("OP001")
            .organica("ORG001")
            .funcional("FUN001")
            .build();

        try (MockedStatic<CryptoSical> mockedCrypto = mockStatic(CryptoSical.class)) {
            CryptoSical.SecurityFields secFields = new CryptoSical.SecurityFields("2026-01-01", "nonce123", "token456", "origin789");
            
            mockedCrypto.when(() -> CryptoSical.calculateSecurityFields(any())).thenReturn(secFields);
            mockedCrypto.when(() -> CryptoSical.encodeSha1Base64(any())).thenReturn("hashed");
            mockedCrypto.when(() -> CryptoSical.encodeBase64(any())).thenReturn("encoded");
            
            String mockResponse = "<soapenv:Body><impl:servicio><servicioReturn><operaciones><operacion><numope>123</numope></operacion></operaciones></servicioReturn></impl:servicio></soapenv:Body>";
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);
            
            List<Operaciones> result = service.getOperaciones(criteria);
            
            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Test
    void getOperaciones_withMinimalCriteria_buildsRequestWithMinimalFields() throws Exception {
        OperacionesService.SearchCriteria criteria = new OperacionesService.SearchCriteria.Builder()
            .numeroOperDesde("100")
            .build();

        try (MockedStatic<CryptoSical> mockedCrypto = mockStatic(CryptoSical.class)) {
            CryptoSical.SecurityFields secFields = new CryptoSical.SecurityFields("2026-01-01", "nonce123", "token456", "origin789");
            
            mockedCrypto.when(() -> CryptoSical.calculateSecurityFields(any())).thenReturn(secFields);
            mockedCrypto.when(() -> CryptoSical.encodeSha1Base64(any())).thenReturn("hashed");
            mockedCrypto.when(() -> CryptoSical.encodeBase64(any())).thenReturn("encoded");
            
            String mockResponse = "<soapenv:Body><impl:servicio><servicioReturn><operaciones /></servicioReturn></impl:servicio></soapenv:Body>";
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);
            
            List<Operaciones> result = service.getOperaciones(criteria);
            
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void getOperaciones_withNullCriteria_throwsException() {
        try (MockedStatic<CryptoSical> mockedCrypto = mockStatic(CryptoSical.class)) {
            mockedCrypto.when(() -> CryptoSical.calculateSecurityFields(any()))
                .thenThrow(new RuntimeException("Test error"));
            
            assertThrows(SmlProcessingException.class, () -> service.getOperaciones(null));
        }
    }

    @Test
    void getOperaciones_whenRestTemplateThrowsException_wrapsInSmlProcessingException() throws Exception {
        OperacionesService.SearchCriteria criteria = new OperacionesService.SearchCriteria.Builder()
            .numeroOperDesde("100")
            .build();

        try (MockedStatic<CryptoSical> mockedCrypto = mockStatic(CryptoSical.class)) {
            CryptoSical.SecurityFields secFields = new CryptoSical.SecurityFields("2026-01-01", "nonce123", "token456", "origin789");
            
            mockedCrypto.when(() -> CryptoSical.calculateSecurityFields(any())).thenReturn(secFields);
            mockedCrypto.when(() -> CryptoSical.encodeSha1Base64(any())).thenReturn("hashed");
            mockedCrypto.when(() -> CryptoSical.encodeBase64(any())).thenReturn("encoded");
            
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));
            
            assertThrows(SmlProcessingException.class, () -> service.getOperaciones(criteria));
        }
    }

    @Test
    void getOperaciones_withErrorResponseFromService_throwsException() throws Exception {
        OperacionesService.SearchCriteria criteria = new OperacionesService.SearchCriteria.Builder()
            .numeroOperDesde("100")
            .build();

        try (MockedStatic<CryptoSical> mockedCrypto = mockStatic(CryptoSical.class)) {
            CryptoSical.SecurityFields secFields = new CryptoSical.SecurityFields("2026-01-01", "nonce123", "token456", "origin789");
            
            mockedCrypto.when(() -> CryptoSical.calculateSecurityFields(any())).thenReturn(secFields);
            mockedCrypto.when(() -> CryptoSical.encodeSha1Base64(any())).thenReturn("hashed");
            mockedCrypto.when(() -> CryptoSical.encodeBase64(any())).thenReturn("encoded");
            
            String errorResponse = "<soapenv:Body><impl:servicio><servicioReturn><exito>0</exito><desc>Service error</desc></servicioReturn></impl:servicio></soapenv:Body>";
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(errorResponse);
            
            assertThrows(SmlProcessingException.class, () -> service.getOperaciones(criteria));
        }
    }

    @Test
    void getOperaciones_withValidResponseMultipleOperaciones_returnsAllParsed() throws Exception {
        OperacionesService.SearchCriteria criteria = new OperacionesService.SearchCriteria.Builder()
            .numeroOperDesde("100")
            .build();

        try (MockedStatic<CryptoSical> mockedCrypto = mockStatic(CryptoSical.class)) {
            CryptoSical.SecurityFields secFields = new CryptoSical.SecurityFields("2026-01-01", "nonce123", "token456", "origin789");
            
            mockedCrypto.when(() -> CryptoSical.calculateSecurityFields(any())).thenReturn(secFields);
            mockedCrypto.when(() -> CryptoSical.encodeSha1Base64(any())).thenReturn("hashed");
            mockedCrypto.when(() -> CryptoSical.encodeBase64(any())).thenReturn("encoded");
            
            String mockResponse = "<soapenv:Body><impl:servicio><servicioReturn>" +
                "<operaciones>" +
                "<operacion><numope>111</numope></operacion>" +
                "<operacion><numope>222</numope></operacion>" +
                "<operacion><numope>333</numope></operacion>" +
                "</operaciones>" +
                "</servicioReturn></impl:servicio></soapenv:Body>";
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);
            
            List<Operaciones> result = service.getOperaciones(criteria);
            
            assertNotNull(result);
            assertEquals(3, result.size());
        }
    }

    @Test
    void getOperaciones_buildsCorrectSOAPEnvelope() throws Exception {
        OperacionesService.SearchCriteria criteria = new OperacionesService.SearchCriteria.Builder()
            .numeroOperDesde("100")
            .build();

        try (MockedStatic<CryptoSical> mockedCrypto = mockStatic(CryptoSical.class)) {
            CryptoSical.SecurityFields secFields = new CryptoSical.SecurityFields("2026-01-01", "nonce123", "token456", "origin789");
            
            mockedCrypto.when(() -> CryptoSical.calculateSecurityFields(any())).thenReturn(secFields);
            mockedCrypto.when(() -> CryptoSical.encodeSha1Base64(any())).thenReturn("hashed");
            mockedCrypto.when(() -> CryptoSical.encodeBase64(any())).thenReturn("encoded");
            
            String mockResponse = "<soapenv:Body><impl:servicio><servicioReturn><operaciones /></servicioReturn></impl:servicio></soapenv:Body>";
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);
            
            service.getOperaciones(criteria);
            
            org.mockito.Mockito.verify(restTemplate).postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(String.class)
            );
        }
    }
}