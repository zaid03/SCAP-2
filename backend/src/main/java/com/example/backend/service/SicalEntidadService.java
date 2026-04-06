package com.example.backend.service;

import com.example.backend.dto.Entidad;
import com.example.backend.exception.SmlProcessingException;
import com.example.sical.CryptoSical;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

@Service
public class SicalEntidadService {

    @Value("${sical.org.code}")
    private String orgCode;
    @Value("${sical.entidad}")
    private String entidad;
    @Value("${sical.eje}")
    private String eje;
    @Value("${sical.username}")
    private String username;
    @Value("${sical.password}")
    private String password;
    @Value("${sical.ws.url}")
    private String wsUrl;
    @Value("${sical.public.key}")
    private String publicKey;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    public List<Entidad> getEntidades() throws SmlProcessingException {
        try {
            CryptoSical.SecurityFields sec = CryptoSical.calculateSecurityFields(publicKey);

        StringBuilder sb = new StringBuilder();
        sb.append("<e>");
        sb.append("<ope><apl>SNP</apl><tobj>ListaEntidades</tobj><cmd>LST</cmd><ver>2.0</ver></ope>");
        sb.append("<sec>");
        sb.append("<cli>SAGE-AYTOS</cli>");
        sb.append("<org>").append(orgCode).append("</org>");
        sb.append("<ent>").append(entidad).append("</ent>");
        sb.append("<eje>").append(eje).append("</eje>");
        sb.append("<usu>").append(username).append("</usu>");
        sb.append("<pwd>").append(CryptoSical.encodeSha1Base64(password)).append("</pwd>");
        sb.append("<fecha>").append(sec.created).append("</fecha>");
        sb.append("<nonce>").append(sec.nonce).append("</nonce>");
        sb.append("<token>").append(sec.token).append("</token>");
        sb.append("<tokenSha1>").append(CryptoSical.encodeSha1Base64(sec.origin)).append("</tokenSha1>");
        sb.append("</sec>");
        sb.append("<par><ruta></ruta></par>");
        sb.append("</e>");

        String xml = sb.toString();

        String soapEnvelope =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:impl=\"http://desa-sical-ws:8080/services/Ci\">" +
              "<soapenv:Header/>" +
              "<soapenv:Body>" +
                "<impl:servicio>" +
                  "<impl:in0><![CDATA[" + xml + "]]></impl:in0>" +
                "</impl:servicio>" +
              "</soapenv:Body>" +
            "</soapenv:Envelope>";

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "text/xml; charset=utf-8");
        headers.add("SOAPAction", "");

        HttpEntity<String> request = new HttpEntity<>(soapEnvelope, headers);
        RestTemplate template = (this.restTemplate != null) ? this.restTemplate : new RestTemplate();
        String responseXml = template.postForObject(wsUrl, request, String.class);
        
        if (responseXml == null) {
            throw new SmlProcessingException("Empty response from SICAL");
        }

        String inner = null;
        int start = responseXml.indexOf("<servicioReturn");
        if (start >= 0) {
            int gt = responseXml.indexOf(">", start);
            int end = responseXml.indexOf("</servicioReturn>", gt);
            if (gt >= 0 && end >= 0) {
                inner = responseXml.substring(gt + 1, end);
            }
        }

        if (inner != null) {
            String sml = unescapeXml(inner);
            return parseEntidades(sml);
        } else {
            return parseEntidades(responseXml);
        }
        } catch (SmlProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SmlProcessingException("Error retrieving entidades: " + ex.getMessage(), ex);
        }
    }

    private String unescapeXml(String s) {
        if (s == null) return null;
        return s.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }

    private List<Entidad> parseEntidades(String sml) throws SmlProcessingException {
        try {
            List<Entidad> result = new ArrayList<>();
            if (sml == null || sml.isEmpty()) {
                return result;
            }

            Document doc = parseXmlDocument(sml);
            validateExito(doc);
            
            NodeList detalleNodes = doc.getElementsByTagName("detalle");
            return parseDetalleList(detalleNodes);
        } catch (SmlProcessingException ex) {
            throw ex;
        } catch (com.example.backend.exception.XmlParsingException ex) {
            throw new SmlProcessingException("Error parsing entidades response: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new SmlProcessingException("Error processing entidades response: " + ex.getMessage(), ex);
        }
    }

    private Document parseXmlDocument(String sml) throws com.example.backend.exception.XmlParsingException {
        try {
            DocumentBuilderFactory dbf = configureDocumentBuilderFactory();
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(new java.io.ByteArrayInputStream(sml.getBytes(StandardCharsets.UTF_8)));
        } catch (com.example.backend.exception.XmlParsingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new com.example.backend.exception.XmlParsingException("Failed to parse XML document", ex);
        }
    }

    private DocumentBuilderFactory configureDocumentBuilderFactory() throws com.example.backend.exception.XmlParsingException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return dbf;
        } catch (Exception ex) {
            throw new com.example.backend.exception.XmlParsingException("Failed to configure document builder factory", ex);
        }
    }

    private void validateExito(Document doc) throws SmlProcessingException {
        NodeList exitoNodes = doc.getElementsByTagName("exito");
        if (exitoNodes == null || exitoNodes.getLength() == 0) {
            return;
        }

        String exito = exitoNodes.item(0).getTextContent();
        if ("-1".equals(exito)) {
            return;
        }

        NodeList descNodes = doc.getElementsByTagName("desc");
        String desc = (descNodes.getLength() > 0) ? descNodes.item(0).getTextContent() : "error";
        throw new SmlProcessingException("SICAL error: " + desc);
    }

    private List<Entidad> parseDetalleList(NodeList detalleNodes) {
        List<Entidad> result = new ArrayList<>();
        
        for (int i = 0; i < detalleNodes.getLength(); i++) {
            String detalle = detalleNodes.item(i).getTextContent();
            if (detalle == null || detalle.isEmpty()) {
                continue;
            }
            
            Entidad ent = parseEntidadFromDetalle(detalle);
            if (ent != null) {
                result.add(ent);
            }
        }
        
        return result;
    }

    private Entidad parseEntidadFromDetalle(String detalle) {
        String[] parts = detalle.split("@", 2);
        String codigo = parts.length > 0 ? decodeBase64Safe(parts[0]) : "";
        String nombre = parts.length > 1 ? decodeBase64Safe(parts[1]) : "";
        
        if (codigo.isEmpty()) {
            return null;
        }
        
        Entidad ent = new Entidad();
        ent.setCodigo(codigo);
        ent.setNombre(nombre);
        return ent;
    }

    private String decodeBase64Safe(String b64) {
        if (b64 == null || b64.isEmpty()) return "";
        try {
            byte[] decoded = Base64.getDecoder().decode(b64);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return b64;
        }
    }
}