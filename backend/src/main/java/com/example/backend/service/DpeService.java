package com.example.backend.service;

import org.springframework.stereotype.Service;

import com.example.backend.dto.PersonaServiceRequest;
import com.example.backend.sqlserver2.model.Dpe;
import com.example.backend.sqlserver2.model.DpeId;
import com.example.backend.sqlserver2.repository.DpeRepository;

import jakarta.transaction.Transactional;

@Service
public class DpeService {

    private final DpeRepository dpeRepository;

    public DpeService(DpeRepository dpeRepository) {
        this.dpeRepository = dpeRepository;
    }

    @Transactional
    public void savePersonaServices(PersonaServiceRequest req) {

        if (req.getServices() == null || req.getServices().isEmpty()) {
            return;
        }

        Integer ent = req.getEnt();
        String eje = req.getEje();
        String percod = req.getPercod();

        for (String depcod : req.getServices()) {
            DpeId id = new DpeId(ent, eje, depcod, percod);
            if (dpeRepository.existsById(id)) {
                continue;
            }

            Dpe dpe = new Dpe();
            dpe.setENT(ent);
            dpe.setEJE(eje);
            dpe.setDEPCOD(depcod);
            dpe.setPERCOD(percod);

            dpeRepository.save(dpe);
        }
    }
}