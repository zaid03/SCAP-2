package com.example.backend.controller;

import com.example.backend.dto.AnaliticaArticulosProjectin;
import com.example.backend.sqlserver2.model.ArtId;
import com.example.backend.sqlserver2.model.Mea;
import com.example.backend.sqlserver2.model.Art;
import com.example.backend.sqlserver2.repository.ArtRepository;
import com.example.backend.sqlserver2.repository.AsuRepository;
import com.example.backend.sqlserver2.repository.MeaRepository;
import com.example.backend.sqlserver2.repository.AfaRepository;
import com.example.backend.service.ExistenciasSearch;
import com.example.backend.dto.ArticleProjection;
import com.example.backend.dto.magcodOnly;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/art")
public class ArtController {
    @Autowired
    private ArtRepository artRepository;
    @Autowired
    private AfaRepository afaRepository;
    @Autowired
    private AsuRepository asuRepository;
    @Autowired
    private MeaRepository meaRepository;
    @Autowired
    private ExistenciasSearch existenciasSearch; 
    
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

    //updating an articulo
    public record updateArticulo (String ARTDES, String ARTREF, Integer ARTBLO, String AUNCOD, Double ARTUCO, Double ARTUEM, Double ARTMIN, Double ARTOPT) {}
    @PatchMapping("/update-art/{ent}/{afacod}/{asucod}/{artcod}")
    public ResponseEntity<?> updateArticle(
        @PathVariable Integer ent,
        @PathVariable String afacod,
        @PathVariable String asucod,
        @PathVariable String artcod,
        @RequestBody updateArticulo payload
    ) {
        try {
            if (payload == null || ent == null || afacod == null || asucod == null || artcod == null) {
                return ResponseEntity.badRequest().body("Faltan datos obligatorios.");
            }

            ArtId id = new ArtId(ent, afacod, asucod, artcod);
            Optional<Art> artUpdate = artRepository.findById(id);
            if (artUpdate.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            Art articulo = artUpdate.get();
            articulo.setARTDES(payload.ARTDES());
            articulo.setARTREF(payload.ARTREF());
            articulo.setARTBLO(payload.ARTBLO());
            articulo.setAUNCOD(payload.AUNCOD());
            articulo.setARTUCO(payload.ARTUCO());
            articulo.setARTUEM(payload.ARTUEM());
            articulo.setARTMIN(payload.ARTMIN());
            articulo.setARTOPT(payload.ARTOPT());
            artRepository.save(articulo);

            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + ex.getMessage());
        }
    }

    //deleting an articulo
    @DeleteMapping("/delete-art/{ent}/{afacod}/{asucod}/{artcod}")
    public ResponseEntity<?> artDelete (
        @PathVariable Integer ent,
        @PathVariable String afacod,
        @PathVariable String asucod,
        @PathVariable String artcod
    ) {
        try {
            if (ent == null || afacod == null || asucod == null || artcod == null) {
                return ResponseEntity.badRequest().body("Faltan datos obligatorios.");
            }

            ArtId id = new ArtId(ent, afacod, asucod, artcod);
            Optional<Art> artDelete = artRepository.findById(id);
            if (artDelete.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            Art articulo = artDelete.get();
            articulo.setARTBLO(1);
            artRepository.save(articulo);

            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + ex.getMessage());
        }
    }

    //adding an articulo
    public record addArt (Integer ENT, String AFACOD, String ASUCOD, String ARTCOD, String ARTDES, String ARTREF, Integer ARTBLO, Double ARTUNI, Double ARTSOL, Double ARTREC, String AUNCOD, Double ARTUCO, Double ARTUEM, Double ARTPMP, Double ARTMIN, Double ARTOPT) {}
    @PostMapping("/add-art")
    public ResponseEntity<?> artAdd (
        @RequestBody addArt payload
    ) {
        try {
            if (payload == null || payload.ENT() == null || payload.AFACOD() == null || payload.ASUCOD() == null || payload.ARTCOD() == null || payload.ARTDES() == null) {
                return ResponseEntity.badRequest().body("Faltan datos obligatorios.");
            }

            ArtId id = new ArtId(payload.ENT(), payload.AFACOD(), payload.ASUCOD(), payload.ARTCOD());
            Optional<Art> articuloCheck = artRepository.findById(id);

            if (!articuloCheck.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El artículo ya existe.");
            }

            Art articulo = new Art();
            articulo.setENT(payload.ENT());
            articulo.setAFACOD(payload.AFACOD());
            articulo.setASUCOD(payload.ASUCOD());
            articulo.setARTCOD(payload.ARTCOD());
            articulo.setARTDES(payload.ARTDES());
            articulo.setARTREF(payload.ARTREF());
            articulo.setARTBLO(payload.ARTBLO());
            articulo.setARTUNI(payload.ARTUNI());
            articulo.setARTSOL(payload.ARTSOL());
            articulo.setARTREC(payload.ARTREC());
            articulo.setAUNCOD(payload.AUNCOD());
            articulo.setARTUCO(payload.ARTUCO());
            articulo.setARTUEM(payload.ARTUEM());
            articulo.setARTPMP(payload.ARTPMP());
            articulo.setARTMIN(payload.ARTMIN());
            articulo.setARTOPT(payload.ARTOPT());
            artRepository.save(articulo);

            List<magcodOnly> magcods = asuRepository.findMagcods(payload.ENT(), payload.AFACOD, payload.ASUCOD);

            if (!magcods.isEmpty()) {
                for (magcodOnly magcod: magcods) {
                    Mea meaAdd = new Mea();
                    meaAdd.setENT(payload.ENT());
                    meaAdd.setMAGCOD(magcod.getMAGCOD());
                    meaAdd.setAFACOD(payload.AFACOD());
                    meaAdd.setASUCOD(payload.ASUCOD());
                    meaAdd.setARTCOD(payload.ARTCOD());
                    meaAdd.setMEAUNI(payload.ARTUNI());
                    meaAdd.setMEAMIN(payload.ARTMIN());
                    meaAdd.setMEASOL(payload.ARTSOL());
                    meaAdd.setMEAOPT(payload.ARTOPT());
                    meaAdd.setMEAPMP(payload.ARTPMP());
                    meaAdd.setMEAPMI(0.00);
                    meaAdd.setMEAREC(payload.ARTREC());
                    meaAdd.setMEAIND(0);
                    meaRepository.save(meaAdd);
                }
            }

            return ResponseEntity.noContent().build();
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERROR + ex.getMostSpecificCause().getMessage());
        }
    }

    //main fetch for C.general existencias
    @GetMapping("/Existencias/{ent}")
    public ResponseEntity<?> fetchExistencias(
        @PathVariable Integer ent
    ) {
        try {
            System.out.println("Controller reached");
            List<ArticleProjection> existencias = artRepository.findByENTAndARTBLONot(ent, 0);
            if (existencias.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El artículo ya existe.");
            }

            return ResponseEntity.ok(existencias);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERROR + ex.getMostSpecificCause().getMessage());
        }
    }

    //search in C.general existencias
    @GetMapping("/existencias/{ent}/search")
    public ResponseEntity<?> searchExistenciasQuery(
        @PathVariable Integer ent,
        @RequestParam(required = false) String campo,
        @RequestParam(required = false) String afacod,
        @RequestParam(required = false) String asucod
    ) {
        try {
            List<ArticleProjection> existencias = existenciasSearch.searchExistencias(ent, campo, afacod, asucod);
            if (existencias.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(SIN_RESULTADO);
            }

            return ResponseEntity.ok(existencias);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERROR + ex.getMostSpecificCause().getMessage());
        }
    }
}