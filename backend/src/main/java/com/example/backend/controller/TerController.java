package com.example.backend.controller;

import com.example.backend.sqlserver2.model.Ter;
import com.example.backend.sqlserver2.model.TerId;
import com.example.backend.dto.TerDto;
import com.example.backend.service.TerSearchOptions;
import com.example.backend.sqlserver2.repository.TerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@RestController
@RequestMapping("/api/ter")
public class TerController {
    @Autowired
    private TerRepository terRepository;

    // Get all Ter records for a specific ENT
    @GetMapping("/by-ent/{ent}")
    public ResponseEntity<?> getByEnt(
        @PathVariable int ent
    ) {
        try {
            List<Ter> proveedorees = terRepository.findByENT(ent);
            if(proveedorees.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }
            return ResponseEntity.ok(proveedorees);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //for the list filtered by TERCOD and option bloqueado
    @GetMapping("/by-tercod-bloqueado/{ent}/tercod/{tercod}")
    public ResponseEntity<?> getByENTAndTERCODAndTERBLO(
        @PathVariable int ent, 
        @PathVariable Integer tercod
    ) {
        try {
            List<Ter> proveedorees = terRepository.findByENTAndTERCODAndTERBLO(ent, tercod, 1);
            if(proveedorees.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }
            return ResponseEntity.ok(proveedorees);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //for the list filtered by TERCOD and option no bloqueado
    @GetMapping("/by-tercod-no-bloqueado/{ent}/tercod/{tercod}")
    public ResponseEntity<?> getByENTAndTERCODAndTERBLONot(
        @PathVariable int ent,
        @PathVariable Integer tercod
    ) {
        try {
            List<Ter> proveedorees = terRepository.findByENTAndTERCODAndTERBLO(ent, tercod, 0);
            if(proveedorees.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }
            return ResponseEntity.ok(proveedorees);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //for the list filtered by TERNIF and option bloqueado
    @GetMapping("/by-ternif-bloquado/{ent}/ternif/{ternif}")
    public ResponseEntity<?> getByENTAndTERNIFAndTERBLO(
        @PathVariable int ent, 
        @PathVariable String ternif
    ) {
        try {
            List<Ter> proveedorees = terRepository.findByENTAndTERNIFContainingAndTERBLO(ent, ternif, 1);
            if(proveedorees.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }
            return ResponseEntity.ok(proveedorees);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //for the list filtered by TERNIF and option no bloqueado 
    @GetMapping("/by-ternif-no-bloqueado/{ent}/ternif/{ternif}")
    public ResponseEntity<?> getByENTAndTERNIFAndTERBLONot(
        @PathVariable int ent, 
        @PathVariable String ternif
    ) {
        try {
            List<Ter> proveedorees = terRepository.findByENTAndTERNIFContainingAndTERBLO(ent, ternif, 0);
            if(proveedorees.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }
            return ResponseEntity.ok(proveedorees);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //for the list filtered by TerNIF and TERNOM and TERALI bloqueado
    @GetMapping("/by-ternif-nom-ali-bloquado/{ent}/search")
    public ResponseEntity<?> search(
            @PathVariable int ent,
            @RequestParam String term
    ) {
        try {
            Specification<Ter> spec = TerSearchOptions.searchFiltered(ent, term);
            List<Ter> results = terRepository.findAll(spec);
            if (results.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }
            
            return ResponseEntity.ok(results);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //for the list filtered by TERNIF and TERNOM and TERALI no bloqueado
    @GetMapping("/by-nif-nom-ali-no-bloquado/{ent}/search-by-term")
    public ResponseEntity<?> searchByTerm(
        @PathVariable int ent,
        @RequestParam String term
    ) {
        try {
            Specification<Ter> spec = TerSearchOptions.searchByTerm(ent, term);
            List<Ter> results = terRepository.findAll(spec);
            
            if (results.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }
            
            return ResponseEntity.ok(results);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //for the list filtered by TERNOM and TERALI bloqueado
    @GetMapping("/by-nom-ali-bloquado/{ent}/searchByNomOrAli")
    public ResponseEntity<?> searchByNomOrAli(
        @PathVariable int ent,
        @RequestParam String term
    ) {
        try {
            Specification<Ter> spec = TerSearchOptions.searchByNomOrAli(ent, term);
            List<Ter> results = terRepository.findAll(spec);
            
            if (results.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }
            return ResponseEntity.ok(results);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //for the list filtered by TERNOM and TERALI no bloqueado
    @GetMapping("/by-nom-ali-no-bloquado/{ent}/findMatchingNomOrAli")
    public ResponseEntity<?> findMatchingNomOrAli(
        @PathVariable int ent,
        @RequestParam String term
    ) {
        try {
            Specification<Ter> spec = TerSearchOptions.findMatchingNomOrAli(ent, term);
            List<Ter> results = terRepository.findAll(spec);
            
            if (results.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }   
            return ResponseEntity.ok(results);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //for the option todos
    // filtering by tercod
    @GetMapping("/by-ent/{ent}/tercod/{tercod}")
    public ResponseEntity<?> getByENTAndTERCOD(
        @PathVariable int ent, 
        @PathVariable Integer tercod
    ) {
        try {
            List<Ter> proveedorees = terRepository.findAllByENTAndTERCOD(ent, tercod);
            if(proveedorees.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }
            return ResponseEntity.ok(proveedorees);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }
    
    //filtering by ternif
    @GetMapping("/by-ent/{ent}/ternif/{ternif}")
    public ResponseEntity<?> getByENTAndTERNIF(
        @PathVariable int ent, 
        @PathVariable String ternif
    ) {
        try {
            List<Ter> proveedorees = terRepository.findByENTAndTERNIFContaining(ent, ternif);
            if(proveedorees.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }
            return ResponseEntity.ok(proveedorees);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    // filtering by ternif and ternom and terali
    @GetMapping("/by-ent/{ent}/search-todos")
    public ResponseEntity<?> searchTodos(
        @PathVariable int ent,
        @RequestParam String term
    ) {
        try {
            Specification<Ter> spec = TerSearchOptions.searchTodos(ent, term);
            List<Ter> results = terRepository.findAll(spec);
            
            if (results.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }
            return ResponseEntity.ok(results);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //to filter with bloqueado option only
    @GetMapping("/filter/{ent}")
    public ResponseEntity<?> filterBloqueado(
        @PathVariable Integer ent
    ) {
        try {
            List<Ter> proveedores = terRepository.findByENTAndTERBLO(ent, 1);

            if (proveedores.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }
            return ResponseEntity.ok(proveedores);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al filter de proveedores: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //to filter with no bloqueado option only
    @GetMapping("/filter-no/{ent}")
    public ResponseEntity<?> filterNoBloqueado(
        @PathVariable Integer ent
    ) {
        try {
            List<Ter> proveedores = terRepository.findByENTAndTERBLO(ent, 0);

            if (proveedores.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }
            return ResponseEntity.ok(proveedores);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al filter de proveedores: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //for modifying a Ter record
    public record updateProveedor(String TERWEB, String TEROBS, Integer TERBLO, Integer TERACU) {};
    @PutMapping("/updateFields/{ent}/{tercod}")
    public ResponseEntity<?> updateTerFields(
        @PathVariable Integer ent,
        @PathVariable Integer tercod, 
        @RequestBody updateProveedor payload
    ) {
        try {
            TerId id = new TerId(ent, tercod);
            Optional<Ter> proveedor = terRepository.findById(id);
            if (proveedor.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");  
            }

            Ter updateProveedor = proveedor.get();
            updateProveedor.setTERWEB(payload.TERWEB());
            updateProveedor.setTEROBS(payload.TEROBS());
            updateProveedor.setTERBLO(payload.TERBLO());
            updateProveedor.setTERACU(payload.TERACU());

            terRepository.save(updateProveedor);
            return ResponseEntity.noContent().build();

        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //for selected proveedores to be added from sicalwin
    @PostMapping(value = "/save-proveedores/{ent}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> createMultipleForEnt(@PathVariable int ent, @RequestBody(required = false) List<TerDto> dtos) {
        try {
            if (dtos == null || dtos.isEmpty()) {
                return ResponseEntity.badRequest().body("Faltan datos obligatorios");
            }
            for (TerDto dto : dtos) {
                if (dto.getTERNOM() == null || dto.getTERNOM().trim().isEmpty()
                        || dto.getTERNIF() == null || dto.getTERNIF().trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("Datos incompletos");
                }
            }

            Integer next = terRepository.findNextTercodForEnt(ent);
            if (next == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            List<Ter> saved = new ArrayList<>();
            for (TerDto dto : dtos) {
                Ter t = new Ter();
                t.setENT(ent);
                t.setTERCOD(next++);
                t.setTERNOM(dto.getTERNOM());
                t.setTERALI(dto.getTERALI());
                t.setTERNIF(dto.getTERNIF());
                t.setTERDOM(dto.getTERDOM());
                t.setTERCPO(dto.getTERCPO());
                t.setTERTEL(dto.getTERTEL());
                t.setTERFAX(dto.getTERFAX());
                t.setTERWEB(dto.getTERWEB());
                t.setTERCOE(dto.getTERCOE());
                t.setTEROBS(dto.getTEROBS());
                t.setTERPOB(dto.getTERPOB());
                saved.add(terRepository.save(t));
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Server error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
}