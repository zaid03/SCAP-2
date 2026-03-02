package com.example.backend.controller;

import com.example.backend.sqlserver2.model.Apr;
import com.example.backend.sqlserver2.model.AprId;
import com.example.backend.sqlserver2.repository.AprRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
@RestController
@RequestMapping("/api/more")
public class AprController {
    @Autowired
    private AprRepository aprRepository;

    // fetching articulos fr proveedor
    @GetMapping("/by-apr/{ent}/{tercod}")
    public ResponseEntity<?> getApr(
        @PathVariable Integer ent, 
        @PathVariable Integer tercod
    ) {
        try {
            List<Apr> articulos = aprRepository.findByENTAndTERCOD(ent, tercod);
            if (articulos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }
            return ResponseEntity.ok(articulos);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    // Modifying an articulo
    public record articulo(String aprref, Double aprpre, Double apruem, String aprobs, Integer apracu) {}
    @PatchMapping("/update-apr/{ent}/{tercod}/{afacod}/{asucod}/{artcod}")
    public ResponseEntity<?> updateArticulo(
        @PathVariable Integer ent,
        @PathVariable Integer tercod,
        @PathVariable String afacod,
        @PathVariable String asucod,
        @PathVariable String artcod,
        @RequestBody articulo payload
    ) {
        try {

            AprId id = new AprId(ent, tercod, afacod, asucod, artcod);
            Optional<Apr> articulo = aprRepository.findById(id);
            if(articulo.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }
            
            Apr articulosUpdate = articulo.get();
            articulosUpdate.setAPRREF(payload.aprref());
            articulosUpdate.setAPRPRE(payload.aprpre());
            articulosUpdate.setAPRUEM(payload.apruem());
            articulosUpdate.setAPROBS(payload.aprobs());
            articulosUpdate.setAPRACU(payload.apracu());

            aprRepository.save(articulosUpdate);
            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error:" + ex.getMostSpecificCause().getMessage());
        }
    }
    
    // Deleting data
    @DeleteMapping("/delete-apr")
    public ResponseEntity<String> deleteApr(
        @RequestParam Integer ent,
        @RequestParam Integer tercod,
        @RequestParam String afacod,
        @RequestParam String asucod,
        @RequestParam String artcod
    ) {
        try {
            AprId id = new AprId(ent, tercod, afacod, asucod, artcod);
            if (!aprRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }
            aprRepository.deleteById(id);
            return ResponseEntity.ok("articulo eliminado exitosamente");
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //adding data
    @PostMapping("/add-apr")
    public ResponseEntity<?> addApr(
        @RequestBody Apr apr
    ) {
        try {
            aprRepository.save(apr);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(apr.getAPRREF() + " added successfully");
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error:" + ex.getMostSpecificCause().getMessage());
        }
    }
}
