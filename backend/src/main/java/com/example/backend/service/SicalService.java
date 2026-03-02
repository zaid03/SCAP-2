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
import org.w3c.dom.NodeList;

import com.example.backend.dto.Tercero;
import com.example.sical.CryptoSical;

import java.util.regex.Pattern;
import java.util.Arrays;
import org.apache.commons.text.StringEscapeUtils;

@Service
public class SicalService {

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

    public List<Tercero> getTerceros(String nif, String nom, String apell) throws Exception {
      CryptoSical.SecurityFields sec = CryptoSical.calculateSecurityFields(publicKey);

      String fecha = sec.created;
      String nonce = sec.nonce;
      String token = sec.token;
      String tokenSha1 = CryptoSical.encodeSha1Base64(sec.origin);

      String xml =
          "<e>" +
            "<ope><apl>SNP</apl><tobj>TercerosyCuentas</tobj><cmd>LST</cmd><ver>2.0</ver></ope>" +
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
              "<ruta></ruta>" +                                      
              "<l_tercero>" +
                "<tercero>" +
                  "<portal>S</portal>" + 
                  "<idenTercero></idenTercero>" +
                  (nif   != null ? "<NIFtercero>"   + CryptoSical.encodeBase64(nif)   + "</NIFtercero>"   : "") +
                  (nom   != null ? "<nomTercero>"   + CryptoSical.encodeBase64(nom)   + "</nomTercero>"   : "") +
                  (apell != null ? "<apellTercero>" + CryptoSical.encodeBase64(apell) + "</apellTercero>" : "") +
                  "<indice>0</indice>" +
                  "<NumRegDev>50</NumRegDev>" +
                "</tercero>" +
              "</l_tercero>" +
            "</par>"+
          "</e>";

      System.out.println("=== USERNAME SENT: " + username);
      System.out.println("=== PASSWORD (plain): " + password);
      System.out.println("=== PASSWORD (SHA1): " + CryptoSical.encodeSha1Base64(password));
      System.out.println("=== XML BEING SENT ===");
      System.out.println(xml);
      System.out.println("=== END XML ===");

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
      String responseXml = restTemplate.postForObject(wsUrl, request, String.class);

      System.out.println("=== SICAL RAW RESPONSE ===");
      System.out.println(responseXml);
      System.out.println("=== END RESPONSE ===");

      return parseTerceros(responseXml);
  }

    private List<Tercero> parseTerceros(String xml) throws Exception {
        List<Tercero> result = new ArrayList<>();

        String inner = null;
        int start = xml != null ? xml.indexOf("<servicioReturn") : -1;
        if (start >= 0) {
            int gt = xml.indexOf(">", start);
            int end = xml.indexOf("</servicioReturn>", gt);
            if (gt >= 0 && end >= 0) {
                inner = xml.substring(gt + 1, end);
            }
        }
        if (inner == null) {
            inner = xml == null ? "" : xml;
        }

        String sml = inner
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(sml.getBytes(StandardCharsets.UTF_8)));

        NodeList tercerosNodes = doc.getElementsByTagName("tercero");

        for (int i = 0; i < tercerosNodes.getLength(); i++) {
            Element e = (Element) tercerosNodes.item(i);
            Tercero t = new Tercero();

            NodeList detterNodes = e.getElementsByTagName("detter");
            if (detterNodes.getLength() > 0) {
                String detter = detterNodes.item(0).getTextContent();
                String[] parts = detter.split(Pattern.quote("-@-"), -1);

                t.setIdenTercero(unescapePart(parts, 0));          
                t.setNIFtercero(unescapePart(parts, 1));         
                t.setTipoDocumento(unescapePart(parts, 2));      
                t.setAlias(unescapePart(parts, 3));            
                t.setNomTercero(unescapePart(parts, 4));         
                t.setDomicilio(unescapePart(parts, 5));          
                t.setPoblacion(unescapePart(parts, 6));           
                t.setCodigoPostal(unescapePart(parts, 7));        
                t.setProvincia(unescapePart(parts, 8));          
                t.setTelefono(unescapePart(parts, 9));          
                t.setFax(unescapePart(parts, 10));               
                t.setTipoTercero(unescapePart(parts, 11));        
                t.setObservaciones(unescapePart(parts, 18));    
                t.setEmbargado(unescapePart(parts, 19));         
                t.setEmail(unescapePart(parts, 20));             
                t.setNombreCompleto(unescapePart(parts, 21));    
                t.setApellido1(unescapePart(parts, 22));         
                t.setApellido2(unescapePart(parts, 23));       

                if ((t.getApellTercero() == null || t.getApellTercero().isEmpty())) {
                    String a1 = t.getApellido1();
                    String a2 = t.getApellido2();
                    if ((a1 != null && !a1.isEmpty()) || (a2 != null && !a2.isEmpty())) {
                        String combined = ((a1 == null ? "" : a1) + " " + (a2 == null ? "" : a2)).trim();
                        t.setApellTercero(combined);
                    } else if (t.getNombreCompleto() != null && !t.getNombreCompleto().isEmpty()) {
                        String[] nm = t.getNombreCompleto().trim().split("\\s+");
                        if (nm.length > 1) {
                            t.setApellTercero(nm[nm.length - 1]);
                            t.setNomTercero(String.join(" ", Arrays.copyOfRange(nm, 0, nm.length - 1)));
                        } else {
                            t.setNomTercero(t.getNombreCompleto());
                        }
                    }
                }

            } else {
                t.setIdenTercero(getTagValue(e, "idenTercero"));
                t.setNIFtercero(getTagValue(e, "NIFtercero"));
                t.setNomTercero(getTagValue(e, "nomTercero"));
                t.setApellTercero(getTagValue(e, "apellTercero"));
            }
            result.add(t);
        }

        return result;
    }

   private String getTagValue(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    private String unescapePart(String[] parts, int idx) {
        if (idx < 0 || idx >= parts.length) return null;
        String v = parts[idx];
        if (v == null) return null;
        return StringEscapeUtils.unescapeXml(v.trim());
    }
}
