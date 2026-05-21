package com.example.backend.controller;

import com.example.backend.dto.ProveedoresArticleProjection;
import com.example.backend.sqlserver2.repository.AprRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/more")
public class AprController {
    @Autowired
    private AprRepository aprRepository;

    private static final String SIN_RESULTADO = "Sin resultado";
    private static final String ERROR = "Error :";

    //selecting proveedores for an article
    @GetMapping("/proveedores-por-articulo/{ent}/{afacod}/{asucod}/{artcod}")
    public ResponseEntity<?> proveedoresPorArticulo (
        @PathVariable Integer ent,
        @PathVariable String afacod,
        @PathVariable String asucod,
        @PathVariable String artcod
    ) {
        try {
            List<ProveedoresArticleProjection> proveedores = aprRepository.findByENTAndAFACODAndASUCODAndARTCOD(ent, afacod, asucod, artcod);
            if(proveedores.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(SIN_RESULTADO);
            }

            return ResponseEntity.ok(proveedores);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERROR + ex.getMessage());
        }
    }
    
}
