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

    public String buildSmlInput(ContabilizacionRequestDto req, Fac fac, List<Fde> fdeList, List<Fdt> fdtList, String terAyt) throws Exception {
        String org = req.getOrg();
        String ent = req.getEnt();
        String eje = req.getEje();
        String usu = req.getUsu();
        String pwd = req.getPwd();
        String publicKey = req.getPublicKey();

        CryptoSical.SecurityFields sec = CryptoSical.calculateSecurityFields(publicKey);

        String fecha = sec.created;
        String nonce = sec.nonce;
        String token = sec.token;
        String tokenSha1 = CryptoSical.encodeSha1Base64(sec.origin);
        String pwdSha1Base64 = CryptoSical.encodeSha1Base64(pwd);

        String fechaContable = formatFechaContable(req.getFechaContable());

        String codope = "200";

        String numope = fac.getEJE() + "-" + fac.getFACNUM();

        StringBuilder sb = new StringBuilder();
        sb.append("<e>");
        sb.append("<ope>");
        sb.append("<apl>SNP</apl>");
        sb.append("<tobj>GenOpeGasto</tobj>");
        sb.append("<cmd>CRE</cmd>");
        sb.append("<ver>2.0</ver>");
        sb.append("</ope>");

        sb.append("<sec>");
        sb.append("<cli>SAGE-AYTOS</cli>");
        sb.append("<org>").append(org).append("</org>");
        sb.append("<ent>").append(ent).append("</ent>");
        sb.append("<eje>").append(eje).append("</eje>");
        sb.append("<usu>").append(usu).append("</usu>");
        sb.append("<pwd>").append(pwdSha1Base64).append("</pwd>");
        sb.append("<fecha>").append(fecha).append("</fecha>");
        sb.append("<nonce>").append(nonce).append("</nonce>");
        sb.append("<token>").append(token).append("</token>");
        sb.append("<tokenSha1>").append(tokenSha1).append("</tokenSha1>");
        sb.append("</sec>");

        sb.append("<par>");
        sb.append("<gensinalmacenar>0</gensinalmacenar>");

        sb.append("<l_operacion>");
        sb.append("<operacion>");
        sb.append("<prevdef>").append(CryptoSical.encodeBase64("P")).append("</prevdef>");
        sb.append("<numope>").append(numope).append("</numope>");  
        sb.append("<codope>").append(CryptoSical.encodeBase64(codope)).append("</codope>");
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
        
        sb.append("<usuope>").append(CryptoSical.encodeBase64(usu)).append("</usuope>");
        
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
        
        String tipContrato = (fac.getCONCTP() != null && !fac.getCONCTP().isEmpty()) ? fac.getCONCTP() : "Suministro";
        String proContrato = (fac.getCONCPR() != null && !fac.getCONCPR().isEmpty()) ? fac.getCONCPR() : "AdDirec";
        String criContrato = (fac.getCONCCR() != null && !fac.getCONCCR().isEmpty()) ? fac.getCONCCR() : "SinC";
        
        sb.append("<tipContrato>").append(CryptoSical.encodeBase64(tipContrato)).append("</tipContrato>");
        sb.append("<proContrato>").append(CryptoSical.encodeBase64(proContrato)).append("</proContrato>");
        sb.append("<criContrato>").append(CryptoSical.encodeBase64(criContrato)).append("</criContrato>");

        sb.append("<l_factura>");
        sb.append("</l_factura>");

        sb.append("<l_linea>");
        for (Fde fde : fdeList) {
            Double imp = (fde.getFDEIMP() != null ? fde.getFDEIMP() : 0.0) + 
                         (fde.getFDEDIF() != null ? fde.getFDEDIF() : 0.0);
            
            if (imp <= 0) {
                continue;
            }
            
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

        sb.append("<l_dto>");
        for (Fdt fdt : fdtList) {
            sb.append("<dto>");
            if (fdt.getFDTARE() != null) {
                sb.append("<areaD>").append(fdt.getFDTARE()).append("</areaD>");
            }
            sb.append("<ejeD>").append(eje).append("</ejeD>");
            if (fdt.getFDTORG() != null) {
                sb.append("<orgD>").append(CryptoSical.encodeBase64(fdt.getFDTORG())).append("</orgD>");
            }
            if (fdt.getFDTFUN() != null) {
                sb.append("<funD>").append(CryptoSical.encodeBase64(fdt.getFDTFUN())).append("</funD>");
            }
            if (fdt.getFDTECO() != null) {
                sb.append("<ecoD>").append(CryptoSical.encodeBase64(fdt.getFDTECO())).append("</ecoD>");
            }
            if (fdt.getFDTDTO() != null) {
                sb.append("<impD>").append(fdt.getFDTDTO()).append("</impD>");
            }
            if (fdt.getFDTBSE() != null) {
                sb.append("<baseRet>").append(fdt.getFDTBSE()).append("</baseRet>");
            }
            if (fdt.getFDTPRE() != null) {
                sb.append("<porcRet>").append(fdt.getFDTPRE()).append("</porcRet>");
            }
            if (fdt.getFDTTXT() != null) {
                sb.append("<textoD>").append(CryptoSical.encodeBase64(fdt.getFDTTXT())).append("</textoD>");
            }
            sb.append("</dto>");
        }
        sb.append("</l_dto>");

        sb.append("</operacion>");
        sb.append("</l_operacion>");
        sb.append("</par>");
        sb.append("</e>");

        return sb.toString();
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
            System.out.println("====== EXTRACTED SML ======");
            System.out.println(sml);
            System.out.println("====== END EXTRACTED SML ======");
            
            if (sml == null) {
                dto.setExito(false);
                dto.setMensaje("Respuesta SOAP inválida");
                return dto;
            }

            javax.xml.parsers.DocumentBuilderFactory dbFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            org.w3c.dom.Document doc = dBuilder.parse(new java.io.ByteArrayInputStream(sml.getBytes()));

            String exito = getTagValue(doc, "exito");
            System.out.println("Exito value: " + exito);
            
            
            if ("-1".equals(exito)) {
                dto.setExito(true);
                
                org.w3c.dom.NodeList opNodes = doc.getElementsByTagName("operacion");
                if (opNodes.getLength() > 0) {
                    org.w3c.dom.Node opNode = opNodes.item(0);
                    org.w3c.dom.NodeList children = opNode.getChildNodes();
                    
                    for (int i = 0; i < children.getLength(); i++) {
                        org.w3c.dom.Node child = children.item(i);
                        if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                            String name = child.getNodeName();
                            String value = child.getTextContent();
                            
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
                            }
                        }
                    }
                }
                dto.setMensaje("Operación generada correctamente");
            } else {
                dto.setExito(false);
                String desc = getTagValue(doc, "desc");
                String codigo = getTagValue(doc, "codigo");
                
                org.w3c.dom.NodeList errorNodes = doc.getElementsByTagName("error");
                StringBuilder errors = new StringBuilder();
                for (int i = 0; i < errorNodes.getLength(); i++) {
                    if (i > 0) errors.append("; ");
                    errors.append(errorNodes.item(i).getTextContent());
                }
                
                String mensaje = "";
                if (codigo != null && !codigo.isEmpty()) {
                    mensaje += "Código: " + codigo + ". ";
                }
                if (desc != null && !desc.isEmpty()) {
                    mensaje += desc;
                }
                if (errors.length() > 0) {
                    mensaje += " Errores: " + errors.toString();
                }
                dto.setMensaje(mensaje.isEmpty() ? "Error desconocido del servicio" : mensaje);
            }
        } catch (Exception e) {
            dto.setExito(false);
            dto.setMensaje("Error al procesar respuesta: " + e.getMessage());
            e.printStackTrace();
        }
        
        return dto;
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
        try {
            int start = soap.indexOf("<servicioReturn");
            if (start < 0) return null;
            start = soap.indexOf(">", start) + 1;
            int end = soap.indexOf("</servicioReturn>", start);
            if (end > start) {
                String sml = soap.substring(start, end)
                    .replace("&lt;", "<")
                    .replace("&gt;", ">");
                return sml;
            }
        } catch (Exception ignored) {}
        return null;
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