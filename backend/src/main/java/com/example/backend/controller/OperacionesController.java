package com.example.backend.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.Operaciones;
import com.example.backend.service.OperacionesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/sical")
@CrossOrigin(origins = "http://localhost:4200")
public class OperacionesController {

    private static final Logger logger = LoggerFactory.getLogger(OperacionesController.class);

    private final OperacionesService operacionesService;

    public OperacionesController(OperacionesService operacionesService) {
        this.operacionesService = operacionesService;
    }

    @GetMapping("/operaciones")
    public ResponseEntity<?> getOperaciones(
        @RequestParam(name = "numeroOperDesde", required = false) String numeroOperDesde,
        @RequestParam(name = "numeroOperHasta", required = false) String numeroOperHasta,
        @RequestParam(name = "codigoOperacion", required = false) String codigoOperacion,
        @RequestParam(name = "clorg", required = false) String organica,
        @RequestParam(name = "clfun", required = false) String funcional,
        @RequestParam(name = "cleco", required = false) String economica,
        @RequestParam(name = "expediente", required = false) String expediente,
        @RequestParam(name = "grupoApunte", required = false) String grupoApunte,
        @RequestParam(name = "oficina", required = false) String oficina) {
        try {
            List<Operaciones> operaciones = operacionesService.getOperaciones(
                    numeroOperDesde,
                    numeroOperHasta,
                    codigoOperacion,
                    organica,
                    funcional,
                    economica,
                    expediente,
                    grupoApunte,
                    oficina);
            return ResponseEntity.ok(operaciones);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Error in getOperaciones", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", ex.getMessage()));
        }
    }
}