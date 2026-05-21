package com.example.backend.sqlserver2.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.dto.AnaliticaArticulosProjectin;
import com.example.backend.sqlserver2.model.Art;
import com.example.backend.sqlserver2.model.ArtId;

@Repository
public interface ArtRepository extends JpaRepository<Art, ArtId> {
    //main select for analitica de articulos
    List<AnaliticaArticulosProjectin> findByENT(Integer ent);
}