package com.example.backend.sqlserver2.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.annotation.MergedAnnotations.Search;
import org.springframework.data.domain.Pageable;

import com.example.backend.dto.personasPorServiciosProjection;
import com.example.backend.sqlserver2.model.Dpe;
import com.example.backend.sqlserver2.model.DpeId;

@Repository
public interface DpeRepository extends JpaRepository<Dpe, DpeId> {
    //inserting services
    List<Dpe> findByENTAndEJEAndDEPCOD(Integer ENT, String EJE, String DEPCOD);

    //needed for copy perfil function and selecting centro getor for login and selecting a persona's services
    List<Dpe> findByENTAndEJEAndPERCOD(Integer ENT, String EJE, String PERCOD);
    
    //deleting a persona
    @Transactional
    @Modifying
    int deleteByENTAndEJEAndPERCOD(Integer ENT, String EJE, String PERCOD);

    //selecting personas por servicios
    List<personasPorServiciosProjection> findByENTAndEJE(Integer ENT, String EJE, Pageable pageable);

    //downloading as excel
    List<personasPorServiciosProjection> findByENTAndEJE(Integer ENT, String EJE);

    // ========== SINGLE FILTERS ==========
    
    // Servicio
    List<personasPorServiciosProjection> findByENTAndEJEAndDEPCODContaining(Integer ent, String eje, String depcod);
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPDESContaining(Integer ent, String eje, String depdes);
    
    // Persona
    List<personasPorServiciosProjection> findProjectionByENTAndEJEAndPERCOD(Integer ent, String eje, String percod);
    List<personasPorServiciosProjection> findByENTAndEJEAndPer_PERNOMContaining(Integer ent, String eje, String pernom);
    
    // Centro Gestor
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_Cge_CGECOD(Integer ent, String eje, String cgecod);
    
    // Perfil
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPALM(Integer ent, String eje, Integer depalm);
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPCOM(Integer ent, String eje, Integer depcom);
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPINT(Integer ent, String eje, Integer depint);
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPALMAndDep_DEPCOMAndDep_DEPINT(Integer ent, String eje, Integer depalm, Integer depcom, Integer depint);

    // ========== TWO FILTERS ==========
    
    // Servicio + Persona
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPDESContainingAndPer_PERNOMContaining(Integer ent, String eje, String depdes, String pernom);
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPDESContainingAndPERCOD(Integer ent, String eje, String depdes, String percod);
    List<personasPorServiciosProjection> findByENTAndEJEAndDEPCODContainingAndPer_PERNOMContaining(Integer ent, String eje, String depcod, String pernom);
    List<personasPorServiciosProjection> findByENTAndEJEAndDEPCODContainingAndPERCOD(Integer ent, String eje, String depcod, String percod);
    
    // Servicio + CGE
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPDESContainingAndDep_Cge_CGECOD(Integer ent, String eje, String depdes, String cgecod);
    List<personasPorServiciosProjection> findByENTAndEJEAndDEPCODContainingAndDep_Cge_CGECOD(Integer ent, String eje, String depcod, String cgecod);
    
    // Servicio + Perfil (DEPALM)
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPDESContainingAndDep_DEPALM(Integer ent, String eje, String depdes, Integer depalm);
    List<personasPorServiciosProjection> findByENTAndEJEAndDEPCODContainingAndDep_DEPALM(Integer ent, String eje, String depcod, Integer depalm);
    
    // Servicio + Perfil (DEPCOM)
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPDESContainingAndDep_DEPCOM(Integer ent, String eje, String depdes, Integer depcom);
    List<personasPorServiciosProjection> findByENTAndEJEAndDEPCODContainingAndDep_DEPCOM(Integer ent, String eje, String depcod, Integer depcom);
    
    // Servicio + Perfil (DEPINT)
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPDESContainingAndDep_DEPINT(Integer ent, String eje, String depdes, Integer depint);
    List<personasPorServiciosProjection> findByENTAndEJEAndDEPCODContainingAndDep_DEPINT(Integer ent, String eje, String depcod, Integer depint);
    
    // Persona + CGE
    List<personasPorServiciosProjection> findByENTAndEJEAndPer_PERNOMContainingAndDep_Cge_CGECOD(Integer ent, String eje, String pernom, String cgecod);
    List<personasPorServiciosProjection> findByENTAndEJEAndPERCODAndDep_Cge_CGECOD(Integer ent, String eje, String percod, String cgecod);
    
    // Persona + Perfil (DEPALM)
    List<personasPorServiciosProjection> findByENTAndEJEAndPer_PERNOMContainingAndDep_DEPALM(Integer ent, String eje, String pernom, Integer depalm);
    List<personasPorServiciosProjection> findByENTAndEJEAndPERCODAndDep_DEPALM(Integer ent, String eje, String percod, Integer depalm);
    
    // Persona + Perfil (DEPCOM)
    List<personasPorServiciosProjection> findByENTAndEJEAndPer_PERNOMContainingAndDep_DEPCOM(Integer ent, String eje, String pernom, Integer depcom);
    List<personasPorServiciosProjection> findByENTAndEJEAndPERCODAndDep_DEPCOM(Integer ent, String eje, String percod, Integer depcom);
    
    // Persona + Perfil (DEPINT)
    List<personasPorServiciosProjection> findByENTAndEJEAndPer_PERNOMContainingAndDep_DEPINT(Integer ent, String eje, String pernom, Integer depint);
    List<personasPorServiciosProjection> findByENTAndEJEAndPERCODAndDep_DEPINT(Integer ent, String eje, String percod, Integer depint);
    
    // CGE + Perfil
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_Cge_CGECODAndDep_DEPALM(Integer ent, String eje, String cgecod, Integer depalm);
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_Cge_CGECODAndDep_DEPCOM(Integer ent, String eje, String cgecod, Integer depcom);
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_Cge_CGECODAndDep_DEPINT(Integer ent, String eje, String cgecod, Integer depint);

    // ========== THREE FILTERS ==========
    
    // Servicio + Persona + CGE
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPDESContainingAndPer_PERNOMContainingAndDep_Cge_CGECOD(Integer ent, String eje, String depdes, String pernom, String cgecod);
    
    // Servicio + Persona + Perfil
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPDESContainingAndPer_PERNOMContainingAndDep_DEPALM(Integer ent, String eje, String depdes, String pernom, Integer depalm);
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPDESContainingAndPer_PERNOMContainingAndDep_DEPCOM(Integer ent, String eje, String depdes, String pernom, Integer depcom);
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPDESContainingAndPer_PERNOMContainingAndDep_DEPINT(Integer ent, String eje, String depdes, String pernom, Integer depint);
    
    // Servicio + CGE + Perfil
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPDESContainingAndDep_Cge_CGECODAndDep_DEPALM(Integer ent, String eje, String depdes, String cgecod, Integer depalm);
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPDESContainingAndDep_Cge_CGECODAndDep_DEPCOM(Integer ent, String eje, String depdes, String cgecod, Integer depcom);
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPDESContainingAndDep_Cge_CGECODAndDep_DEPINT(Integer ent, String eje, String depdes, String cgecod, Integer depint);
    
    // Persona + CGE + Perfil
    List<personasPorServiciosProjection> findByENTAndEJEAndPer_PERNOMContainingAndDep_Cge_CGECODAndDep_DEPALM(Integer ent, String eje, String pernom, String cgecod, Integer depalm);
    List<personasPorServiciosProjection> findByENTAndEJEAndPer_PERNOMContainingAndDep_Cge_CGECODAndDep_DEPCOM(Integer ent, String eje, String pernom, String cgecod, Integer depcom);
    List<personasPorServiciosProjection> findByENTAndEJEAndPer_PERNOMContainingAndDep_Cge_CGECODAndDep_DEPINT(Integer ent, String eje, String pernom, String cgecod, Integer depint);

    // ========== FOUR FILTERS (ALL) ==========
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPDESContainingAndPer_PERNOMContainingAndDep_Cge_CGECODAndDep_DEPALM(Integer ent, String eje, String depdes, String pernom, String cgecod, Integer depalm);
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPDESContainingAndPer_PERNOMContainingAndDep_Cge_CGECODAndDep_DEPCOM(Integer ent, String eje, String depdes, String pernom, String cgecod, Integer depcom);
    List<personasPorServiciosProjection> findByENTAndEJEAndDep_DEPDESContainingAndPer_PERNOMContainingAndDep_Cge_CGECODAndDep_DEPINT(Integer ent, String eje, String depdes, String pernom, String cgecod, Integer depint);

}