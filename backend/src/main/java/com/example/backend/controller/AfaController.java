package com.example.backend.controller;

import com.example.backend.sqlserver2.model.Afa;
import com.example.backend.sqlserver2.model.AfaId;
import com.example.backend.sqlserver2.repository.AfaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataAccessException;


import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/afa")
public class AfaController {

    @Autowired
    private AfaRepository afaRepository;

    @GetMapping("/by-ent/{ent}/{afacod}")
    public ResponseEntity<?> getByEntAndAfacod(
        @PathVariable int ent, 
        @PathVariable String afacod
    ) {
        try{
            List<Afa> familias = afaRepository.findByENTAndAFACOD(ent, afacod);
            if(familias.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }
            return ResponseEntity.ok(familias);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error : " + ex.getMostSpecificCause().getMessage());
        }
    }

    @GetMapping("/by-ent-like/{ent}/{afades}")
    public ResponseEntity<?> getByEntAndAfadesLike(
        @PathVariable int ent, 
        @PathVariable String afades
    ) {
        try {
            List<Afa> familias = afaRepository.findByENTAndAFADESContaining(ent, afades);
            if(familias.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }

            return ResponseEntity.ok(familias);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error : " + ex.getMostSpecificCause().getMessage());
        }
    }

    //find familias by ent
    @GetMapping("/by-ent/{ent}")
    public ResponseEntity<?> getAfaByEnt(
        @PathVariable int ent
    ) {
        try {
            List<Afa> familias = afaRepository.findByENT(ent);
            if(familias.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }

            return ResponseEntity.ok(familias);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error : " + ex.getMostSpecificCause().getMessage());
        }
    }

    //update description of familias
    public record updateFamilia(String AFADES) {}
    @PatchMapping("/update-familia/{ent}/{afacod}")
    public ResponseEntity<?> updateFamilia(
        @PathVariable Integer ent,
        @PathVariable String afacod,
        @RequestBody updateFamilia payload
    ) {
        try {
            if(payload == null || payload.AFADES() == null) {
                return ResponseEntity.badRequest().body("Faltan datos obligatorios.");
            }

            AfaId id = new AfaId(ent, afacod);
            Optional<Afa> familia = afaRepository.findById(id);
            if(familia.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }

            Afa familiaUpdate = familia.get();
            familiaUpdate.setAFADES(payload.AFADES());

            afaRepository.save(familiaUpdate);
            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Update failed: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //familia add
    public record newFamilia(Integer ent, String afacod, String afades) {}
    @PostMapping("/Insert-familia")
    public ResponseEntity<?> insertFamilia(
        @RequestBody newFamilia payload
    )
    {
        if (payload == null || payload.ent() == null || payload.afacod() == null || payload.afades() == null) {
            return ResponseEntity.badRequest().body("Faltan datos obligatorios.");
        }

        if (!afaRepository.findByENTAndAFACOD(payload.ent(), payload.afacod()).isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Sin resultado");
        }

        Afa nueva = new Afa();
        nueva.setENT(payload.ent());
        nueva.setAFACOD(payload.afacod());
        nueva.setAFADES(payload.afades());

        afaRepository.save(nueva);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
