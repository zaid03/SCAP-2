package com.example.backend.controller;

import com.example.backend.dto.Entidad;
import com.example.backend.service.SicalEntidadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sical")
@CrossOrigin(origins = "http://localhost:4200")
public class SicalEntidadController {

    private final SicalEntidadService sicalEntidadService;

    public SicalEntidadController(SicalEntidadService sicalEntidadService) {
        this.sicalEntidadService = sicalEntidadService;
    }

    @GetMapping("/entidades")
    public ResponseEntity<?> getEntidades() {
        try {
            List<Entidad> result = sicalEntidadService.getEntidades();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "SICAL service error: " + e.getMessage()));
        }
    }
}