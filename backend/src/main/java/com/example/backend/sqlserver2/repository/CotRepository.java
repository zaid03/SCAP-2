package com.example.backend.sqlserver2.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.sqlserver2.model.Cot;
import com.example.backend.sqlserver2.model.CotId;

public interface CotRepository extends JpaRepository<Cot, CotId> {
   
}