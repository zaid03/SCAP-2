package com.example.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.ArticulosPorAlmcenProjection;
import com.example.backend.sqlserver2.repository.MeaRepository;

@RestController
@RequestMapping("/api/mea")
public class MeaController {
    @Autowired
    private MeaRepository meaRepository;

    private static final String SIN_RESULTADO = "Sin resultado";
    private static final String ERROR = "Error :";
    private static final int PAGE_SIZE = 20;

    //selecting existencias for articles
    @GetMapping("/fetch-articulos-por-almacen/{ent}")
    public ResponseEntity<?> fetchArticulosPorAlmacen(
        @PathVariable Integer ent,
        @RequestParam(defaultValue = "0") int page
    ) {
        try {
            List<ArticulosPorAlmcenProjection> Almacenes = meaRepository.findByENT(ent, PageRequest.of(page, PAGE_SIZE));
            if (Almacenes.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(SIN_RESULTADO);
            }

            return ResponseEntity.ok(Almacenes);
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
            Integer pagination = meaRepository.countByENT(ent);
            if (pagination == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(SIN_RESULTADO);
            }

            return ResponseEntity.ok(pagination);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERROR + ex.getMessage());
        }
    }

    //search query for articulos por almacen
    @GetMapping("/search-articulos/{ent}")
    public ResponseEntity<?> searchArticulos(
        @PathVariable Integer ent,
        @RequestParam(required = false) String mainSearch,
        @RequestParam(required = false) String afaCod,
        @RequestParam(required = false) String asuCod,
        @RequestParam(defaultValue = "Todos") String bloqueado,
        @RequestParam(required = false) String almacen,
        @RequestParam(defaultValue = "0") int page
    ) {
        try {
            List<ArticulosPorAlmcenProjection> articulos = meaRepository.searchArticulos(
                ent,
                mainSearch,
                afaCod,
                asuCod,
                bloqueado,
                almacen,
                PageRequest.of(page, PAGE_SIZE)
            );
            
            if (articulos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(SIN_RESULTADO);
            }

            return ResponseEntity.ok(articulos);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERROR + ex.getMessage());
        }
    }
}