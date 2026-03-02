package com.example.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.MenuDto;
import com.example.backend.sqlserver1.repository.RpmRepository;

@RestController
@RequestMapping("/api/mnucods")
@CrossOrigin(origins = "*")
public class RpmController {

    @Autowired
    private RpmRepository rpmRepository;

    @GetMapping
    public List<MenuDto> getMnucodsByPERCOD(@RequestParam String PERCOD) {
    return rpmRepository.findMNUCODsByPERCOD(PERCOD)
            .stream()
            .map(MenuDto::new)
            .collect(java.util.stream.Collectors.toList());
    }
}
