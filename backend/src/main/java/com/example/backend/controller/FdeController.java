package com.example.backend.controller;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.backend.sqlserver2.repository.FdeRepository;
import com.example.backend.dto.ProjectionContabilizar;
import com.example.backend.service.ContabilizarSearch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fde")
public class FdeController {
    @Autowired
    private FdeRepository fdeRepository;
    @Autowired
    private ContabilizarSearch contabilizarSearch;

    private static final String SIN_RESULTADO = "Sin resultado";
    private static final String ERROR = "Error :";

    //selecting all facturas in consulta del pendiente de contabilizado
    @GetMapping("/fetch-pendiente-del-contabilizar/{ent}/{eje}")
    public ResponseEntity<?> fetchContabilizado (
        @PathVariable Integer ent,
        @PathVariable String eje
    ) {
        try {
            List<ProjectionContabilizar> facturas = fdeRepository.findPendienteContabilizar(ent, eje);

            if (facturas.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(SIN_RESULTADO);
            }

            return ResponseEntity.ok(facturas);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERROR + ex.getMostSpecificCause().getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERROR + ex.getMessage());
        }
    }

    //search in consulta del pendiente de contabilizado
    @GetMapping("/search-pendiente-contabilizar")
    public ResponseEntity<?> searchContabilizado (
        @RequestParam Integer ent,
        @RequestParam String eje,
        @RequestParam(required = false) String proveedor,
        @RequestParam(required = false) String centroGestor,
        @RequestParam(required = false) String economica,
        @RequestParam(required = false) Integer ano
    ) {
        try {
            List<ProjectionContabilizar> facturas = contabilizarSearch.searchContabilizado(ent, eje, proveedor, centroGestor, economica, ano);

            if (facturas.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(SIN_RESULTADO);
            }

            return ResponseEntity.ok(facturas);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERROR + ex.getMostSpecificCause().getMessage());
        }
    }
}