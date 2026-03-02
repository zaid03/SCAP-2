package com.example.backend.service;

import com.example.backend.dto.Entidad;
import com.example.sical.CryptoSical;
import org.springframework.stereotype.Service;
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

    public List<Entidad> getEntidades() throws Exception {
        CryptoSical.SecurityFields sec = CryptoSical.calculateSecurityFields(publicKey);

        // Build SML with required encodings: alphanumeric -> Base64, password -> SHA1+Base64
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

        System.out.println("=== SICAL REQUEST XML ===");
        System.out.println(xml);
        System.out.println("=== SICAL SOAP ENVELOPE ===");
        System.out.println(soapEnvelope);

        HttpEntity<String> request = new HttpEntity<>(soapEnvelope, headers);
        RestTemplate restTemplate = new RestTemplate();
        String responseXml = restTemplate.postForObject(wsUrl, request, String.class);

        System.out.println("=== SICAL RAW RESPONSE ===");
        System.out.println(responseXml);
        System.out.println("=== END RESPONSE ===");

        if (responseXml == null) {
            throw new RuntimeException("Empty response from SICAL");
        }

        // Extract inner servicioReturn content (escaped SML) and unescape it
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
            // fallback: try to parse the whole response (unlikely to work if escaped)
            return parseEntidades(responseXml);
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

    private List<Entidad> parseEntidades(String sml) throws Exception {
        List<Entidad> result = new ArrayList<>();
        if (sml == null || sml.isEmpty()) return result;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new java.io.ByteArrayInputStream(sml.getBytes(StandardCharsets.UTF_8)));

        // Check for success
        NodeList exitoNodes = doc.getElementsByTagName("exito");
        if (exitoNodes != null && exitoNodes.getLength() > 0) {
            String exito = exitoNodes.item(0).getTextContent();
            if (!"-1".equals(exito)) {
                // error case, return empty list or throw
                NodeList descNodes = doc.getElementsByTagName("desc");
                String desc = (descNodes.getLength() > 0) ? descNodes.item(0).getTextContent() : "error";
                throw new RuntimeException("SICAL error: " + desc);
            }
        }

        // detalle nodes under <operacion> / <l_operacion>
        NodeList detalleNodes = doc.getElementsByTagName("detalle");
        for (int i = 0; i < detalleNodes.getLength(); i++) {
            String detalle = detalleNodes.item(i).getTextContent();
            if (detalle == null || detalle.isEmpty()) continue;
            // detalle contains values separated by "@" : ENT_COD@ENT_NOM (Base64 encoded)
            String[] parts = detalle.split("@", 2);
            String codigo = parts.length > 0 ? decodeBase64Safe(parts[0]) : "";
            String nombre = parts.length > 1 ? decodeBase64Safe(parts[1]) : "";
            Entidad ent = new Entidad();
            ent.setCodigo(codigo);
            ent.setNombre(nombre);
            result.add(ent);
        }

        return result;
    }

    private String decodeBase64Safe(String b64) {
        if (b64 == null || b64.isEmpty()) return "";
        try {
            byte[] decoded = Base64.getDecoder().decode(b64);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // not base64 or malformed, return raw
            return b64;
        }
    }
}