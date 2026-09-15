package com.example.backend.controller;

import java.util.List;

import com.example.backend.dto.SaldoContrato;
import com.example.backend.sqlserver2.repository.CogRepository;
import com.example.backend.service.SaldoContratoSearch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/cog")
public class CogController {
    @Autowired
    private CogRepository cogRepository;
    @Autowired
    private SaldoContratoSearch saldoContratoSearch;

    private static final String SIN_RESULTADO = "Sin resultado";
    private static final String ERROR = "Error :";

    //main fetch for C.saldo de contrato
    @GetMapping("/Saldo-contrato/{ent}/{eje}")
    public ResponseEntity<?> fetchSaldoContrato (
        @PathVariable Integer ent,
        @PathVariable String eje
    ) {
        try {
            List<SaldoContrato> contratos = cogRepository.findByENTAndEJEAndCot_conn_CONTIPAndCot_conn_CONBLONot(ent, eje, 3, 1);
            if (contratos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(SIN_RESULTADO);
            }

            return ResponseEntity.ok(contratos);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERROR + ex.getMostSpecificCause().getMessage());
        }
    }

    //filtering for C.saldo de contrato
    @GetMapping("/search-saldo-contrato/{ent}/{eje}")
    public ResponseEntity<?> searchSaldoContrato (
        @PathVariable Integer ent,
        @PathVariable String eje,
        @RequestParam(required = false) String cge,
        @RequestParam(required = false) String contrato,
        @RequestParam(required = false) String proveedor
    ) {
        try {
            if (cge == null && contrato == null && proveedor == null) {
            return ResponseEntity.badRequest().body("Faltan datos obligatorios");
            }

            List<SaldoContrato> contratos = saldoContratoSearch.searchSaldoContratos(ent, eje, cge, contrato, proveedor);
            if (contratos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(SIN_RESULTADO);
            }

            return ResponseEntity.ok(contratos);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERROR + ex.getMostSpecificCause().getMessage());
        }  
    }
}