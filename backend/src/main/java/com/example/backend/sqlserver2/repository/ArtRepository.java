package com.example.backend.sqlserver2.repository;

import java.util.List;
import org.springframework.data.domain.Page;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import com.example.backend.dto.articulosExistenciasProjection;
import com.example.backend.dto.AnaliticaArticulosProjectin;
import com.example.backend.dto.ArticleProjection;
import com.example.backend.sqlserver2.model.Art;
import com.example.backend.sqlserver2.model.ArtId;

@Repository
public interface ArtRepository extends JpaRepository<Art, ArtId> {
    //main select for analitica de articulos
    List<AnaliticaArticulosProjectin> findByENT(Integer ent);

    //main fetch for consulta general de articulos
    List<ArticleProjection> findByENT(Integer ent, Pageable pageable);

    //getting pagination number
    Integer countByENT(Integer ent);
    
    //search in consulta general de articulos with all criteria
    @Query(value = "SELECT " +
        "R.AFACOD, " +
        "F.AFADES as Afa_AFADES, " +
        "R.ASUCOD, " +
        "S.ASUDES as Asu_ASUDES, " +
        "R.ARTCOD, " +
        "R.ARTDES, " +
        "R.ARTREF, " +
        "R.ARTBLO, " +
        "R.ARTUNI, " +
        "R.ARTSOL, " +
        "R.ARTREC, " +
        "U.AUNDES as Aun_AUNDES, " +
        "R.ARTUCO, " +
        "R.ARTUEM, " +
        "R.ARTPMI, " +
        "R.ARTPMP, " +
        "R.ARTMIN, " +
        "R.ARTOPT " +
        "FROM art R " +
        "INNER JOIN afa F ON R.afacod = F.afacod AND R.ent = F.ent " +
        "INNER JOIN asu S ON R.asucod = S.asucod AND R.ent = S.ent AND R.afacod = S.afacod " +
        "INNER JOIN aun U ON R.auncod = U.auncod AND R.ent = U.ent " +
        "WHERE R.ent = :ent " +
        "AND (" +
            "(:search IS NULL OR :search = '') " +
            "OR (LEN(:search) < 11 AND R.ARTCOD = :search) " +
            "OR (UPPER(R.ARTDES) LIKE UPPER('%' + :search + '%')) " +
            "OR (LEN(:search) < 31 AND R.ARTREF = :search) " +
        ") " +
        "AND (:afacod IS NULL OR :afacod = '' OR R.AFACOD = :afacod) " +
        "AND (:asucod IS NULL OR :asucod = '' OR R.ASUCOD = :asucod) " +
        "AND (" +
            ":bloqueado = 'todos' " +
            "OR (:bloqueado = 'bloqueado' AND R.ARTBLO = 0) " +
            "OR (:bloqueado = 'nobloqueado' AND R.ARTBLO <> 0) " +
        ")", 
        nativeQuery = true
    )
    Page<ArticleProjection> searchArticles(
        @Param("ent") Integer ent,
        @Param("search") String search,
        @Param("afacod") String afacod,
        @Param("asucod") String asucod,
        @Param("bloqueado") String bloqueado,
        Pageable pageable
    );

    //delete a familia check
    Long countByENTAndAFACOD(Integer ent, String afacod);

    //delete a subfamilia check
    Long countByENTAndASUCOD(Integer ent, String asucod);

    //needed for deleting a tipo de unidades
    int countByENTAndAUNCOD(Integer ent, String auncod);

    //main fetch for C.general existencias
    List<articulosExistenciasProjection> findByENTAndARTBLONot(Integer ent, Integer artblo);
}