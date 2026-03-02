package com.example.backend.sqlserver2.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.sqlserver2.model.Asu;
import com.example.backend.sqlserver2.model.AsuId;

@Repository
public interface AsuRepository extends JpaRepository<Asu, AsuId> {

    //fetching subfamilias and search them
    List<Asu> findByENTAndAFACOD(int ent, String afacod);
    List<Asu> findByENTAndASUCOD(int ent, String asucod);

    // Method to find Asu records by ENT and ASUCOD like
    List<Asu> findByENTAndASUDESContaining(int ent, String asudes);

    //find an art name to add subs
    List<Asu> findByENTAndAFACODAndASUCOD(int ENT, String AFACOD, String ASUCOD);

    //for deleting familias
    @Modifying
    @Transactional
    int deleteByENTAndAFACOD(Integer ent, String afacod);

    //to delete a subfamilia
    @Modifying
    @Transactional
    int deleteByENTAndAFACODAndASUCOD(Integer ENT, String AFACOD, String ASUCOD);
}
