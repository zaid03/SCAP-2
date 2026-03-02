package com.example.backend.controller;

import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.DepCodDesDto;
import com.example.backend.dto.PersonaDto;
import com.example.backend.dto.PersonaServiceRequest;
import com.example.backend.dto.ServicePersonaRequest;
import com.example.backend.dto.personasPorServiciosProjection;
import com.example.backend.service.DpePersonasForService;
import com.example.backend.service.DpeService;
import com.example.backend.sqlserver2.model.Dep;
import com.example.backend.sqlserver2.model.Dpe;
import com.example.backend.sqlserver2.model.DpeId;
import com.example.backend.sqlserver2.model.Per;
import com.example.backend.sqlserver2.repository.DepRepository;
import com.example.backend.sqlserver2.repository.DpeRepository;
import com.example.backend.sqlserver2.repository.PerRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/depe")
public class DpeController {
    @Autowired
    private DpeRepository dpeRepository;
    @Autowired
    private PerRepository perRepository;
    @Autowired DepRepository depRepository;

    //for adding personas to services and vice versa
    private final DpeService dpeService;
    private final DpePersonasForService dpePersonasForService;

    public DpeController(DpeService dpeService, DpePersonasForService dpePersonasForService) {
        this.dpeService = dpeService;
        this.dpePersonasForService = dpePersonasForService;
    }

    //selecting personas for servicios
    @GetMapping("/fetch-service-personas/{ent}/{eje}/{depcod}")
    public ResponseEntity<?> fetchAllservices(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable String depcod
    ) {
        try {
            List<Dpe> dpes = dpeRepository.findByENTAndEJEAndDEPCOD(ent, eje, depcod);

            List<String> percods = dpes.stream()
                .map(Dpe::getPERCOD)
                .distinct()
                .toList();

            List<Per> personas = perRepository.findByPERCODIn(percods);

            List<PersonaDto> result = personas.stream()
                .map(p -> new PersonaDto(p.getPERCOD(), p.getPERNOM()))
                .toList();
            return ResponseEntity.ok(result);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    } 

    //deleting a persona from a service
    @DeleteMapping("/delete-persona-service/{ent}/{eje}/{depcod}/{percod}")
    public ResponseEntity<?> fetchAllservices(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable String depcod,
        @PathVariable String percod
    ) {
        try {
            DpeId id = new DpeId(ent, eje, depcod, percod);
            if(!dpeRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Sin resultado");
            }

            dpeRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //selecting a persona's services
    @GetMapping("/fetch-persona-service/{ent}/{eje}/{percod}")
    public ResponseEntity<?> fetchPersonaService(
         @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable String percod
    ) {
        try{
            List<Dpe> dpes = dpeRepository.findByENTAndEJEAndPERCOD(ent, eje, percod);
            if (dpes.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }

            List<String> depcods = dpes.stream()
            .map(Dpe::getDEPCOD)
            .distinct()
            .toList();

            List<Dep> deps = depRepository.findByENTAndEJEAndDEPCODIn(ent, eje, depcods);

            List<DepCodDesDto> result = deps.stream()
                .map(d -> new DepCodDesDto(d.getDEPCOD(), d.getDEPDES()))
                .toList();

            return ResponseEntity.ok(result);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //deleting a persona's services
    @DeleteMapping("/delete-service-persona/{ent}/{eje}/{depcod}/{percod}")
    public ResponseEntity<?> deleteService(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable String depcod,
        @PathVariable String percod
    ) {
        try {
            DpeId id = new DpeId(ent, eje, depcod, percod);
            if(!dpeRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Sin resultado");
            }

            dpeRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //adding serivces for a person
    @PostMapping("/add-persona-services")
    public ResponseEntity<?> addPersonaServices(@RequestBody PersonaServiceRequest request) {
        try {
            if (request.getPercod() == null || request.getPercod().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Falta un dato obligatorio");
            }
            if (request.getServices() == null || request.getServices().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Debe seleccionar al menos un servicio.");
            }

            dpeService.savePersonaServices(request);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMessage());
        }
    }

    //needed for copy perfil function
    @DeleteMapping("/delete-persona-Allservice/{ent}/{eje}/{percod}")
    public ResponseEntity<?> deleteServices(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable String percod
    ) {
        try {
            List<Dpe> existing = dpeRepository.findByENTAndEJEAndPERCOD(ent, eje, percod);
            if (existing.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }

            int deletedd = dpeRepository.deleteByENTAndEJEAndPERCOD(ent, eje, percod);
            if (deletedd == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }
            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error durante la eliminación: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //adding serivces for a person
    @PostMapping("/add-services-persona")
    public ResponseEntity<?> addServicePersonas(@RequestBody ServicePersonaRequest request) {
        try {
            if (request.getDepcod() == null || request.getDepcod().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El código de servicio es obligatorio.");
            }
            if (request.getPersonas() == null || request.getPersonas().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Debe seleccionar al menos un persona.");
            }

            dpePersonasForService.saveServicePersonas(request);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMessage());
        }
    }

    //selecting personas por servicios
    @GetMapping("/personas-servicios/{ent}/{eje}/{page}")
    public ResponseEntity<?> fetchPersonasServicios(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable Integer page
    ) {
        try {
            int size = 20;
            Pageable pageable = PageRequest.of(page, size);
            List<personasPorServiciosProjection> personas = dpeRepository.findByENTAndEJE(ent, eje, pageable);

            if (personas.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            return ResponseEntity.ok(personas);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMessage());
        }
    }

    //downloading as excel
    @GetMapping("/personas-servicios/excel/{ent}/{eje}")
    public void exportPersonasServiciosExcel(
        @PathVariable Integer ent,
        @PathVariable String eje,
        HttpServletResponse response
    ) {
        try {
            List<personasPorServiciosProjection> personas = dpeRepository.findByENTAndEJE(ent, eje);
            if (personas.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=personas_por_servicios.xlsx");

            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Personas por Servicios");
                String[] columns = {
                    "#", "Cód Persona", "Nombre", "Cód Servicio", "Servicio",
                    "Almacén/Farmacia", "Comprador", "Contable", "Cód Centro Gestor", "Nombre Centro Gestor"
                };
                Row header = sheet.createRow(0);
                for (int i = 0; i < columns.length; i++) {
                    header.createCell(i).setCellValue(columns[i]);
                }
                int rowIdx = 1;
                for (personasPorServiciosProjection p : personas) {
                    Row row = sheet.createRow(rowIdx);
                    row.createCell(0).setCellValue(rowIdx);
                    row.createCell(1).setCellValue(p.getPERCOD() != null ? p.getPERCOD() : "");
                    row.createCell(2).setCellValue(p.getPer() != null && p.getPer().getPERNOM() != null ? p.getPer().getPERNOM() : "");
                    row.createCell(3).setCellValue(p.getDEPCOD() != null ? p.getDEPCOD() : "");
                    row.createCell(4).setCellValue(p.getDep() != null && p.getDep().getDEPDES() != null ? p.getDep().getDEPDES() : "");
                    row.createCell(5).setCellValue(
                        p.getDep() != null && p.getDep().getDEPALM() != null && p.getDep().getDEPALM() == 1 ? "Sí" : "No"
                    );
                    row.createCell(6).setCellValue(
                        p.getDep() != null && p.getDep().getDEPCOM() != null && p.getDep().getDEPCOM() == 1 ? "Sí" : "No"
                    );
                    row.createCell(7).setCellValue(
                        p.getDep() != null && p.getDep().getDEPINT() != null && p.getDep().getDEPINT() == 1 ? "Sí" : "No"
                    );
                    row.createCell(8).setCellValue(
                        p.getDep() != null && p.getDep().getCge() != null && p.getDep().getCge().getCGECOD() != null ? p.getDep().getCge().getCGECOD() : ""
                    );
                    row.createCell(9).setCellValue(
                        p.getDep() != null && p.getDep().getCge() != null && p.getDep().getCge().getCGEDES() != null ? p.getDep().getCge().getCGEDES() : ""
                    );
                    rowIdx++;
                }
                for (int i = 0; i < columns.length; i++) {
                    sheet.autoSizeColumn(i);
                }
                workbook.write(response.getOutputStream());
            }
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    //downloading as pdf
    @GetMapping("/personas-servicios/pdf/{ent}/{eje}")
    public void exportPersonasServiciosPdf(
        @PathVariable Integer ent,
        @PathVariable String eje,
        HttpServletResponse response
    ) {
        try {
            List<personasPorServiciosProjection> personas = dpeRepository.findByENTAndEJE(ent, eje);
            if (personas.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            StringBuilder htmlRows = new StringBuilder();
            int idx = 1;
            for (personasPorServiciosProjection p : personas) {
                htmlRows.append("<tr>")
                    .append("<td>").append(idx).append("</td>")
                    .append("<td class='servicio-col'>").append(p.getPERCOD() != null ? p.getPERCOD() : "").append("</td>")
                    .append("<td>").append(p.getPer() != null && p.getPer().getPERNOM() != null ? p.getPer().getPERNOM() : "").append("</td>")
                    .append("<td>").append(p.getDEPCOD() != null ? p.getDEPCOD() : "").append("</td>")
                    .append("<td class='servicio-col'>").append(p.getDep() != null && p.getDep().getDEPDES() != null ? p.getDep().getDEPDES() : "").append("</td>")
                    .append("<td>").append(p.getDep() != null && p.getDep().getDEPALM() != null && p.getDep().getDEPALM() == 1 ? "Sí" : "No").append("</td>")
                    .append("<td>").append(p.getDep() != null && p.getDep().getDEPCOM() != null && p.getDep().getDEPCOM() == 1 ? "Sí" : "No").append("</td>")
                    .append("<td>").append(p.getDep() != null && p.getDep().getDEPINT() != null && p.getDep().getDEPINT() == 1 ? "Sí" : "No").append("</td>")
                    .append("<td>").append(p.getDep() != null && p.getDep().getCge() != null && p.getDep().getCge().getCGECOD() != null ? p.getDep().getCge().getCGECOD() : "").append("</td>")
                    .append("<td>").append(p.getDep() != null && p.getDep().getCge() != null && p.getDep().getCge().getCGEDES() != null ? p.getDep().getCge().getCGEDES() : "").append("</td>")
                    .append("</tr>");
                idx++;
            }

            String html = "<html><head><style>"
                + "@page { size: A4 landscape; margin: 1px; }"
                + ".servicio-col { width: 10px !important;}"
                + "body { font-family: 'Poppins', sans-serif; padding: 24px; }"
                + "h1 { text-align: center; margin-bottom: 16px; }"
                + "table { width: 100%; border-collapse: collapse; }"
                + "th, td { border: 1px solid #ccc; padding: 8px 12px; text-align: left; }"
                + "th { background: #f3f4f6; }"
                + "th:last-child, td:last-child { width: 180px; }"
                + "</style></head><body>"
                + "<h1>listas de servicios</h1>"
                + "<table><thead><tr>"
                + "<th>#</th><th class='servicio-col'>C.Persona</th><th>Nombre</th><th>C.Servicio</th>"
                + "<th class='servicio-col'>Servicio</th><th>Alma</th><th>Com</th><th>Con</th>"
                + "<th>C.C.Gestor</th><th>N.Gestor</th>"
                + "</tr></thead><tbody>"
                + htmlRows
                + "</tbody></table></body></html>";

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=personas_por_servicios.pdf");

            try (var os = response.getOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.withHtmlContent(html, null);
                builder.toStream(os);
                builder.run();
            }
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    //search in personas de servicio
    @GetMapping("/personas-servicios/search")
    public ResponseEntity<?> searchPersonasServicios(
        @RequestParam Integer ent,
        @RequestParam String eje,
        @RequestParam(required = false) String servicio,
        @RequestParam(required = false) String persona,
        @RequestParam(required = false) String cgecod,
        @RequestParam(required = false) String perfil
    ) {
        try {
            List<personasPorServiciosProjection> result = null;

            Integer perfilValue = null;
            String perfilType = null;
            if (perfil != null && !perfil.isEmpty()) {
                switch (perfil.toLowerCase()) {
                    case "almacen": perfilType = "depalm"; perfilValue = 1; break;
                    case "comprador": perfilType = "depcom"; perfilValue = 1; break;
                    case "contabilidad": perfilType = "depint"; perfilValue = 1; break;
                    case "peticionario": perfilType = "peticionario"; break;
                    case "todos": default: break;
                }
            }

            boolean hasServicio = servicio != null && !servicio.isEmpty();
            boolean hasPersona = persona != null && !persona.isEmpty();
            boolean hasCgecod = cgecod != null && !cgecod.isEmpty();
            boolean hasPerfil = perfilType != null && !"todos".equals(perfil);

            if (hasServicio && hasPersona && hasCgecod && hasPerfil && !"peticionario".equals(perfilType)) {
                if ("depalm".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_DEPDESContainingAndPer_PERNOMContainingAndDep_Cge_CGECODAndDep_DEPALM(ent, eje, servicio, persona, cgecod, perfilValue);
                } else if ("depcom".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_DEPDESContainingAndPer_PERNOMContainingAndDep_Cge_CGECODAndDep_DEPCOM(ent, eje, servicio, persona, cgecod, perfilValue);
                } else if ("depint".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_DEPDESContainingAndPer_PERNOMContainingAndDep_Cge_CGECODAndDep_DEPINT(ent, eje, servicio, persona, cgecod, perfilValue);
                }
            }
            else if (hasServicio && hasPersona && hasCgecod) {
                result = dpeRepository.findByENTAndEJEAndDep_DEPDESContainingAndPer_PERNOMContainingAndDep_Cge_CGECOD(ent, eje, servicio, persona, cgecod);
            }
            else if (hasServicio && hasPersona && hasPerfil && !"peticionario".equals(perfilType)) {
                if ("depalm".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_DEPDESContainingAndPer_PERNOMContainingAndDep_DEPALM(ent, eje, servicio, persona, perfilValue);
                } else if ("depcom".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_DEPDESContainingAndPer_PERNOMContainingAndDep_DEPCOM(ent, eje, servicio, persona, perfilValue);
                } else if ("depint".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_DEPDESContainingAndPer_PERNOMContainingAndDep_DEPINT(ent, eje, servicio, persona, perfilValue);
                }
            }
            else if (hasServicio && hasCgecod && hasPerfil && !"peticionario".equals(perfilType)) {
                if ("depalm".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_DEPDESContainingAndDep_Cge_CGECODAndDep_DEPALM(ent, eje, servicio, cgecod, perfilValue);
                } else if ("depcom".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_DEPDESContainingAndDep_Cge_CGECODAndDep_DEPCOM(ent, eje, servicio, cgecod, perfilValue);
                } else if ("depint".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_DEPDESContainingAndDep_Cge_CGECODAndDep_DEPINT(ent, eje, servicio, cgecod, perfilValue);
                }
            }
            else if (hasPersona && hasCgecod && hasPerfil && !"peticionario".equals(perfilType)) {
                if ("depalm".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndPer_PERNOMContainingAndDep_Cge_CGECODAndDep_DEPALM(ent, eje, persona, cgecod, perfilValue);
                } else if ("depcom".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndPer_PERNOMContainingAndDep_Cge_CGECODAndDep_DEPCOM(ent, eje, persona, cgecod, perfilValue);
                } else if ("depint".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndPer_PERNOMContainingAndDep_Cge_CGECODAndDep_DEPINT(ent, eje, persona, cgecod, perfilValue);
                }
            }
            else if (hasServicio && hasPersona) {
                result = dpeRepository.findByENTAndEJEAndDep_DEPDESContainingAndPer_PERNOMContaining(ent, eje, servicio, persona);
            }
            else if (hasServicio && hasCgecod) {
                result = dpeRepository.findByENTAndEJEAndDep_DEPDESContainingAndDep_Cge_CGECOD(ent, eje, servicio, cgecod);
            }
            else if (hasServicio && hasPerfil && !"peticionario".equals(perfilType)) {
                if ("depalm".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_DEPDESContainingAndDep_DEPALM(ent, eje, servicio, perfilValue);
                } else if ("depcom".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_DEPDESContainingAndDep_DEPCOM(ent, eje, servicio, perfilValue);
                } else if ("depint".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_DEPDESContainingAndDep_DEPINT(ent, eje, servicio, perfilValue);
                }
            }
            else if (hasPersona && hasCgecod) {
                result = dpeRepository.findByENTAndEJEAndPer_PERNOMContainingAndDep_Cge_CGECOD(ent, eje, persona, cgecod);
            }
            else if (hasPersona && hasPerfil && !"peticionario".equals(perfilType)) {
                if ("depalm".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndPer_PERNOMContainingAndDep_DEPALM(ent, eje, persona, perfilValue);
                } else if ("depcom".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndPer_PERNOMContainingAndDep_DEPCOM(ent, eje, persona, perfilValue);
                } else if ("depint".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndPer_PERNOMContainingAndDep_DEPINT(ent, eje, persona, perfilValue);
                }
            }
            else if (hasCgecod && hasPerfil && !"peticionario".equals(perfilType)) {
                if ("depalm".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_Cge_CGECODAndDep_DEPALM(ent, eje, cgecod, perfilValue);
                } else if ("depcom".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_Cge_CGECODAndDep_DEPCOM(ent, eje, cgecod, perfilValue);
                } else if ("depint".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_Cge_CGECODAndDep_DEPINT(ent, eje, cgecod, perfilValue);
                }
            }
            else if (hasServicio) {
                result = dpeRepository.findByENTAndEJEAndDep_DEPDESContaining(ent, eje, servicio);
                if (result == null || result.isEmpty()) {
                    result = dpeRepository.findByENTAndEJEAndDEPCODContaining(ent, eje, servicio);
                }
            }
            else if (hasPersona) {
                result = dpeRepository.findByENTAndEJEAndPer_PERNOMContaining(ent, eje, persona);
                if (result == null || result.isEmpty()) {
                    result = dpeRepository.findProjectionByENTAndEJEAndPERCOD(ent, eje, persona);
                }
            }
            else if (hasCgecod) {
                result = dpeRepository.findByENTAndEJEAndDep_Cge_CGECOD(ent, eje, cgecod);
            }
            else if (hasPerfil) {
                if ("peticionario".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_DEPALMAndDep_DEPCOMAndDep_DEPINT(ent, eje, 0, 0, 0);
                } else if ("depalm".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_DEPALM(ent, eje, perfilValue);
                } else if ("depcom".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_DEPCOM(ent, eje, perfilValue);
                } else if ("depint".equals(perfilType)) {
                    result = dpeRepository.findByENTAndEJEAndDep_DEPINT(ent, eje, perfilValue);
                }
            }

            if (result == null || result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }
            return ResponseEntity.ok(result);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }
}