package com.example.backend.controller;

import com.example.backend.sqlserver2.model.Cco;
import com.example.backend.sqlserver2.model.CcoId;
import com.example.backend.sqlserver2.repository.CcoRepository;
import com.example.backend.sqlserver2.repository.DepRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cco")
public class CcoController {
    @Autowired
    private CcoRepository ccoRepository;
    @Autowired DepRepository depRepository;

    //selecting all centros de coste
    @GetMapping("/fetch-all/{ENT}/{EJE}")
    public ResponseEntity<?> fetchCosteAll(
        @PathVariable Integer ENT,
        @PathVariable String EJE
    ) {
        try {
            List<?> centros =  ccoRepository.findByENTAndEJE(ENT, EJE);
            if (centros.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Sin resultado");
            }
            return ResponseEntity.ok(centros);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //search by ccocod
    @GetMapping("/filter-by/{ENT}/{EJE}/{CCOCOD}")
    public ResponseEntity<?> searchCoste(
        @PathVariable Integer ENT,
        @PathVariable String EJE,
        @PathVariable String CCOCOD
    ) {
        try {
            List<?> centros =  ccoRepository.findByENTAndEJEAndCCOCOD(ENT, EJE, CCOCOD);
            if (centros.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Sin resultado");
            }
            return ResponseEntity.ok(centros);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //search by ccodes
    @GetMapping("/filter-by-des/{ENT}/{EJE}/{CCODES}")
    public ResponseEntity<?> searchCosteLike(
        @PathVariable Integer ENT,
        @PathVariable String EJE,
        @PathVariable String CCODES
    ) {
        try {
            List<?> centros =  ccoRepository.findByENTAndEJEAndCCODESContaining(ENT, EJE, CCODES);
            if (centros.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Sin resultado");
            }
            return ResponseEntity.ok(centros);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //to add a centro de coste
    public record newCentro(Integer ENT, String EJE, String CCOCOD, String CCODES) {}
    @PostMapping("/Insert-centro")
    public ResponseEntity<?> insertCentro(
        @RequestBody newCentro payload
    ) {
        try {
            if (payload == null || payload.ENT() == null || payload.EJE() == null || payload.CCOCOD() == null || payload.CCODES() == null) {
                return ResponseEntity.badRequest().body("Faltan datos obligatorios.");
            }

            if(!ccoRepository.findByENTAndEJEAndCCOCOD(payload.ENT(), payload.EJE(), payload.CCOCOD()).isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Sin resultado");
            }

            Cco nueva = new Cco();
            nueva.setENT(payload.ENT());
            nueva.setEJE(payload.EJE());
            nueva.setCCOCOD(payload.CCOCOD());
            nueva.setCCODES(payload.CCODES());

            ccoRepository.save(nueva);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //update a centro coste
    public record updateCentro(String CCODES) {}
    @PatchMapping("/update-centro/{ENT}/{EJE}/{CCOCOD}")
    public ResponseEntity<?> UpdateCentro(
        @PathVariable Integer ENT,
        @PathVariable String EJE,
        @PathVariable String CCOCOD,
        @RequestBody updateCentro payload
    ) {
        try {
            if (payload == null || payload.CCODES() == null) {
                return ResponseEntity.badRequest().body("Faltan datos obligatorios.");
            }

            CcoId id = new CcoId(ENT, EJE, CCOCOD);
            Optional<Cco> coste = ccoRepository.findById(id);
            if (coste.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }

            Cco costeUpdate = coste.get();
            costeUpdate.setCCODES(payload.CCODES());

            ccoRepository.save(costeUpdate);
            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //delete a centro de coste
    @DeleteMapping("/delete-coste/{ENT}/{EJE}/{CCOCOD}")
    public ResponseEntity<?> deleteCoste(
        @PathVariable Integer ENT,
        @PathVariable String EJE,
        @PathVariable String CCOCOD
    ) {
        try {
            CcoId id = new CcoId(ENT, EJE, CCOCOD);
            if(!ccoRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Sin resultado");
            }

            long costes = depRepository.countByENTAndEJEAndCCOCOD(ENT, EJE, CCOCOD);
            if (costes > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede borrar un centro de coste con centros gestores asociados");
            }

            ccoRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }
}
