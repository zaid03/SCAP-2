package com.example.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.sqlserver2.model.Afa;
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
}
