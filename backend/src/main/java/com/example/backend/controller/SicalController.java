package com.example.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.Tercero;
import com.example.backend.service.SicalService;
import com.example.sical.CryptoSical;

@RestController
@RequestMapping("/api/sical") 
@CrossOrigin(origins = "http://localhost:4200")
public class SicalController {

    private final SicalService sicalService;

    public SicalController(SicalService sicalService) {
        this.sicalService = sicalService;
    }

    @GetMapping("/terceros")
    public ResponseEntity<?> getTerceros(
            @RequestParam(required = false) String nif,
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) String apell
    ) {
        try {
            List<Tercero> result = sicalService.getTerceros(nif, nom, apell);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/test-crypto")
    public ResponseEntity<?> testCrypto() {
        try {
            CryptoSical.SecurityFields sec = CryptoSical.calculateSecurityFields("TEST_KEY_123");
            
            return ResponseEntity.ok(Map.of(
                "status", "Crypto working",
                "created", sec.created,
                "nonce", sec.nonce,
                "tokenPreview", sec.token.substring(0, 20) + "...",
                "originPreview", sec.origin.substring(0, 30) + "..."
            ));
        } catch (Exception err) {
            return ResponseEntity.status(500).body(Map.of("Error", err.getMessage()));
        }
    }

}
