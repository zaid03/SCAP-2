package com.example.backend.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.COGAIPOnlyDto;
import com.example.backend.dto.CogCgeProjection;
import com.example.backend.dto.CogSaveDto;
import com.example.backend.sqlserver2.model.Cog;
import com.example.backend.sqlserver2.model.CogId;
import com.example.backend.sqlserver2.repository.CogRepository;

@RestController
@RequestMapping("/api/cog")
public class CogController {
    @Autowired
    private CogRepository cogRepository;

    //selecting centro gestores for contrato
    @GetMapping("/fetch-centros/{ent}/{eje}/{concod}")
    public ResponseEntity<?> fetchCentroGestores(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable Integer concod
    ) {
        try {
            List<CogCgeProjection> centros = cogRepository.findAllByENTAndEJEAndCONCOD(ent, eje, concod);
            if (centros.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            return ResponseEntity.ok(centros);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //deleting a centro gestor from contrato
    @DeleteMapping("/delete-centro/{ent}/{eje}/{concod}/{cgecod}")
    public ResponseEntity<?> deleteCentroGestore(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable Integer concod,
        @PathVariable String cgecod
    ) {
        try {
            Optional<COGAIPOnlyDto> centro =  cogRepository.findByENTAndEJEAndCONCODAndCGECOD(ent, eje, concod, cgecod);

            if (centro.isPresent()) {
                Double cogaip = centro.get().getCOGAIP();
                if (cogaip != null && cogaip > 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se puede quitar un centro gestor donde ya hay pedidos");
                } else if (cogaip != null && cogaip == 0) {
                    CogId id = new CogId(ent, eje, concod, cgecod);
                    cogRepository.deleteById(id);

                    return ResponseEntity.noContent().build();
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //saving centro gestores to a contrato
    @PostMapping("/save-centroGestores")
    public ResponseEntity<?> saveCentros(
        @RequestBody List<CogSaveDto> items
    ) {
        try {
            List<Cog> toSave = new ArrayList<>();
            for (CogSaveDto dto: items) {
                boolean exists = cogRepository.existsByENTAndEJEAndCONCODAndCGECOD(dto.ent, dto.eje, dto.concod, dto.cgecod);
                if (!exists) {
                    Cog c = new Cog();
                    c.setENT(dto.ent);
                    c.setEJE(dto.eje);
                    c.setCONCOD(dto.concod);
                    c.setCGECOD(dto.cgecod);
                    c.setCOGIMP(dto.cogimp);
                    c.setCOGAIP(dto.cogaip);
                    toSave.add(c);
                }
            }
            cogRepository.saveAll(toSave);
            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //adding D to a contrato's centro gestor
    public record addD(Double COGIMP, String COGOPD) {}

    @PatchMapping("/update-centro-D/{ent}/{eje}/{concod}/{cgecod}")
    public ResponseEntity<?> addDCentro(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable Integer concod,
        @PathVariable String cgecod,
        @RequestBody addD payload
    ) {
        try {
            if (payload == null || payload.COGIMP() == null || payload.COGOPD() == null) {
                return ResponseEntity.badRequest().body("Faltan datos obligatorios.");
            }

            CogId id = new CogId(ent, eje, concod, cgecod);
            Optional<Cog> centro = cogRepository.findById(id);
            if (centro.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            Cog updateCentro = centro.get();
            updateCentro.setCOGIMP(payload.COGIMP());
            updateCentro.setCOGOPD(payload.COGOPD());
            cogRepository.save(updateCentro);

            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }
}
