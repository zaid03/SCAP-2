package com.example.backend.controller;

import com.example.backend.sqlserver2.model.Art;
import com.example.backend.sqlserver2.repository.ArtRepository;
import com.example.backend.sqlserver2.repository.AsuRepository;
import com.example.backend.sqlserver2.repository.AfaRepository;
import com.example.backend.dto.ArtAsuContratoProjection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/art")
public class ArtController {

    @Autowired
    private ArtRepository artRepository;
    @Autowired
    private AfaRepository afaRepository;
    @Autowired
    private AsuRepository asuRepository;

    // Method to find Art records by ENT and AFACOD and artcod
    @GetMapping("/by-ent/{ent}/{afacod}/{asucod}/{artcod}")
    public ResponseEntity<?> getByEntAfacodAsucodArtcod(
            @PathVariable int ent,
            @PathVariable String afacod,
            @PathVariable String asucod,
            @PathVariable String artcod
    ) {
        try {
            List<Art> byAfacod = artRepository.findByENTAndAFACOD(ent, afacod);
            List<Art> byAsucod = artRepository.findByENTAndASUCOD(ent, asucod);
            List<Art> byArtcod = artRepository.findByENTAndARTCOD(ent, artcod);
            
            List<Art> combined = Stream.concat(
                Stream.concat(byAfacod.stream(), byAsucod.stream()),
                byArtcod.stream()
            )
            .distinct()
            .toList();

            return ResponseEntity.ok(combined);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    // Method to find art records by ent and artdes like
    @GetMapping("/by-ent-like/{ent}/{artdes}")
    public ResponseEntity<?> getByEntAndArtdesLike(
            @PathVariable int ent,
            @PathVariable String artdes
    ) {
        try {
            List<Art> articulos = artRepository.findByENTAndARTDESContaining(ent, artdes);
            if(articulos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Sin resultado");
            }
            return ResponseEntity.ok(articulos);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //find an art name
    @GetMapping("/art-name/{ent}/{afacod}/{asucod}/{artcod}")
    public ResponseEntity<?> getArtName(
        @PathVariable int ent,
        @PathVariable String afacod,
        @PathVariable String asucod,
        @PathVariable String artcod
    ) {
        try {
            List<Art> articulo = artRepository.findByENTAndAFACODAndASUCODAndARTCOD(ent, afacod, asucod, artcod);
            if(articulo.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Sin resultado");
            }

            return ResponseEntity.ok(articulo);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //deleting a familia
    @DeleteMapping("/delete-familia/{ent}/{afacod}")
    public ResponseEntity<?> deleteFamilia(
        @PathVariable Integer ent,
        @PathVariable String afacod
    ) {
        try {
            long articulos = artRepository.countByENTAndAFACOD(ent, afacod);
            if (articulos > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede borrar una familia con artículos asociados");
            }

            asuRepository.deleteByENTAndAFACOD(ent, afacod);
            int removed = afaRepository.deleteByENTAndAFACOD(ent, afacod);
            return removed == 0
                ? ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Sin resultado")
                : ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //to delete a subfamilia
    @DeleteMapping("/delete-sub-familia/{ent}/{afacod}/{asucod}")
    public ResponseEntity<?> deleteSubFamilia(
        @PathVariable Integer ent,
        @PathVariable String afacod,
        @PathVariable String asucod
    ) {
        try {
            long articulos = artRepository.countByENTAndASUCOD(ent, asucod);
            if (articulos > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede borrar una subfamilia con artículos asociados");
            }

            int removed = asuRepository.deleteByENTAndAFACODAndASUCOD(ent, afacod, asucod);
            return removed == 0
                ? ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Sin resultado")
                : ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //selecting articulos for contratos
    @GetMapping("/art-cont/{ent}/{conlot}")
    public ResponseEntity<?> getArticulosContratos(
        @PathVariable int ent,
        @PathVariable String conlot
    ) {
        try {
            List<ArtAsuContratoProjection> articulos = artRepository.findDistinctByENTAndAsuASUECO(ent, conlot);
            if(articulos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Sin resultado");
            }

            return ResponseEntity.ok(articulos);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //searching in articulos for contratos by nums
    @GetMapping("/search-art-cont/{ent}/{conlot}/{term}")
    public ResponseEntity<?> searchArticulosContratosNum(
        @PathVariable int ent,
        @PathVariable String conlot,
        @PathVariable String term
    ) {
        try {
            List<ArtAsuContratoProjection> articulos = artRepository.findDistinctByENTAndAsuASUECOAndAFACODOrENTAndAsuASUECOAndASUCOD(ent, conlot, term, ent, conlot, term);
            if(articulos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Sin resultado");
            }

            return ResponseEntity.ok(articulos);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    //selecting articulos for contratos
    @GetMapping("/search-art-cont-des/{ent}/{conlot}/{artdes}")
    public ResponseEntity<?> searchArticulosContratosDes(
        @PathVariable int ent,
        @PathVariable String conlot,
        @PathVariable String artdes
    ) {
        try {
            List<ArtAsuContratoProjection> articulos = artRepository.findDistinctByENTAndAsuASUECOAndARTDESContaining(ent, conlot, artdes);
            if(articulos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Sin resultado");
            }

            return ResponseEntity.ok(articulos);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }
}