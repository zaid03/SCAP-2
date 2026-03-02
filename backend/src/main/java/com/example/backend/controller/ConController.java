package com.example.backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.backend.dto.ContratoDto;
import com.example.backend.service.CotContratoProjection;
import com.example.backend.sqlserver2.repository.CotRepository;
import com.example.backend.sqlserver2.model.Cot;
import com.example.backend.sqlserver2.model.Conn;
import com.example.backend.sqlserver2.model.ConId;
import com.example.backend.sqlserver2.repository.ConRepository;

@RestController
@RequestMapping("/api/con")
public class ConController {
    @Autowired
    private CotRepository cotRepository;
    @Autowired
    private ConRepository conRepository;

    //selecting all contratos
    @GetMapping("/fetch-contratos/{ent}/{eje}")
    public ResponseEntity<?> fetchContratos(
        @PathVariable Integer ent,
        @PathVariable String eje
    ) {
        try {
            List<CotContratoProjection> rows = cotRepository.findAllProjectedByConnCONTIPAndConnENTAndConnEJE(3, ent, eje);
            if (rows == null || rows.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            List<ContratoDto> dto = rows.stream().map(p -> {
                CotContratoProjection.ConnInfo c = p.getConn();
                CotContratoProjection.TerInfo t = p.getTer();
                return new ContratoDto(
                    c.getCONCOD(),
                    c.getCONLOT(),
                    c.getCONDES(),
                    c.getCONFIN(),
                    c.getCONFFI(),
                    c.getCONBLO(),
                    t.getTERCOD(),
                    t.getTERNOM()
                );
            }).collect(Collectors.toList());

            return ResponseEntity.ok(dto);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //search by concod bloqueado
    @GetMapping("/searchByCodigoBloque/{ent}/{eje}/{concod}")
    public ResponseEntity<?> searchContratosCodigoBloqueado(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable Integer concod
    ) {
        try {
            List<CotContratoProjection> rows = cotRepository.findAllProjectedByConnCONTIPAndConnENTAndConnEJEAndConnCONCODAndConnCONBLO(3, ent, eje, concod, 0);
            if (rows == null || rows.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            List<ContratoDto> dto = rows.stream().map(p -> {
                CotContratoProjection.ConnInfo c = p.getConn();
                CotContratoProjection.TerInfo t = p.getTer();
                return new ContratoDto(
                    c.getCONCOD(),
                    c.getCONLOT(),
                    c.getCONDES(),
                    c.getCONFIN(),
                    c.getCONFFI(),
                    c.getCONBLO(),
                    t.getTERCOD(),
                    t.getTERNOM()
                );
            }).collect(Collectors.toList());

            return ResponseEntity.ok(dto);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //search by concod no bloqueado
    @GetMapping("/searchByCodigoNoBloque/{ent}/{eje}/{concod}")
    public ResponseEntity<?> searchContratosCodigoNoBloqueado(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable Integer concod
    ) {
        try {
            List<CotContratoProjection> rows = cotRepository.findAllProjectedByConnCONTIPAndConnENTAndConnEJEAndConnCONCODAndConnCONBLONot(3, ent, eje, concod, 0);
            if (rows == null || rows.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            List<ContratoDto> dto = rows.stream().map(p -> {
                CotContratoProjection.ConnInfo c = p.getConn();
                CotContratoProjection.TerInfo t = p.getTer();
                return new ContratoDto(
                    c.getCONCOD(),
                    c.getCONLOT(),
                    c.getCONDES(),
                    c.getCONFIN(),
                    c.getCONFFI(),
                    c.getCONBLO(),
                    t.getTERCOD(),
                    t.getTERNOM()
                );
            }).collect(Collectors.toList());

            return ResponseEntity.ok(dto);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //search by concod todos
    @GetMapping("/searchByCodigoTodos/{ent}/{eje}/{concod}")
    public ResponseEntity<?> searchContratosCodigoTodos(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable Integer concod
    ) {
        try {
            List<CotContratoProjection> rows = cotRepository.findAllProjectedByConnCONTIPAndConnENTAndConnEJEAndConnCONCOD(3, ent, eje, concod);
            if (rows == null || rows.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            List<ContratoDto> dto = rows.stream().map(p -> {
                CotContratoProjection.ConnInfo c = p.getConn();
                CotContratoProjection.TerInfo t = p.getTer();
                return new ContratoDto(
                    c.getCONCOD(),
                    c.getCONLOT(),
                    c.getCONDES(),
                    c.getCONFIN(),
                    c.getCONFFI(),
                    c.getCONBLO(),
                    t.getTERCOD(),
                    t.getTERNOM()
                );
            }).collect(Collectors.toList());

            return ResponseEntity.ok(dto);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //search by condes bloqueado
    @GetMapping("/searchByDescBloque/{ent}/{eje}/{condes}")
    public ResponseEntity<?> searchContratosDescBloqueado(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable String condes
    ) {
        try {
            List<CotContratoProjection> rows = cotRepository.findAllProjectedByConnCONTIPAndConnENTAndConnEJEAndConnCONDESContainingAndConnCONBLO(3, ent, eje, condes, 0);
            if (rows == null || rows.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            List<ContratoDto> dto = rows.stream().map(p -> {
                CotContratoProjection.ConnInfo c = p.getConn();
                CotContratoProjection.TerInfo t = p.getTer();
                return new ContratoDto(
                    c.getCONCOD(),
                    c.getCONLOT(),
                    c.getCONDES(),
                    c.getCONFIN(),
                    c.getCONFFI(),
                    c.getCONBLO(),
                    t.getTERCOD(),
                    t.getTERNOM()
                );
            }).collect(Collectors.toList());

            return ResponseEntity.ok(dto);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //search by condes no bloqueado
    @GetMapping("/searchByDescNoBloque/{ent}/{eje}/{condes}")
    public ResponseEntity<?> searchContratosDescNoBloqueado(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable String condes
    ) {
        try {
            List<CotContratoProjection> rows = cotRepository.findAllProjectedByConnCONTIPAndConnENTAndConnEJEAndConnCONDESContainingAndConnCONBLONot(3, ent, eje, condes, 0);
            if (rows == null || rows.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            List<ContratoDto> dto = rows.stream().map(p -> {
                CotContratoProjection.ConnInfo c = p.getConn();
                CotContratoProjection.TerInfo t = p.getTer();
                return new ContratoDto(
                    c.getCONCOD(),
                    c.getCONLOT(),
                    c.getCONDES(),
                    c.getCONFIN(),
                    c.getCONFFI(),
                    c.getCONBLO(),
                    t.getTERCOD(),
                    t.getTERNOM()
                );
            }).collect(Collectors.toList());

            return ResponseEntity.ok(dto);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //search by condes todos
    @GetMapping("/searchByDescTodos/{ent}/{eje}/{condes}")
    public ResponseEntity<?> searchContratosDescTodos(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable String condes
    ) {
        try {
            List<CotContratoProjection> rows = cotRepository.findAllProjectedByConnCONTIPAndConnENTAndConnEJEAndConnCONDESContaining(3, ent, eje, condes);
            if (rows == null || rows.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            List<ContratoDto> dto = rows.stream().map(p -> {
                CotContratoProjection.ConnInfo c = p.getConn();
                CotContratoProjection.TerInfo t = p.getTer();
                return new ContratoDto(
                    c.getCONCOD(),
                    c.getCONLOT(),
                    c.getCONDES(),
                    c.getCONFIN(),
                    c.getCONFFI(),
                    c.getCONBLO(),
                    t.getTERCOD(),
                    t.getTERNOM()
                );
            }).collect(Collectors.toList());

            return ResponseEntity.ok(dto);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //search by bloqueado all
    @GetMapping("/searchByBloqu/{ent}/{eje}")
    public ResponseEntity<?> searchContratosBloqu(
        @PathVariable Integer ent,
        @PathVariable String eje
    ) {
        try {
            List<CotContratoProjection> rows = cotRepository.findAllProjectedByConnCONTIPAndConnENTAndConnEJEAndConnCONBLO(3, ent, eje, 0);
            if (rows == null || rows.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            List<ContratoDto> dto = rows.stream().map(p -> {
                CotContratoProjection.ConnInfo c = p.getConn();
                CotContratoProjection.TerInfo t = p.getTer();
                return new ContratoDto(
                    c.getCONCOD(),
                    c.getCONLOT(),
                    c.getCONDES(),
                    c.getCONFIN(),
                    c.getCONFFI(),
                    c.getCONBLO(),
                    t.getTERCOD(),
                    t.getTERNOM()
                );
            }).collect(Collectors.toList());

            return ResponseEntity.ok(dto);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //search by bloqueado all
    @GetMapping("/searchByNobloq/{ent}/{eje}")
    public ResponseEntity<?> searchContratosNobloq(
        @PathVariable Integer ent,
        @PathVariable String eje
    ) {
        try {
            List<CotContratoProjection> rows = cotRepository.findAllProjectedByConnCONTIPAndConnENTAndConnEJEAndConnCONBLONot(3, ent, eje, 0);
            if (rows == null || rows.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            List<ContratoDto> dto = rows.stream().map(p -> {
                CotContratoProjection.ConnInfo c = p.getConn();
                CotContratoProjection.TerInfo t = p.getTer();
                return new ContratoDto(
                    c.getCONCOD(),
                    c.getCONLOT(),
                    c.getCONDES(),
                    c.getCONFIN(),
                    c.getCONFFI(),
                    c.getCONBLO(),
                    t.getTERCOD(),
                    t.getTERNOM()
                );
            }).collect(Collectors.toList());

            return ResponseEntity.ok(dto);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //update a contrato
    public record CUpdate(Integer CONBLO, LocalDateTime CONFIN, LocalDateTime CONFFI, String CONDES) {}
    @PatchMapping("/update-contrato/{ent}/{eje}/{concod}")
    public ResponseEntity<?> updateContrato(
        @PathVariable Integer ent,
        @PathVariable String eje,
        @PathVariable Integer concod,
        @RequestBody CUpdate payload
    ) {
        try {
            if(payload == null || payload.CONDES() == null) {
                return ResponseEntity.badRequest().body("Faltan datos obligatorios.");
            }

            ConId id = new ConId(ent, eje, concod);
            Optional<Conn> contrato = conRepository.findById(id);
            if (contrato.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            Conn conUpdate = contrato.get();
            conUpdate.setCONBLO(payload.CONBLO());
            conUpdate.setCONFIN(payload.CONFIN());
            conUpdate.setCONFFI(payload.CONFFI());
            conUpdate.setCONDES(payload.CONDES());
            conRepository.save(conUpdate);

            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //add a contrato
    private int nextConcod(Integer ent, String eje) {
        return conRepository.findFirstByENTAndEJEOrderByCONCODDesc(ent, eje)
            .map(conn -> (conn.getCONCOD() == null ? 0 : conn.getCONCOD()) + 1)
            .orElse(1);
    }
    public record CAdd(Integer ENT, String EJE, String CONLOT, Integer CONBLO, LocalDateTime CONFIN, LocalDateTime CONFFI, String CONDES, Integer TERCOD) {}
    @PostMapping("/add-contrato")
    public ResponseEntity<?> addContrato(
        @RequestBody CAdd payload
    ) {
        try {
            if(payload == null || payload.ENT() == null || payload.EJE() == null || payload.CONDES() == null || payload.CONLOT() == null || payload.TERCOD() == null) {
                return ResponseEntity.badRequest().body("Faltan datos obligatorios.");
            }

            int concod = nextConcod(payload.ENT(), payload.EJE());
            Conn conAdd = new Conn();
            conAdd.setENT(payload.ENT());
            conAdd.setEJE(payload.EJE());
            conAdd.setCONCOD(concod);
            conAdd.setCONLOT(payload.CONLOT());
            conAdd.setCONBLO(payload.CONBLO());
            conAdd.setCONFIN(payload.CONFIN());
            conAdd.setCONFFI(payload.CONFFI());
            conAdd.setCONDES(payload.CONDES());
            conRepository.save(conAdd);

            Cot cotAdd = new Cot();
            cotAdd.setENT(payload.ENT());
            cotAdd.setEJE(payload.EJE());
            cotAdd.setCONCOD(concod);
            cotAdd.setTERCOD(payload.TERCOD());
            cotRepository.save(cotAdd);

            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }
}