package com.example.backend.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.backend.service.FacContabilizacionSpecification;
import com.example.backend.service.FacSpecification;
import com.example.backend.service.FacturaInsertService;
import com.example.backend.sqlserver2.model.Fde;
import com.example.backend.sqlserver2.model.Gbs;
import com.example.backend.sqlserver2.model.Fac;
import com.example.backend.sqlserver2.model.FacId;
import com.example.backend.sqlserver2.model.Ter;
import com.example.backend.sqlserver2.repository.FacRepository;
import com.example.backend.sqlserver2.repository.TerRepository;
import com.example.backend.sqlserver2.repository.FdeRepository;
import com.example.backend.sqlserver2.repository.GbsRepository;
import com.example.backend.dto.FacWithTerDto;
import com.example.backend.dto.FacWithTerProjection;
import com.example.backend.dto.FacturaInsertDto;

@RestController
@RequestMapping("/api/fac")
public class FacController {
    @Autowired
    private FacRepository facRepository;
    @Autowired
    private TerRepository terRepository;
    @Autowired
    private FacturaInsertService facturaInsertService;
    @Autowired
    private FdeRepository fdeRepository;
    @Autowired
    private GbsRepository gbsRepository;

    //for the main list
    @GetMapping("/{ent}/{eje}/{cgecod}")
    public ResponseEntity<?> getFacturas(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable String cgecod
    ) {
        try {
            List<FacWithTerProjection> facturas = facRepository.findByENTAndEJEAndCGECODOrderByFACFREAsc(ent, eje, cgecod);
            if (facturas.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }
            
            return ResponseEntity.ok(facturas);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //search in gestion de factura
    @GetMapping("/search")
    public ResponseEntity<?> searchFacturas(
        @RequestParam Integer ent,
        @RequestParam String eje,
        @RequestParam String cgecod,
        @RequestParam(defaultValue = "TODAS") String estado,
        @RequestParam(defaultValue = "REGISTRO") String dateType,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(defaultValue = "ANY") String facannMode,
        @RequestParam(required = false) String facann,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "OTROS") String searchType
    ) {
        try {
            Specification<Fac> spec = FacSpecification.searchFacturas(
                ent, eje, cgecod, estado, dateType, fromDate, toDate,
                facannMode, facann, search, searchType
            );
            
            List<Fac> facturas = facRepository.findAll(spec);
            
            List<FacWithTerDto> result = facturas.stream().map(f -> {
                Ter ter = f.getTer();
                return new FacWithTerDto(
                    f.getENT(), f.getEJE(), f.getFACNUM(), f.getTERCOD(), f.getCGECOD(),
                    f.getFACOBS(), f.getFACIMP(), f.getFACIEC(), f.getFACIDI(), f.getFACTDC(),
                    f.getFACANN() != null ? String.valueOf(f.getFACANN()) : null,
                    f.getFACFAC() != null ? String.valueOf(f.getFACFAC()) : null,
                    f.getFACDOC(), f.getFACDAT(), f.getFACFCO(), f.getFACADO(),
                    f.getFACTXT(), f.getFACFRE(), f.getCONCTP(), f.getCONCPR(),
                    f.getCONCCR(), f.getFACOCT(), f.getFACFPG(), f.getFACOPG(),
                    f.getFACTPG(), f.getFACDTO(),
                    ter != null ? ter.getTERNOM() : null,
                    ter != null ? ter.getTERNIF() : null
                );
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMessage());
        }
    }

    //adding a factura
    @PostMapping("/add-facturas")
    public ResponseEntity<?> addFacturas(
        @RequestBody List<FacturaInsertDto> facturas
    ) {
        try {
            List<String> messages = facturaInsertService.insertFacturas(facturas);
            return ResponseEntity.ok(messages);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + ex.getMessage());
        }
    }

    //modifying a factura
    public record facturaUpdate(String FACOBS, String CONCTP, String CONCPR, String CONCCR, LocalDateTime FACFRE, String FACFPG, String FACOPG, String FACTPG, Integer FACOCT) {}
    @PatchMapping("/update-factura/{ent}/{eje}/{facnum}")
    public ResponseEntity<?> updateFactura(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable Integer facnum,
        @RequestBody facturaUpdate payload
    ) {
        try {
            if (payload == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Falta un dato obligatorio");
            }

            FacId id = new FacId(ent, eje, facnum);
            Optional<Fac> facOptio = facRepository.findById(id);
            if (facOptio.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }

            Fac factura = facOptio.get();
            factura.setFACOBS(payload.FACOBS());
            factura.setCONCTP(payload.CONCTP());
            factura.setCONCPR(payload.CONCPR());
            factura.setCONCCR(payload.CONCCR());
            factura.setFACFRE(payload.FACFRE());
            factura.setFACFPG(payload.FACFPG());
            factura.setFACOPG(payload.FACOPG());
            factura.setFACTPG(payload.FACTPG());
            factura.setFACOCT(payload.FACOCT());
            facRepository.save(factura);

            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + ex.getMessage());
        }
    }

    //search in contabilizacion
    @GetMapping("/contabilizacion/search")
    public ResponseEntity<?> searchContabilizacion(
        @RequestParam Integer ent,
        @RequestParam String eje,
        @RequestParam String cgecod,
        @RequestParam(defaultValue = "registro") String fechaType,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
        @RequestParam(required = false) Integer facann,
        @RequestParam(required = false) String search 
    ) {
        try {
            Specification<Fac> spec = FacContabilizacionSpecification.searchContabilizacion(
                ent, eje, cgecod, fechaType, desde, hasta, facann, search
            );

            List<Fac> facturas = facRepository.findAll(spec);

            if (facturas.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            return ResponseEntity.ok(facturas);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //contabilizar a factura
    public record contabilizar(Integer ENT, String EJE, Integer FACNUM, String FACADO, LocalDateTime FACFCO, String CGECOD, Boolean ESCONTRATO) {}
    @PatchMapping("/contabilizar-facturas")
    public ResponseEntity<?> contabilizarFactura(
        @RequestBody contabilizar payload
    ) {
        try {
            if (payload == null || payload.ENT() == null || payload.EJE() == null || payload.FACNUM() == null || payload.FACADO() == null || payload.FACFCO() == null || payload.CGECOD() == null || payload.ESCONTRATO() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Falta un dato obligatorio");
            }

            FacId id = new FacId(payload.ENT(), payload.EJE(), payload.FACNUM());
            Optional<Fac> facturaOptio = facRepository.findById(id);

            if (facturaOptio.isPresent()) {
                Fac factura = facturaOptio.get();
                factura.setFACADO(payload.FACADO());
                factura.setFACFCO(payload.FACFCO());
                facRepository.save(factura);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Factura no encontrada"); 
            }

            if (!payload.ESCONTRATO()) {
                List<Fde> applicacionesList = fdeRepository.findByENTAndEJEAndFACNUM(payload.ENT(), payload.EJE(), payload.FACNUM());

                List<String> errores = new ArrayList<>(); 

                Double newGbsius;
                Double newGbsiut;
                Double fdeimp;
                Double fdedif;
                for (Fde fde : applicacionesList) {
                    Optional<Gbs> bolsaOptio = gbsRepository.findByENTAndEJEAndCGECODAndGBSORGAndGBSFUNAndGBSECO(payload.ENT(), payload.EJE(), payload.CGECOD(), fde.getFDEORG(), fde.getFDEFUN(), fde.getFDEECO());

                    if (bolsaOptio.isPresent()) {
                        Gbs bolsa = bolsaOptio.get();

                        fdeimp = fde.getFDEIMP() != null ? fde.getFDEIMP() : 0.0;
                        fdedif = fde.getFDEDIF() != null ? fde.getFDEDIF() : 0.0;

                        newGbsius = bolsa.getGBSIUS() + fdeimp + fdedif;
                        newGbsiut = bolsa.getGBSIUT() + fdeimp + fdedif;

                        bolsa.setGBSIUS(newGbsius);
                        bolsa.setGBSIUT(newGbsiut);
                        gbsRepository.save(bolsa);
                    } else {
                        errores.add(fde.getFDEORG() + "/" + fde.getFDEFUN() + "/" + fde.getFDEECO());
                    }
                }

                if (!errores.isEmpty()) {
                    return ResponseEntity.badRequest().body("No existe bolsa para: " + String.join(", ", errores));
                }
            }

            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }
}