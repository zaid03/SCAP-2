package com.example.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.sqlserver2.model.Ter;
import com.example.backend.sqlserver2.repository.TerRepository;
import com.example.backend.service.ProveedoresSearch;

@RestController
@RequestMapping("/api/ter")
public class TerController {
    @Autowired
    private TerRepository terRepository;
    @Autowired
    private ProveedoresSearch proveedoresSearch;

    private static final String SIN_RESULTADO = "Sin resultado";
    private static final String ERROR = "Error :";

    // Get all Ter records for a specific ENT
    @GetMapping("/by-ent/{ent}")
    public ResponseEntity<?> getByEnt(
        @PathVariable int ent
    ) {
        try {
            List<Ter> proveedorees = terRepository.findByENT(ent);
            if(proveedorees.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(SIN_RESULTADO);
            }
            return ResponseEntity.ok(proveedorees);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ERROR + ex.getMostSpecificCause().getMessage());
        }
    }

    //searching in proveedores
    @GetMapping("/search-proveedores")
    public ResponseEntity<?> searchProveedores (
        @RequestParam Integer ent,
        @RequestParam String searchMode,
        @RequestParam String term
    ) {
        try {
            List<Ter> proveedores = proveedoresSearch.searchProveedoers(ent, searchMode, term);
            if (proveedores.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(SIN_RESULTADO);
            }

            return ResponseEntity.ok(proveedores);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERROR + ex.getMessage());
        }
    }
}