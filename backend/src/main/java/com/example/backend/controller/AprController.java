package com.example.backend.controller;

import com.example.backend.dto.ProveedoresArticleProjection;
import com.example.backend.sqlserver2.repository.AprRepository;
import com.example.backend.sqlserver2.model.AprId;
import com.example.backend.sqlserver2.model.Apr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

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
    
    //modifying an article's proveedor 
    public record provUpdate(String APRREF, String APROBS, Double APRUEM) {}
    @PatchMapping("/update-prov-info/{ent}/{afacod}/{asucod}/{artcod}/{tercod}")
    public ResponseEntity<?>  proveedorInfoUpdate(
        @PathVariable Integer ent,
        @PathVariable String afacod,
        @PathVariable String asucod,
        @PathVariable String artcod,
        @PathVariable Integer tercod,
        @RequestBody provUpdate payload
    ) {
        try {
            if (payload == null) {
                return ResponseEntity.badRequest().body("Faltan datos obligatorios.");
            }

            AprId id = new AprId(ent, tercod, afacod, asucod, artcod);
            Optional<Apr> aprCheck = aprRepository.findById(id);

            if (aprCheck.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sin resultado");
            }

            Apr proveedorInfo = aprCheck.get();
            proveedorInfo.setAPRREF(payload.APRREF());
            proveedorInfo.setAPROBS(payload.APROBS());
            proveedorInfo.setAPRUEM(payload.APRUEM());
            aprRepository.save(proveedorInfo);

            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERROR + ex.getMessage());
        }
    }

    //deleting a proveedor from an article
    @DeleteMapping("delete-proveedor-art/{ent}/{afacod}/{asucod}/{artcod}/{tercod}")
    public ResponseEntity<?> artProveedorDelete(
        @PathVariable Integer ent,
        @PathVariable String afacod,
        @PathVariable String asucod,
        @PathVariable String artcod,
        @PathVariable Integer tercod
    ) {
        try {
            AprId id = new AprId(ent, tercod, afacod, asucod, artcod);
            if (!aprRepository.existsById(id)) {
                return ResponseEntity.badRequest().body("Proveedor no existe");
            }

            aprRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERROR + ex.getMessage());
        }
    }

    //adding proveedores to an articulo
    public record provsAdd(Integer ENT, String AFACOD, String ASUCOD, String ARTCOD, List<Integer> tercods) {};
    @PostMapping("/add-terceros")
    public ResponseEntity<?> tercerodAdd (
        @RequestBody provsAdd payload
    ) {
        try {
            if (payload == null || payload.ENT() == null || payload.AFACOD() == null || payload.ASUCOD() == null || payload.ARTCOD() == null || payload.tercods() == null) {
                return ResponseEntity.badRequest().body("Faltan datos obligatorios.");
            }

            Integer count = 0;
            for(Integer tercod : payload.tercods()) {
                AprId id = new AprId(payload.ENT(), tercod, payload.AFACOD() ,payload.ASUCOD(), payload.ARTCOD());
                if (!aprRepository.existsById(id)) {
                    Apr articuloAdd = new Apr();
                    articuloAdd.setENT(payload.ENT());
                    articuloAdd.setAFACOD(payload.AFACOD());
                    articuloAdd.setASUCOD(payload.ASUCOD());
                    articuloAdd.setARTCOD(payload.ARTCOD());
                    articuloAdd.setTERCOD(tercod);
                    articuloAdd.setAPRACU(0);
                    articuloAdd.setAPRPRE(0.00);
                    articuloAdd.setAPRUEM(0.00);
                    aprRepository.save(articuloAdd);
                    count = count + 1;
                }

            }

            return ResponseEntity.ok("Se guardaron " + count + " artículos");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERROR + ex.getMessage());
        }
    }
}
