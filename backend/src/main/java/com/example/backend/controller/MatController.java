package com.example.backend.controller;

import com.example.backend.dto.MatShortDto;
import com.example.backend.sqlserver2.model.Mat;
import com.example.backend.sqlserver2.model.Mta;
import com.example.backend.sqlserver2.repository.MatRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataAccessException;

import java.util.stream.Collectors;
import java.util.Objects;
import java.util.function.Function;

import java.util.List;

@RestController
@RequestMapping("/api/mat")
public class MatController {
    @Autowired
    private MatRepository matRepository;

    //selecting name for almacen
    @GetMapping("/fetch-almacen/{ent}/{depcod}")
    public ResponseEntity<?> fetchAlmacen(
        @PathVariable Integer ent,
        @PathVariable String depcod
    ) {
        try {
            var mats = matRepository.findByENT(ent);
            var uniq = mats.stream()
                .filter(m -> m.getMag() != null && depcod.equals(m.getMag().getDEPCOD()))
                .map(Mat::getMta)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Mta::getMTACOD, Function.identity(), (a, b) -> a))
                .values().stream().toList();

            if (uniq.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No resultado");

            return ResponseEntity.ok(uniq);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> testApi() {
        try {
            List<Mat> almacen = matRepository.findAll();

            return ResponseEntity.ok(almacen);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + ex.getMostSpecificCause().getMessage());
        }
    }
}
