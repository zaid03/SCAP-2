package com.example.backend.sqlserver2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.backend.sqlserver2.model.Cog;
import com.example.backend.sqlserver2.model.CogId;
import com.example.backend.dto.SaldoContrato;

import java.util.List;

public interface CogRepository extends JpaRepository<Cog, CogId> {
  //main fetch for C.saldo de contrato
  List<SaldoContrato> findByENTAndEJEAndCot_conn_CONTIPAndCot_conn_CONBLONot(Integer ent, String eje, Integer contip, Integer conblo);
}