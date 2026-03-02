package com.example.backend.service;

import org.springframework.stereotype.Service;

import com.example.backend.dto.ServicePersonaRequest;
import com.example.backend.sqlserver2.model.Dpe;
import com.example.backend.sqlserver2.model.DpeId;
import com.example.backend.sqlserver2.repository.DpeRepository;

import jakarta.transaction.Transactional;

@Service
public class DpePersonasForService {
    private final DpeRepository dpeRepository;

    public DpePersonasForService(DpeRepository dpeRepository) {
        this.dpeRepository = dpeRepository;
    }

    @Transactional
    public void saveServicePersonas(ServicePersonaRequest req) {
        if (req.getPersonas() == null || req.getPersonas().isEmpty()) {
            return;
        }

        Integer ent = req.getEnt();
        String eje = req.getEje();
        String depcod = req.getDepcod();

        for (String percod : req.getPersonas()) {
            DpeId id = new DpeId(ent, eje, percod, depcod);
            if (dpeRepository.existsById(id)) {
                continue;
            }

            Dpe dpe = new Dpe();
            dpe.setENT(ent);
            dpe.setEJE(eje);
            dpe.setPERCOD(percod);
            dpe.setDEPCOD(depcod);

            dpeRepository.save(dpe);
        }
    }
}