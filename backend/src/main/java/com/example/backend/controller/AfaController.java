package com.example.backend.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.sqlserver2.model.Afa;
import com.example.backend.sqlserver2.model.AfaId;
import com.example.backend.sqlserver2.repository.AfaRepository;

@RestController
@RequestMapping("/api/afa")
public class AfaController {
    @Autowired
    private AfaRepository afaRepository;

    private static final String SIN_RESULTADO = "Sin resultado";
    private static final String ERROR = "Error :";

    //method to fetch all familias for consulta analitica de familias
    @GetMapping("/fetch-familia-analitica/{ent}")
    public ResponseEntity<?> FetchFamiliaAnalitic (
        @PathVariable Integer ent
    ) {
        try {
            List<Afa> familias = afaRepository.findByENT(ent);

            if (familias.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(SIN_RESULTADO);
            }

            return ResponseEntity.ok(familias);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ERROR + ex.getMostSpecificCause().getMessage());
        }
    }

    //selecting familias for main familia y subfamilia grid
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

    //for the search
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
