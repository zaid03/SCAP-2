package com.example.backend.sqlserver2.repository;

import com.example.backend.sqlserver2.model.Mat;
import com.example.backend.sqlserver2.model.MatId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatRepository extends JpaRepository<Mat, MatId> {

}