package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.*;

import com.example.backend.sqlserver2.model.Cfg;
import com.example.backend.sqlserver2.repository.CfgRepository;

import java.util.List;

@RestController
@RequestMapping("/api/cfg")
public class CfgController {
    
    @Autowired
    private CfgRepository cfgRepository;

    //method to ejercicio in Cfg table by entidad and CFGEST
    @GetMapping("/by-ent/{ent}")
    public ResponseEntity<?> getEJE(@PathVariable int ent) {
        try {
            List<Cfg> results = cfgRepository.findEjeByENTAndCFGEST(ent, 0);
            
            if (results.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }
            
            return ResponseEntity.ok(results);
            
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ex.getMostSpecificCause().getMessage());
        }
    }

    //to fetch all ejes
    @GetMapping("/fetch-Eje/{ENT}")
    public ResponseEntity<?> fetchAllEjercicios(
        @PathVariable Integer ENT
    ) {
        try {
            List<Cfg> Eje = cfgRepository.findByENT(ENT);
            if(Eje.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            return ResponseEntity.ok(Eje);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //to search in eje
    @GetMapping("/search-Eje/{ENT}/{EJE}")
    public ResponseEntity<?> searchEjercicios(
        @PathVariable Integer ENT,
        @PathVariable String EJE
    ) {
        try {
            List<Cfg> Eje = cfgRepository.findByENTAndEJE(ENT, EJE);
            if(Eje.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            return ResponseEntity.ok(Eje);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }
}
