package com.example.backend.controller;

import com.example.backend.sqlserver2.model.Cge;
import com.example.backend.sqlserver2.model.CgeId;
import com.example.backend.sqlserver2.model.Gbs;
import com.example.backend.sqlserver2.model.GbsId;
import com.example.backend.sqlserver2.repository.CgeRepository;
import com.example.backend.dto.GbsWithCgeDto;
import com.example.backend.dto.bolsaSaveDto;
import com.example.backend.sqlserver2.repository.GbsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gbs")
public class GbsController {
    @Autowired
    private GbsRepository gbsRepository;
    @Autowired
    private CgeRepository cgeRepository;

    //for the main list of bolsa por cge
    @GetMapping("fetch-all/{ent}/{eje}/{cgecod}")
    public ResponseEntity<?> getBolsas(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable String cgecod
    ) {
        try {
            List<Gbs> gbsList = gbsRepository.findByENTAndEJEAndCGECOD(ent, eje, cgecod);
            
            Optional<Cge> cgeOpt = cgeRepository.findById(new CgeId(ent, eje, cgecod));
            if (cgeOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }
            Cge cge = cgeOpt.get();

            List<GbsWithCgeDto> result = gbsList.stream().map(g -> new GbsWithCgeDto(
                cge.getCGECOD(), cge.getCGEDES(), cge.getCGECIC() != null ? String.valueOf(cge.getCGECIC()) : null,
                g.getGBSREF(), g.getGBSOPE(), g.getGBSORG(), g.getGBSFUN(), g.getGBSECO(),
                g.getGBSFOP(), g.getGBSIMP(), g.getGBSIBG(), g.getGBSIUS(), g.getGBSICO(),
                g.getGBSIUT(), g.getGBSICT(), g.getGBS413()
            )).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //modifying a gbsimp
    public record updateBolsa(Double GBSIMP, Double GBSIUS, Double GBSICO, LocalDateTime GBSFOP) {}
    @PatchMapping("/{ent}/{eje}/{cgecod}/{gbsref}")
    public ResponseEntity<?> updateBolsa(
            @PathVariable Integer ent,
            @PathVariable String eje,
            @PathVariable String cgecod,
            @PathVariable String gbsref,
            @RequestBody updateBolsa payload
    ) {
        try {
            if (payload == null || payload.GBSIMP() == null || payload.GBSIUS() == null || payload.GBSICO() == null || payload.GBSFOP() == null) {
                return ResponseEntity.badRequest().body("Faltan datos obligatorios.");
            }

            GbsId id = new GbsId(ent, eje, cgecod, gbsref);
            Optional<Gbs> bolsa = gbsRepository.findById(id);
            if (bolsa.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }

            Gbs bolsaUpdate = bolsa.get();
            bolsaUpdate.setGBSIMP(payload.GBSIMP());
            bolsaUpdate.setGBSIUS(payload.GBSIUS());
            bolsaUpdate.setGBSICO(payload.GBSICO());
            bolsaUpdate.setGBSFOP(payload.GBSFOP());

            gbsRepository.save(bolsaUpdate);
            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //modifying gbsibg
    public record updateGbsibg(Double GBSIBG) {}
    @PatchMapping("update-gbsibg/{ent}/{eje}/{cgecod}/{gbsref}")
    public ResponseEntity<?> updateGbsibg(
            @PathVariable Integer ent,
            @PathVariable String eje,
            @PathVariable String cgecod,
            @PathVariable String gbsref,
            @RequestBody updateGbsibg payload
    ) {
        try {
            if (payload == null || payload.GBSIBG() == null) {
                return ResponseEntity.badRequest().body("Faltan datos obligatorios.");
            }

            GbsId id = new GbsId(ent, eje, cgecod, gbsref);
            Optional<Gbs> bolsa = gbsRepository.findById(id);
            if (bolsa.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }

            Gbs bolsaUpdate = bolsa.get();
            bolsaUpdate.setGBSIBG(payload.GBSIBG());

            gbsRepository.save(bolsaUpdate);
            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //Traspasar bolsa de gestión
    public record trans(Double GBSIMP, Double GBSIBG, Double GBSIUS, Double GBSICO, LocalDateTime GBSFOP) {}
    @PatchMapping("transpasar-bolsa/{ent}/{eje}/{cgecod}/{gbsref}")
    public ResponseEntity<?> TranspasarBolsa(
            @PathVariable Integer ent,
            @PathVariable String eje,
            @PathVariable String cgecod,
            @PathVariable String gbsref,
            @RequestBody trans payload
    ) {
        try {
            if (payload == null || payload.GBSIMP() == null || payload.GBSIUS() == null || payload.GBSICO() == null || payload.GBSFOP() == null) {
                return ResponseEntity.badRequest().body("Faltan datos obligatorios.");
            }

            GbsId id = new GbsId(ent, eje, cgecod, gbsref);
            Optional<Gbs> bolsa = gbsRepository.findById(id);
            if (bolsa.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado");
            }

            Gbs bolsaUpdate = bolsa.get();
            bolsaUpdate.setGBSIMP(payload.GBSIMP());
            bolsaUpdate.setGBSIBG(payload.GBSIBG());
            bolsaUpdate.setGBSIUS(payload.GBSIUS());
            bolsaUpdate.setGBSICO(payload.GBSICO());
            bolsaUpdate.setGBSFOP(payload.GBSFOP());

            gbsRepository.save(bolsaUpdate);
            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //for main list of bolsa 
    // @GetMapping("fetchAll/{ent}/{eje}")
    // public ResponseEntity<?> getBolsas(
    //     @PathVariable Integer ent,
    //     @PathVariable String eje
    // ) {
    //     try {
    //         List<Gbs> bolsas = gbsRepository.findByENTAndEJE(ent, eje);
    //         if (bolsas == null || bolsas.isEmpty()) {
    //             return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
    //         }
    //         List<String> cgeCodes = bolsas.stream().map(Gbs::getCGECOD).distinct().collect(Collectors.toList());
    //         List<Cge> cges = cgeRepository.findByENTAndEJEAndCGECODIn(ent, eje, cgeCodes);
    //         Map<String, Cge> cgeByCode = cges.stream().collect(Collectors.toMap(Cge::getCGECOD, c -> c));

    //         List<GbsWithCgeDto> result = bolsas.stream().map(g -> {
    //             Cge cge = cgeByCode.get(g.getCGECOD());
    //             String cgeCic = cge != null && cge.getCGECIC() != null ? String.valueOf(cge.getCGECIC()) : null;
    //             return new GbsWithCgeDto(
    //                 g.getCGECOD(), cge != null ? cge.getCGEDES() : null, cgeCic,
    //                 g.getGBSREF(), g.getGBSOPE(), g.getGBSORG(), g.getGBSFUN(), g.getGBSECO(),
    //                 g.getGBSFOP(), g.getGBSIMP(), g.getGBSIBG(), g.getGBSIUS(), g.getGBSICO(),
    //                 g.getGBSIUT(), g.getGBSICT(), g.getGBS413()
    //             );
    //         }).collect(Collectors.toList());
 
    //          return ResponseEntity.ok(result);
    //     } catch (DataAccessException ex) {
    //         return ResponseEntity.status(HttpStatus.BAD_REQUEST)
    //                 .body("Error: " + ex.getMostSpecificCause().getMessage());
    //     }
    // }

    //adding a bolsa
    @PostMapping("/add-Bolsa")
    public ResponseEntity<?> addBolsa(
        @RequestBody List<bolsaSaveDto> items
    ) {
        try {
            List<Gbs> toSave = new ArrayList<>();
            for (bolsaSaveDto dto: items) {
                GbsId id = new GbsId(dto.ENT, dto.EJE, dto.CGECOD, dto.GBSREF);
                boolean exists = gbsRepository.existsById(id);
                if (!exists) {
                    Optional<Gbs> check = gbsRepository.findByENTAndEJEAndCGECODAndGBSECO(dto.ENT, dto.EJE, dto.CGECOD, dto.GBSECO);
                    if (check.isEmpty()) {
                        Gbs addBolsa = new Gbs();
                        addBolsa.setENT(dto.ENT);
                        addBolsa.setEJE(dto.EJE);
                        addBolsa.setCGECOD(dto.CGECOD);
                        addBolsa.setGBSREF(dto.GBSREF);
                        addBolsa.setGBSOPE(dto.GBSOPE);
                        addBolsa.setGBSORG(dto.GBSORG);
                        addBolsa.setGBSFUN(dto.GBSFUN);
                        addBolsa.setGBSECO(dto.GBSECO);
                        addBolsa.setGBSIMP(dto.GBSIMP);
                        addBolsa.setGBSIBG(dto.GBSIBG);
                        addBolsa.setGBSIUS(dto.GBSIUS);
                        addBolsa.setGBSICO(dto.GBSICO);
                        addBolsa.setGBSIUT(dto.GBSIUT);
                        addBolsa.setGBSICT(dto.GBSICT);
                        addBolsa.setGBS413(dto.GBS413);
                        toSave.add(addBolsa);
                    }
                }
            }
        
            gbsRepository.saveAll(toSave);
            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //deleting a bolsa
    @DeleteMapping("/delete-bolsa/{ent}/{eje}/{cgecod}/{gbsref}")
    public ResponseEntity<?> deleteBolsa(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable String cgecod,
        @PathVariable String gbsref
    ) {
        try {
            GbsId id = new GbsId(ent, eje, cgecod, gbsref);
            Optional<Gbs> bolsa = gbsRepository.findById(id);
            if (bolsa.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            } else {
                Double gbsiut = bolsa.get().getGBSIUT();
                if (Double.compare(gbsiut, 0.0) != 0) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("No se puede eliminar la aplicación. Está en uso");
                } else {
                    gbsRepository.deleteById(id);
                }
            }

            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }
}