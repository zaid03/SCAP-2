package com.example.backend.sqlserver2.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.sqlserver2.model.Dpe;
import com.example.backend.sqlserver2.model.DpeId;
import com.example.backend.dto.ServiceMagsProjection;

@Repository
public interface DpeRepository extends JpaRepository<Dpe, DpeId> {
    //needed for copy perfil function and selecting centro getor for login and selecting a persona's services
    List<Dpe> findByENTAndEJEAndPERCOD(Integer ENT, String EJE, String PERCOD);

    //fetch for C.exitencias almacen
    List<ServiceMagsProjection> findbyENTAndEJEAndPERCODAndCGECOD(Integer ent, String eje, String percod, String cgecod);
}