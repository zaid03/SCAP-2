package com.example.backend.controller;

import com.example.backend.dto.AnaliticaArticulosProjectin;
import com.example.backend.sqlserver2.repository.ArtRepository;
import com.example.backend.sqlserver2.repository.AsuRepository;
import com.example.backend.sqlserver2.repository.AfaRepository;
import com.example.backend.dto.ArticleProjection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.util.List;

@RestController
@RequestMapping("/api/art")
public class ArtController {
    @Autowired
    private ArtRepository artRepository;
    @Autowired
    private AfaRepository afaRepository;
    @Autowired
    private AsuRepository asuRepository;
    
    private static final String SIN_RESULTADO = "Sin resultado";
    private static final String ERROR = "Error :";
    private static final int PAGE_SIZE = 20;

    //main select for analitica de articulos
    @GetMapping("/fetch-analitica-articulos/{ent}")
    public ResponseEntity<?> fetchAnaliticaArticulos(
        @PathVariable Integer ent
    ) {
        try {
            List<AnaliticaArticulosProjectin> articulos = artRepository.findByENT(ent);

            if (articulos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(SIN_RESULTADO);
            }

            return ResponseEntity.ok(articulos);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ERROR + ex.getMostSpecificCause().getMessage());
        }
    }

    //fetching articulos for consulta general
    @GetMapping("/fetch-consulta-general/{ent}")
    public ResponseEntity<?> fetchConsultaGeneral(
        @PathVariable Integer ent,
        @RequestParam(defaultValue = "0") int page
    ) {
        try {
            List<ArticleProjection> articles = artRepository.findByENT(ent, PageRequest.of(page, PAGE_SIZE));
            if (articles.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(SIN_RESULTADO);
            }
            return ResponseEntity.ok(articles);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERROR + ex.getMessage());
        }
    }
    
    //getting pagination number
    @GetMapping("/get-pag/{ent}")
    public ResponseEntity<?> getPag(
        @PathVariable Integer ent
    ) {
        try {
            Integer pagination = artRepository.countByENT(ent);
            if (pagination == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(SIN_RESULTADO);
            }

            return ResponseEntity.ok(pagination);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERROR + ex.getMessage());
        }
    }
    //search in articulos general
    @GetMapping("/search/{ent}")
    public ResponseEntity<?> searchArticles(
        @PathVariable Integer ent,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String afacod,
        @RequestParam(required = false) String asucod,
        @RequestParam(defaultValue = "todos") String bloqueado
    ) {
        try {
            Page<ArticleProjection> articles = artRepository.searchArticles(
                ent, search, afacod, asucod, bloqueado, PageRequest.of(page, PAGE_SIZE)
            );
            if (articles.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(SIN_RESULTADO);
            }
            return ResponseEntity.ok(articles.getContent());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + ex.getMessage());
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
}