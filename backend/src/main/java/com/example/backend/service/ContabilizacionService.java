package com.example.backend.service;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.backend.dto.ContabilizacionRequestDto;
import com.example.backend.dto.ContabilizacionResponseDto;
import com.example.backend.exception.XmlParsingException;
import com.example.backend.exception.SmlBuildingException;
import com.example.backend.sqlserver2.model.Fac;
import com.example.backend.sqlserver2.model.FacId;
import com.example.backend.sqlserver2.model.Fde;
import com.example.backend.sqlserver2.model.Fdt;
import com.example.backend.sqlserver2.model.Ter;
import com.example.backend.sqlserver2.repository.FacRepository;
import com.example.backend.sqlserver2.repository.FdeRepository;
import com.example.backend.sqlserver2.repository.FdtRepository;
import com.example.backend.sqlserver2.repository.TerRepository;
import com.example.sical.CryptoSical;

@Service
public class ContabilizacionService {

    @Value("${sical.ws.url:http://desa-sical-ws:8080/services/Ci?wsdl}")
    private String sicalWsUrl;

    @Autowired
    private FacRepository facRepository;

    @Autowired
    private FdeRepository fdeRepository;

    @Autowired
    private FdtRepository fdtRepository;

    @Autowired
    private TerRepository terRepository;

    public String buildSmlInput(ContabilizacionRequestDto req, Fac fac, List<Fde> fdeList, List<Fdt> fdtList, String terAyt) throws SmlBuildingException {
        try {
            CryptoSical.SecurityFields sec = CryptoSical.calculateSecurityFields(req.getPublicKey());
            String fechaContable = formatFechaContable(req.getFechaContable());
            
            StringBuilder sb = new StringBuilder();
            sb.append("<e>");
            appendOperationHeader(sb);
            appendSecuritySection(sb, req, sec);
            appendParametersSection(sb, req, fac, fdeList, fdtList, terAyt, fechaContable);
            sb.append("</e>");
            
            return sb.toString();
        } catch (SmlBuildingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SmlBuildingException("Error building SML input: " + ex.getMessage(), ex);
        }
    }

    private void appendOperationHeader(StringBuilder sb) {
        sb.append("<ope>");
        sb.append("<apl>SNP</apl>");
        sb.append("<tobj>GenOpeGasto</tobj>");
        sb.append("<cmd>CRE</cmd>");
        sb.append("<ver>2.0</ver>");
        sb.append("</ope>");
    }

    private void appendSecuritySection(StringBuilder sb, ContabilizacionRequestDto req, CryptoSical.SecurityFields sec) throws SmlBuildingException {
        try {
            String pwdSha1Base64 = CryptoSical.encodeSha1Base64(req.getPwd());
            String tokenSha1 = CryptoSical.encodeSha1Base64(sec.origin);
            
            sb.append("<sec>");
            sb.append("<cli>SAGE-AYTOS</cli>");
            sb.append("<org>").append(req.getOrg()).append("</org>");
            sb.append("<ent>").append(req.getEnt()).append("</ent>");
            sb.append("<eje>").append(req.getEje()).append("</eje>");
            sb.append("<usu>").append(req.getUsu()).append("</usu>");
            sb.append("<pwd>").append(pwdSha1Base64).append("</pwd>");
            sb.append("<fecha>").append(sec.created).append("</fecha>");
            sb.append("<nonce>").append(sec.nonce).append("</nonce>");
            sb.append("<token>").append(sec.token).append("</token>");
            sb.append("<tokenSha1>").append(tokenSha1).append("</tokenSha1>");
            sb.append("</sec>");
        } catch (Exception ex) {
            throw new SmlBuildingException("Error building security section: " + ex.getMessage(), ex);
        }
    }

    private void appendParametersSection(StringBuilder sb, ContabilizacionRequestDto req, Fac fac, List<Fde> fdeList, List<Fdt> fdtList, String terAyt, String fechaContable) throws SmlBuildingException {
        sb.append("<par>");
        sb.append("<gensinalmacenar>0</gensinalmacenar>");
        sb.append("<l_operacion>");
        sb.append("<operacion>");
        
        appendOperacionFields(sb, req, fac, terAyt, fechaContable);
        appendIvaSection(sb);
        appendContratoFields(sb, fac);
        
        sb.append("<l_factura>");
        sb.append("</l_factura>");
        appendFdeLines(sb, req.getEje(), fdeList);
        appendFdtLines(sb, req.getEje(), fdtList);
        
        sb.append("</operacion>");
        sb.append("</l_operacion>");
        sb.append("</par>");
    }

    private void appendOperacionFields(StringBuilder sb, ContabilizacionRequestDto req, Fac fac, String terAyt, String fechaContable) {
        String numope = fac.getEJE() + "-" + fac.getFACNUM();
        
        sb.append("<prevdef>").append(CryptoSical.encodeBase64("P")).append("</prevdef>");
        sb.append("<numope>").append(numope).append("</numope>");
        sb.append("<codope>").append(CryptoSical.encodeBase64("200")).append("</codope>");
        sb.append("<signo>0</signo>");
        sb.append("<areGes>").append(CryptoSical.encodeBase64(fac.getCGECOD())).append("</areGes>");
        
        if (terAyt != null && !terAyt.isEmpty()) {
            sb.append("<nif>").append(CryptoSical.encodeBase64(terAyt)).append("</nif>");
        }
        if (fac.getFACOPG() != null) {
            sb.append("<ort>").append(CryptoSical.encodeBase64(fac.getFACOPG())).append("</ort>");
        }
        
        sb.append("<fecont>").append(fechaContable).append("</fecont>");
        
        if (fac.getFACDAT() != null) {
            sb.append("<fdoc>").append(formatDate(fac.getFACDAT())).append("</fdoc>");
        }
        if (fac.getFACOCT() != null) {
            sb.append("<obp>").append(CryptoSical.encodeBase64(String.valueOf(fac.getFACOCT()))).append("</obp>");
        }
        if (fac.getFACFPG() != null && !fac.getFACFPG().isEmpty() && fac.getFACFPG().length() >= 8) {
            sb.append("<fpago>").append(formatDateString(fac.getFACFPG())).append("</fpago>");
        }
        if (fac.getFACTPG() != null) {
            sb.append("<tpago>").append(CryptoSical.encodeBase64(fac.getFACTPG())).append("</tpago>");
        }
        
        sb.append("<ofig>").append(CryptoSical.encodeBase64("AL")).append("</ofig>");
        
        if (fac.getFACTXT() != null) {
            sb.append("<text>").append(CryptoSical.encodeBase64(fac.getFACTXT())).append("</text>");
        }
        
        sb.append("<usuope>").append(CryptoSical.encodeBase64(req.getUsu())).append("</usuope>");
    }

    private void appendIvaSection(StringBuilder sb) {
        sb.append("<ivabex>0</ivabex>");
        sb.append("<ivabse1>0</ivabse1>");
        sb.append("<ivabse2>0</ivabse2>");
        sb.append("<ivabse3>0</ivabse3>");
        sb.append("<iva1>0</iva1>");
        sb.append("<iva2>0</iva2>");
        sb.append("<iva3>0</iva3>");
        sb.append("<piva1>0</piva1>");
        sb.append("<piva2>0</piva2>");
        sb.append("<piva3>0</piva3>");
    }

    private void appendContratoFields(StringBuilder sb, Fac fac) {
        String tipContrato = getContratoValue(fac.getCONCTP(), "Suministro");
        String proContrato = getContratoValue(fac.getCONCPR(), "AdDirec");
        String criContrato = getContratoValue(fac.getCONCCR(), "SinC");
        
        sb.append("<tipContrato>").append(CryptoSical.encodeBase64(tipContrato)).append("</tipContrato>");
        sb.append("<proContrato>").append(CryptoSical.encodeBase64(proContrato)).append("</proContrato>");
        sb.append("<criContrato>").append(CryptoSical.encodeBase64(criContrato)).append("</criContrato>");
    }

    private String getContratoValue(String value, String defaultValue) {
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    private void appendFdeLines(StringBuilder sb, String eje, List<Fde> fdeList) {
        sb.append("<l_linea>");
        for (Fde fde : fdeList) {
            Double imp = (fde.getFDEIMP() != null ? fde.getFDEIMP() : 0.0) + 
                         (fde.getFDEDIF() != null ? fde.getFDEDIF() : 0.0);
            
            if (imp <= 0) continue;
            
            sb.append("<linea>");
            sb.append("<lineje>").append(eje).append("</lineje>");
            if (fde.getFDEORG() != null) {
                sb.append("<org>").append(CryptoSical.encodeBase64(fde.getFDEORG())).append("</org>");
            }
            if (fde.getFDEFUN() != null) {
                sb.append("<fun>").append(CryptoSical.encodeBase64(fde.getFDEFUN())).append("</fun>");
            }
            if (fde.getFDEECO() != null) {
                sb.append("<eco>").append(CryptoSical.encodeBase64(fde.getFDEECO())).append("</eco>");
            }
            if (fde.getFDEREF() != null) {
                sb.append("<refe>").append(fde.getFDEREF()).append("</refe>");
            }
            sb.append("<imp>").append(imp).append("</imp>");
            sb.append("</linea>");
        }
        sb.append("</l_linea>");
    }

    private void appendFdtLines(StringBuilder sb, String eje, List<Fdt> fdtList) {
        sb.append("<l_dto>");
        for (Fdt fdt : fdtList) {
            appendDtoElement(sb, fdt, eje);
        }
        sb.append("</l_dto>");
    }

    private void appendDtoElement(StringBuilder sb, Fdt fdt, String eje) {
        sb.append("<dto>");
        appendOptionalField(sb, "areaD", fdt.getFDTARE(), false);
        sb.append("<ejeD>").append(eje).append("</ejeD>");
        appendOptionalField(sb, "orgD", fdt.getFDTORG(), true);
        appendOptionalField(sb, "funD", fdt.getFDTFUN(), true);
        appendOptionalField(sb, "ecoD", fdt.getFDTECO(), true);
        appendOptionalNumberField(sb, "impD", fdt.getFDTDTO());
        appendOptionalNumberField(sb, "baseRet", fdt.getFDTBSE());
        appendOptionalNumberField(sb, "porcRet", fdt.getFDTPRE());
        appendOptionalField(sb, "textoD", fdt.getFDTTXT(), true);
        sb.append("</dto>");
    }

    private void appendOptionalField(StringBuilder sb, String tag, String value, boolean encode) {
        if (value != null) {
            String encodedValue = encode ? CryptoSical.encodeBase64(value) : value;
            sb.append("<").append(tag).append(">").append(encodedValue).append("</").append(tag).append(">");
        }
    }

    private void appendOptionalNumberField(StringBuilder sb, String tag, Double value) {
        if (value != null) {
            sb.append("<").append(tag).append(">").append(value).append("</").append(tag).append(">");
        }
    }

    public String sendSmlRequest(String smlInput, String url) {
        String endpoint = (url != null && !url.isEmpty()) ? url : sicalWsUrl;
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_XML);
            headers.add("SOAPAction", "");

            String soapEnvelope =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soapenv:Body>" +
                "<ns1:servicio xmlns:ns1=\"http://desa-sical-ws:8080/services/Ci\">" +
                "<in0><![CDATA[" + smlInput + "]]></in0>" +
                "</ns1:servicio>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";

            HttpEntity<String> entity = new HttpEntity<>(soapEnvelope, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entity, String.class);
            
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public ContabilizacionResponseDto parseResponse(String soapResponse) {
        ContabilizacionResponseDto dto = new ContabilizacionResponseDto();
        
        try {
            String sml = extractSmlFromSoap(soapResponse);
            if (sml == null) {
                dto.setExito(false);
                dto.setMensaje("Respuesta SOAP inválida");
                return dto;
            }

            org.w3c.dom.Document doc = parseXmlDocument(sml);
            String exito = getTagValue(doc, "exito");
            
            return "-1".equals(exito) ? parseSuccessResponse(doc, dto) : parseFailureResponse(doc, dto);
        } catch (com.example.backend.exception.XmlParsingException ex) {
            dto.setExito(false);
            dto.setMensaje("XML parsing error: " + ex.getMessage());
        } catch (Exception e) {
            dto.setExito(false);
            dto.setMensaje("Error al procesar respuesta: " + e.getMessage());
        }
        
        return dto;
    }

    private org.w3c.dom.Document parseXmlDocument(String sml) throws com.example.backend.exception.XmlParsingException {
        try {
            javax.xml.parsers.DocumentBuilderFactory dbFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbFactory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbFactory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            javax.xml.parsers.DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.parse(new java.io.ByteArrayInputStream(sml.getBytes()));
        } catch (javax.xml.parsers.ParserConfigurationException | org.xml.sax.SAXException | java.io.IOException ex) {
            throw new com.example.backend.exception.XmlParsingException("Failed to parse XML document", ex);
        }
    }

    private ContabilizacionResponseDto parseSuccessResponse(org.w3c.dom.Document doc, ContabilizacionResponseDto dto) {
        dto.setExito(true);
        org.w3c.dom.NodeList opNodes = doc.getElementsByTagName("operacion");
        if (opNodes.getLength() > 0) {
            parseOperacionNode(opNodes.item(0), dto);
        }
        dto.setMensaje("Operación generada correctamente");
        return dto;
    }

    private void parseOperacionNode(org.w3c.dom.Node opNode, ContabilizacionResponseDto dto) {
        org.w3c.dom.NodeList children = opNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                mapOperacionField(child.getNodeName(), child.getTextContent(), dto);
            }
        }
    }

    private void mapOperacionField(String name, String value, ContabilizacionResponseDto dto) {
        switch (name) {
            case "opeext" -> dto.setOpeext(value);
            case "opesical" -> dto.setOpesical(value);
            case "nap" -> dto.setNap(value);
            case "referencia" -> dto.setReferencia(value);
            case "importe" -> dto.setImporte(value);
            case "ejercicio" -> dto.setEjercicio(value);
            case "organica" -> dto.setOrganica(decodeIfBase64(value));
            case "funcional" -> dto.setFuncional(decodeIfBase64(value));
            case "economica" -> dto.setEconomica(decodeIfBase64(value));
            default -> throw new IllegalArgumentException("Unknown field in XML response: " + name);
        }
    }

    private ContabilizacionResponseDto parseFailureResponse(org.w3c.dom.Document doc, ContabilizacionResponseDto dto) {
        dto.setExito(false);
        String desc = getTagValue(doc, "desc");
        String codigo = getTagValue(doc, "codigo");
        String errors = buildErrorString(doc);
        dto.setMensaje(buildErrorMessage(codigo, desc, errors));
        return dto;
    }

    private String buildErrorString(org.w3c.dom.Document doc) {
        org.w3c.dom.NodeList errorNodes = doc.getElementsByTagName("error");
        StringBuilder errors = new StringBuilder();
        for (int i = 0; i < errorNodes.getLength(); i++) {
            if (i > 0) errors.append("; ");
            errors.append(errorNodes.item(i).getTextContent());
        }
        return errors.toString();
    }

    private String buildErrorMessage(String codigo, String desc, String errors) {
        StringBuilder mensaje = new StringBuilder();
        if (codigo != null && !codigo.isEmpty()) {
            mensaje.append("Código: ").append(codigo).append(". ");
        }
        if (desc != null && !desc.isEmpty()) {
            mensaje.append(desc);
        }
        if (!errors.isEmpty()) {
            mensaje.append(" Errores: ").append(errors);
        }
        return mensaje.toString().isEmpty() ? "Error desconocido del servicio" : mensaje.toString();
    }

    private String formatFechaContable(String fecha) {
        if (fecha == null) return "";
        if (fecha.length() == 8 && !fecha.contains("-")) {
            return fecha;
        }
        return fecha.replace("-", "");
    }

    private String formatDateString(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        if (dateStr.length() == 8 && !dateStr.contains("-")) {
            return dateStr;
        }
        String cleaned = dateStr.replace("-", "");

        if (cleaned.length() >= 8) {
            return cleaned.substring(0, 8);
        }
        return cleaned; 
    }

    private String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private String extractSmlFromSoap(String soap) {
        if (soap == null || soap.isBlank()) {
            return null;
        }

        int start = soap.indexOf("<servicioReturn");
        if (start < 0) {
            return null;
        }

        int openTagEnd = soap.indexOf(">", start);
        if (openTagEnd < 0) {
            return null;
        }

        int end = soap.indexOf("</servicioReturn>", openTagEnd + 1);
        if (end <= openTagEnd) {
            return null;
        }

        return soap.substring(openTagEnd + 1, end)
            .replace("&lt;", "<")
            .replace("&gt;", ">");
    }

    private String getTagValue(org.w3c.dom.Document doc, String tag) {
        org.w3c.dom.NodeList nodes = doc.getElementsByTagName(tag);
        if (nodes.getLength() > 0 && nodes.item(0).getFirstChild() != null) {
            return nodes.item(0).getFirstChild().getNodeValue();
        }
        return null;
    }

    private String decodeIfBase64(String value) {
        if (value == null || value.isEmpty()) return value;
        try {
            return CryptoSical.decodeBase64(value);
        } catch (Exception e) {
            return value;
        }
    }
}