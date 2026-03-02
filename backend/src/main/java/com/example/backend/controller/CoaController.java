package com.example.backend.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.backend.dto.CoaArtProjection;
import com.example.backend.dto.CoaSaveDto;
import com.example.backend.sqlserver2.repository.CoaRepository;
import com.example.backend.sqlserver2.model.Coa;
import com.example.backend.sqlserver2.model.CoaId;

@RestController
@RequestMapping("/api/coa")
public class CoaController {
    @Autowired
    private CoaRepository coaRepository;

    //selecting articulos for a contrato
    @GetMapping("/fetch-articulos/{ent}/{eje}/{concod}")
    public ResponseEntity<?> fetchArticulos(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable Integer concod
    ) {
        try {
            List<CoaArtProjection> articulos = coaRepository.findAllByENTAndEJEAndConnCONCOD(ent, eje, concod);
            if (articulos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            return ResponseEntity.ok(articulos);

        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //update articulo
    public record artUpdate(Double COAPRE) {};

    @PatchMapping("/update-articulo/{ent}/{eje}/{concod}/{afacod}/{asucod}/{artcod}")
    public ResponseEntity<?> updateArticulo(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable Integer concod,
        @PathVariable String afacod,
        @PathVariable String asucod,
        @PathVariable String artcod,
        @RequestBody artUpdate payload
    ) {
        try {
            if(payload == null || payload.COAPRE() == null) {
                return ResponseEntity.badRequest().body("Faltan datos obligatorios.");
            }

            CoaId id = new CoaId(ent, eje, concod, afacod, asucod, artcod);
            Optional<Coa> articulo = coaRepository.findById(id);
            if (articulo.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            Coa coaUpdate = articulo.get();
            coaUpdate.setCOAPRE(payload.COAPRE());
            coaRepository.save(coaUpdate);

            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //delete an article from a contrato
    @DeleteMapping("/delete-articulo/{ent}/{eje}/{concod}/{afacod}/{asucod}/{artcod}")
    public ResponseEntity<?> deleteArticulo(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable Integer concod,
        @PathVariable String afacod,
        @PathVariable String asucod,
        @PathVariable String artcod
    ) {
        try {
            CoaId id = new CoaId(ent, eje, concod, afacod, asucod, artcod);
            if (!coaRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            coaRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //saving articles for a contrato
    @PostMapping("/save-articulos")
    public ResponseEntity<?> saveArticulos(
        @RequestBody List<CoaSaveDto> items
    ) {
        try {
            List<Coa> toSave = new ArrayList<>();
            for (CoaSaveDto dto : items) {
                boolean exists = coaRepository.existsByENTAndCONCODAndAFACODAndASUCODAndARTCOD(dto.ent, dto.concod, dto.afacod, dto.asucod, dto.artcod);
                if (!exists) {
                    Coa c = new Coa();
                    c.setENT(dto.ent);
                    c.setEJE(dto.eje);
                    c.setCONCOD(dto.concod);
                    c.setAFACOD(dto.afacod);
                    c.setASUCOD(dto.asucod);
                    c.setARTCOD(dto.artcod);
                    c.setCOAPRE(dto.COAPRE);
                    c.setCOAPR2(dto.COAPR2);
                    c.setCOAPR3(dto.COAPR3);
                    c.setCOAPR4(dto.COAPR4);
                    c.setCOAPR5(dto.COAPR5);
                    toSave.add(c);
                }
            }
            coaRepository.saveAll(toSave);
            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }
}