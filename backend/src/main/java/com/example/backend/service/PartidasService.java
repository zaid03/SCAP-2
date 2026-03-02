package com.example.backend.service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.example.backend.dto.Partida;
import com.example.sical.CryptoSical;

@Service
public class PartidasService {
    private static final Logger logger = LoggerFactory.getLogger(PartidasService.class);

    @Value("${sical.ws.url}")           
    private String wsUrl;
    
    @Value("${sical.username}")
    private String username;
    
    @Value("${sical.password}")
    private String password;
    
    @Value("${sical.public.key}")
    private String publicKey;
    
    @Value("${sical.org.code}")
    private String orgCode;

    @Value("${sical.entidad}")
    private String entidad;

    @Value("${sical.eje}")
    private String eje;

    public List<Partida> getPartidas(
        String cenges,
        String alias,
        String clorg,
        String clfun,
        String cleco,
        String clcte,
        String clpam,
        String usucenges
    ) throws Exception {
      CryptoSical.SecurityFields sec = CryptoSical.calculateSecurityFields(publicKey);

      String fecha = sec.created;
      String nonce = sec.nonce;
      String token = sec.token;
      String tokenSha1 = CryptoSical.encodeSha1Base64(sec.origin);

      String xml =
        "<e>" +
          "<ope><apl>SNP</apl><tobj>conPartidaGastos</tobj><cmd>LST</cmd><ver>2.0</ver></ope>" +
          "<sec>" +
            "<cli>SAGE-AYTOS</cli>" +
            "<org>" + orgCode + "</org>" +
            "<ent>" + entidad + "</ent>" +
            "<eje>" + eje + "</eje>" +
            "<usu>" + username + "</usu>" +
            "<pwd>" + CryptoSical.encodeSha1Base64(password) + "</pwd>" +
            "<fecha>" + fecha + "</fecha>" +
            "<nonce>" + nonce + "</nonce>" +
            "<token>" + token + "</token>" +
            "<tokenSha1>" + tokenSha1 + "</tokenSha1>" +
          "</sec>" +
          "<par>" +
            (cenges   != null ? "<cenges>"   + CryptoSical.encodeBase64(cenges)   + "</cenges>"   : "") +
            (alias    != null ? "<alias>"    + CryptoSical.encodeBase64(alias)    + "</alias>"    : "") +
            (clorg    != null ? "<clorg>"    + CryptoSical.encodeBase64(clorg)    + "</clorg>"    : "") +
            (clfun    != null ? "<clfun>"    + CryptoSical.encodeBase64(clfun)    + "</clfun>"    : "") +
            (cleco    != null ? "<cleco>"    + CryptoSical.encodeBase64(cleco)    + "</cleco>"    : "") +
           (clcte    != null ? "<clcte>"    + CryptoSical.encodeBase64(clcte)    + "</clcte>"    : "") +
            (clpam    != null ? "<clpam>"    + CryptoSical.encodeBase64(clpam)    + "</clpam>"    : "") +
            (usucenges!= null ? "<usucenges>"+ CryptoSical.encodeBase64(usucenges) + "</usucenges>" : "") +
          "</par>" +
        "</e>";

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
      headers.add(HttpHeaders.CONTENT_TYPE, "text/xml");
      headers.add(HttpHeaders.ACCEPT, "text/xml");
      headers.add("SOAPAction", "");

      HttpEntity<String> request = new HttpEntity<>(soapEnvelope, headers);

      RestTemplate restTemplate = new RestTemplate();
      String endpoint = (wsUrl != null && wsUrl.contains("?")) ? wsUrl.substring(0, wsUrl.indexOf("?")) : wsUrl;
      String responseXml = restTemplate.postForObject(endpoint, request, String.class);

      logger.debug("SICAL raw response for partidas: {}", responseXml);
      return parsePartidas(responseXml);
    }

    private List<Partida> parsePartidas(String xml) throws Exception {
        List<Partida> result = new ArrayList<>();

        String inner = null;
        int start = xml != null ? xml.indexOf("<servicioReturn") : -1;
        if (start >= 0) {
            int gt = xml.indexOf(">", start);
            int end = xml.indexOf("</servicioReturn>", gt);
            if (gt >= 0 && end >= 0) {
                inner = xml.substring(gt + 1, end);
            }
        }
        if (inner == null) inner = xml == null ? "" : xml;

        String sml = inner
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");

        logger.debug("SICAL SML payload (partidas): {}", sml);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(sml.getBytes(StandardCharsets.UTF_8)));

            // if SICAL returned an error, surface it
            NodeList exitoNodes = doc.getElementsByTagName("exito");
            if (exitoNodes.getLength() > 0) {
                String exito = exitoNodes.item(0).getTextContent();
                if (!"-1".equals(exito) && !"1".equals(exito)) { // consider -1 or 1 success depending on CI
                    String desc = "";
                    NodeList descNodes = doc.getElementsByTagName("desc");
                    if (descNodes.getLength() > 0) desc = descNodes.item(0).getTextContent();
                    logger.warn("SICAL returned error for partidas: exito={} desc={}", exito, desc);
                    throw new Exception("SICAL error: " + desc);
                }
            }

            NodeList partidaNodes = doc.getElementsByTagName("partida");
            for (int i = 0; i < partidaNodes.getLength(); i++) {
                Element e = (Element) partidaNodes.item(i);
                Partida p = new Partida();

                p.setAlias(getTagValue(e, "alias"));
                p.setEjeapl(getTagValue(e, "ejeapl"));
                p.setOrgapl(decodeOrNull(getTagValue(e, "orgapl")));
                p.setFunapl(decodeOrNull(getTagValue(e, "funapl")));
                p.setEcoapl(decodeOrNull(getTagValue(e, "ecoapl")));
                p.setPamapl(decodeOrNull(getTagValue(e, "pamapl")));
                p.setCteapl(decodeOrNull(getTagValue(e, "cteapl")));
                p.setDesc(decodeOrNull(getTagValue(e, "desc")));

                p.setCipocin(toDouble(getTagValue(e, "cipocin")));
                p.setModcred(toDouble(getTagValue(e, "modcred")));
                p.setCredextra(toDouble(getTagValue(e, "credextra")));
                p.setSupcred(toDouble(getTagValue(e, "supcred")));
                p.setAmpcred(toDouble(getTagValue(e, "ampcred")));
                p.setTranpos(toDouble(getTagValue(e, "tranpos")));
                p.setTranneg(toDouble(getTagValue(e, "tranneg")));
                p.setReminc(toDouble(getTagValue(e, "reminc")));
                p.setCreging(toDouble(getTagValue(e, "creging")));
                p.setBajanu(toDouble(getTagValue(e, "bajanu")));
                p.setCretot(toDouble(getTagValue(e, "cretot")));
                p.setCreret(toDouble(getTagValue(e, "creret")));
                p.setCrepend(toDouble(getTagValue(e, "crepend")));
                p.setGasauto(toDouble(getTagValue(e, "gasauto")));
                p.setAutdisp(toDouble(getTagValue(e, "autdisp")));
                p.setGascomp(toDouble(getTagValue(e, "gascomp")));
                p.setOblrec(toDouble(getTagValue(e, "oblrec")));
                p.setPagord(toDouble(getTagValue(e, "pagord")));
                p.setPagefe(toDouble(getTagValue(e, "pagefe")));
                p.setReinpag(toDouble(getTagValue(e, "reinpag")));
                p.setSdisp(toDouble(getTagValue(e, "sdisp")));
                p.setSvin(toDouble(getTagValue(e, "svin")));
                p.setSvinpre(toDouble(getTagValue(e, "svinpre")));

                result.add(p);
            }
            return result;
        } catch (Exception ex) {
            logger.error("Failed to parse SICAL partidas SML. SML payload:\n{}\nException: {}", sml, ex.getMessage());
            throw ex;
        }
    }

    private String getTagValue(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes == null || nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        return node != null ? node.getTextContent() : null;
    }

    private Double toDouble(String s) {
      if (s == null || s.trim().isEmpty()) return 0.0;
      try { return Double.parseDouble(s); } catch (NumberFormatException ex) { return 0.0; }
    }

    private String decodeOrNull(String value) {
      if (value == null || value.isBlank()) return null;
      try {
        return CryptoSical.decodeBase64(value);
      } catch (IllegalArgumentException ex) {
        logger.warn("failed to decode to base64 value [{}]: {}", value, ex.getMessage());
        return value;
      }
    }
}
