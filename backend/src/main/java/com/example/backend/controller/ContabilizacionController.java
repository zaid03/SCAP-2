package com.example.backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.ContabilizacionRequestDto;
import com.example.backend.dto.ContabilizacionResponseDto;
import com.example.backend.service.ContabilizacionService;
import com.example.backend.sqlserver2.model.Fac;
import com.example.backend.sqlserver2.model.FacId;
import com.example.backend.sqlserver2.model.Fde;
import com.example.backend.sqlserver2.model.Fdt;
import com.example.backend.sqlserver2.model.Ter;
import com.example.backend.sqlserver2.repository.FacRepository;
import com.example.backend.sqlserver2.repository.FdeRepository;
import com.example.backend.sqlserver2.repository.FdtRepository;
import com.example.backend.sqlserver2.repository.TerRepository;

@RestController
@RequestMapping("/api/contabilizacion")
public class ContabilizacionController {

    @Autowired
    private ContabilizacionService contabilizacionService;

    @Autowired
    private FacRepository facRepository;

    @Autowired
    private FdeRepository fdeRepository;

    @Autowired
    private FdtRepository fdtRepository;

    @Autowired
    private TerRepository terRepository;

    private record ContabilizacionData(String terAyt, List<Fde> fdeList, List<Fdt> fdtList) {}
    @PostMapping("/generar")
    public ResponseEntity<?> generarOperacion(@RequestBody ContabilizacionRequestDto request) {
        try {
            ResponseEntity<?> validation = validateRequest(request);
            if (validation != null) return validation;

            Integer entInt = Integer.parseInt(request.getEnt());
            FacId facId = new FacId(entInt, request.getEje(), request.getFacnum());
            Optional<Fac> facOpt = facRepository.findById(facId);
            
            if (facOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Factura no encontrada");
            }
            
            Fac fac = facOpt.get();
            ContabilizacionData data = retrieveContabilizacionData(request, fac, entInt);
            
            String smlInput = contabilizacionService.buildSmlInput(request, fac, data.fdeList(), data.fdtList(), data.terAyt());
            String soapResponse = contabilizacionService.sendSmlRequest(smlInput, request.getWebserviceUrl());
            ContabilizacionResponseDto response = contabilizacionService.parseResponse(soapResponse);
            
            return handleContabilizacionResponse(response, fac, request);
            
        } catch (Exception ex) {
            ContabilizacionResponseDto error = new ContabilizacionResponseDto();
            error.setExito(false);
            error.setMensaje("Error: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private ResponseEntity<?> validateRequest(ContabilizacionRequestDto request) {
        if (request.getEnt() == null || request.getEje() == null || request.getFacnum() == null) {
            return ResponseEntity.badRequest().body("Faltan datos obligatorios: ent, eje, facnum");
        }
        return null;
    }

    private ContabilizacionData retrieveContabilizacionData(ContabilizacionRequestDto request, Fac fac, Integer entInt) {
        String terAyt = null;
        if (fac.getTERCOD() != null) {
            Optional<Ter> terOpt = terRepository.findByENTAndTERCOD(entInt, fac.getTERCOD());
            if (terOpt.isPresent() && terOpt.get().getTERAYT() != null) {
                terAyt = String.valueOf(terOpt.get().getTERAYT());
            }
        }
        
        List<Fde> fdeList = fdeRepository.findByENTAndEJEAndFACNUM(entInt, request.getEje(), request.getFacnum());
        List<Fdt> fdtList = fdtRepository.findByENTAndEJEAndFACNUM(entInt, request.getEje(), request.getFacnum());
        
        return new ContabilizacionData(terAyt, fdeList, fdtList);
    }

    private LocalDateTime parseFechaContable(String fechaContable) {
        if (fechaContable == null) return null;
        String fc = fechaContable.replace("-", "");
        if (fc.length() == 8) {
            int year = Integer.parseInt(fc.substring(0, 4));
            int month = Integer.parseInt(fc.substring(4, 6));
            int day = Integer.parseInt(fc.substring(6, 8));
            return LocalDateTime.of(year, month, day, 0, 0);
        }
        return null;
    }

    private ResponseEntity<?> handleContabilizacionResponse(ContabilizacionResponseDto response, Fac fac, ContabilizacionRequestDto request) {
        if (response.isExito()) {
            if (response.getOpesical() != null) {
                fac.setFACADO(response.getOpesical());
            }
            LocalDateTime facFco = parseFechaContable(request.getFechaContable());
            if (facFco != null) {
                fac.setFACFCO(facFco);
            }
            facRepository.save(fac);
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}