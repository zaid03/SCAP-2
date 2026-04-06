package com.example.backend.service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.example.backend.exception.XmlParsingException;

import com.example.backend.dto.Partida;
import com.example.sical.CryptoSical;

@Service
public class PartidasService {
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

    public static class SearchCriteria {
        public final String cenges;
        public final String alias;
        public final String clorg;
        public final String clfun;
        public final String cleco;
        public final String clcte;
        public final String clpam;
        public final String usucenges;

        private SearchCriteria(Builder builder) {
            this.cenges = builder.cenges;
            this.alias = builder.alias;
            this.clorg = builder.clorg;
            this.clfun = builder.clfun;
            this.cleco = builder.cleco;
            this.clcte = builder.clcte;
            this.clpam = builder.clpam;
            this.usucenges = builder.usucenges;
        }

        public static class Builder {
            private String cenges;
            private String alias;
            private String clorg;
            private String clfun;
            private String cleco;
            private String clcte;
            private String clpam;
            private String usucenges;

            public Builder cenges(String cenges) {
                this.cenges = cenges;
                return this;
            }

            public Builder alias(String alias) {
                this.alias = alias;
                return this;
            }

            public Builder clorg(String clorg) {
                this.clorg = clorg;
                return this;
            }

            public Builder clfun(String clfun) {
                this.clfun = clfun;
                return this;
            }

            public Builder cleco(String cleco) {
                this.cleco = cleco;
                return this;
            }

            public Builder clcte(String clcte) {
                this.clcte = clcte;
                return this;
            }

            public Builder clpam(String clpam) {
                this.clpam = clpam;
                return this;
            }

            public Builder usucenges(String usucenges) {
                this.usucenges = usucenges;
                return this;
            }

            public SearchCriteria build() {
                return new SearchCriteria(this);
            }
        }
    }

    public List<Partida> getPartidas(SearchCriteria criteria) throws Exception {
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
            (criteria.cenges   != null ? "<cenges>"   + CryptoSical.encodeBase64(criteria.cenges)   + "</cenges>"   : "") +
            (criteria.alias    != null ? "<alias>"    + CryptoSical.encodeBase64(criteria.alias)    + "</alias>"    : "") +
            (criteria.clorg    != null ? "<clorg>"    + CryptoSical.encodeBase64(criteria.clorg)    + "</clorg>"    : "") +
            (criteria.clfun    != null ? "<clfun>"    + CryptoSical.encodeBase64(criteria.clfun)    + "</clfun>"    : "") +
            (criteria.cleco    != null ? "<cleco>"    + CryptoSical.encodeBase64(criteria.cleco)    + "</cleco>"    : "") +
           (criteria.clcte    != null ? "<clcte>"    + CryptoSical.encodeBase64(criteria.clcte)    + "</clcte>"    : "") +
            (criteria.clpam    != null ? "<clpam>"    + CryptoSical.encodeBase64(criteria.clpam)    + "</clpam>"    : "") +
            (criteria.usucenges!= null ? "<usucenges>"+ CryptoSical.encodeBase64(criteria.usucenges) + "</usucenges>" : "") +
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

      return parsePartidas(responseXml);
    }

    private List<Partida> parsePartidas(String xml) throws SicalParseException {
        List<Partida> result = new ArrayList<>();
        
        String innerXml = extractInnerXmlContent(xml);
        String sml = unescapeXmlEntities(innerXml);
        
        if (sml == null) {
            sml = "";
        }
        if (sml.isEmpty()) {
            return result;
        }
        
        try {
            Document doc = parseXmlDocument(sml);
            validateAndThrowIfError(doc);
            
            NodeList partidaNodes = doc.getElementsByTagName("partida");
            for (int i = 0; i < partidaNodes.getLength(); i++) {
                Element e = (Element) partidaNodes.item(i);
                Partida p = createPartidaFromElement(e);
                result.add(p);
            }
        } catch (XmlParsingException ex) {
            throw new SicalParseException("XML parsing error: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new SicalParseException("Error processing response: " + ex.getMessage(), ex);
        }
        
        return result;
    }
    
    private String extractInnerXmlContent(String xml) {
        if (xml == null) {
            return "";
        }
        
        int start = xml.indexOf("<servicioReturn");
        if (start < 0) {
            return xml;
        }
        
        int gt = xml.indexOf(">", start);
        int end = xml.indexOf("</servicioReturn>", gt);
        
        if (gt >= 0 && end >= 0) {
            return xml.substring(gt + 1, end);
        }
        
        return xml;
    }
    
    private String unescapeXmlEntities(String xml) {
        return xml.replace("&lt;", "<")
                  .replace("&gt;", ">")
                  .replace("&quot;", "\"")
                  .replace("&apos;", "'");
    }
    
    private Document parseXmlDocument(String sml) throws com.example.backend.exception.XmlParsingException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(sml.getBytes(StandardCharsets.UTF_8)));
        } catch (javax.xml.parsers.ParserConfigurationException | org.xml.sax.SAXException | java.io.IOException ex) {
            throw new com.example.backend.exception.XmlParsingException("Failed to parse XML document", ex);
        }
    }
    
    private void validateAndThrowIfError(Document doc) throws SicalParseException {
        NodeList exitoNodes = doc.getElementsByTagName("exito");
        if (exitoNodes.getLength() == 0) {
            return;
        }
        
        String exito = exitoNodes.item(0).getTextContent();
        if ("-1".equals(exito) || "1".equals(exito)) {
            return;
        }
        
        String desc = "";
        NodeList descNodes = doc.getElementsByTagName("desc");
        if (descNodes.getLength() > 0) {
            desc = descNodes.item(0).getTextContent();
        }
        throw new SicalParseException("SICAL error: " + desc);
    }
    
    private Partida createPartidaFromElement(Element e) {
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
        
        return p;
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
        return value;
      }
    }

  public static class SicalParseException extends Exception {
    public SicalParseException(String message) {
      super(message);
    }
    
    public SicalParseException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}