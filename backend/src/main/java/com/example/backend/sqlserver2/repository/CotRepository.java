package com.example.backend.sqlserver2.repository;

import com.example.backend.service.CotContratoProjection;
import com.example.backend.sqlserver2.model.Cot;
import com.example.backend.sqlserver2.model.CotId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CotRepository extends JpaRepository<Cot, CotId> {
    //selecting all contratos
    List<CotContratoProjection> findAllProjectedByConnCONTIPAndConnENTAndConnEJE(Integer CONTIP, Integer ent, String eje);

    //search options
    //search by concod bloqueado
    List<CotContratoProjection> findAllProjectedByConnCONTIPAndConnENTAndConnEJEAndConnCONCODAndConnCONBLO(Integer CONTIP, Integer ent, String eje, Integer concod, Integer conblo);

    //search by concod no bloqueado
    List<CotContratoProjection> findAllProjectedByConnCONTIPAndConnENTAndConnEJEAndConnCONCODAndConnCONBLONot(Integer CONTIP, Integer ent, String eje, Integer concod, Integer conblo);

    //search by concod todos
    List<CotContratoProjection> findAllProjectedByConnCONTIPAndConnENTAndConnEJEAndConnCONCOD(Integer CONTIP, Integer ent, String eje, Integer concod);

    //search by condes bloqueado
    List<CotContratoProjection> findAllProjectedByConnCONTIPAndConnENTAndConnEJEAndConnCONDESContainingAndConnCONBLO(Integer CONTIP, Integer ent, String eje, String condes, Integer conblo);

    //search by condes no bloqueado
    List<CotContratoProjection> findAllProjectedByConnCONTIPAndConnENTAndConnEJEAndConnCONDESContainingAndConnCONBLONot(Integer CONTIP, Integer ent, String eje, String condes, Integer conblo);

    //search by condes todos
    List<CotContratoProjection> findAllProjectedByConnCONTIPAndConnENTAndConnEJEAndConnCONDESContaining(Integer CONTIP, Integer ent, String eje, String condes);

    //search by bloqueado all
    List<CotContratoProjection> findAllProjectedByConnCONTIPAndConnENTAndConnEJEAndConnCONBLO(Integer CONTIP, Integer ent, String eje, Integer conblo);

    //search by no bloqueado all
    List<CotContratoProjection> findAllProjectedByConnCONTIPAndConnENTAndConnEJEAndConnCONBLONot(Integer CONTIP, Integer ent, String eje, Integer conblo);
}