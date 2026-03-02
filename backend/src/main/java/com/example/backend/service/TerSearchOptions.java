package com.example.backend.service;

import com.example.backend.sqlserver2.model.Ter;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;

public class TerSearchOptions {
    //for the list filtered by TERNIF and TERNOM and TERALI bloqueado
    public static Specification<Ter> searchFiltered(Integer ent, String term) {
        return (root, query, cb) -> {
            Predicate entPredicate = cb.equal(root.get("ENT"), ent);
            Predicate terbloPredicate = cb.equal(root.get("TERBLO"), 1);
            
            Predicate searchPredicate = cb.or(
                cb.like(root.get("TERNIF"), "%" + term + "%"),
                cb.like(root.get("TERNOM"), "%" + term + "%"),
                cb.like(root.get("TERALI"), "%" + term + "%")
            );
            
            return cb.and(entPredicate, terbloPredicate, searchPredicate);
        };
    }

    //for the list filtered by TERNIF and TERNOM and TERALI no bloqueado
    public static Specification<Ter> searchByTerm(Integer ent, String term) {
        return (root, query, cb) -> {
            Predicate entPredicate = cb.equal(root.get("ENT"), ent);
            Predicate terbloPredicate = cb.notEqual(root.get("TERBLO"), 1);
            
            Predicate searchPredicate = cb.or(
                cb.like(root.get("TERNIF"), "%" + term + "%"),
                cb.like(root.get("TERNOM"), "%" + term + "%"),
                cb.like(root.get("TERALI"), "%" + term + "%")
            );
            
            return cb.and(entPredicate, terbloPredicate, searchPredicate);
        };
    }

    //for the list filtered by TERNOM and TERALI bloqueado
    public static Specification<Ter> searchByNomOrAli(Integer ent, String term) {
        return (root, query, cb) -> {
            Predicate entPredicate = cb.equal(root.get("ENT"), ent);
            Predicate terbloPredicate = cb.equal(root.get("TERBLO"), 1);
            
            Predicate searchPredicate = cb.or(
                cb.like(root.get("TERNOM"), "%" + term + "%"),
                cb.like(root.get("TERALI"), "%" + term + "%")
            );
            
            return cb.and(entPredicate, terbloPredicate, searchPredicate);
        };
    }

    //for the list filtered by TERNOM and TERALI no bloqueado
    public static Specification<Ter> findMatchingNomOrAli(Integer ent, String term) {
        return (root, query, cb) -> {
            Predicate entPredicate = cb.equal(root.get("ENT"), ent);
            Predicate terbloPredicate = cb.notEqual(root.get("TERBLO"), 1);
            
            Predicate searchPredicate = cb.or(
                cb.like(root.get("TERNOM"), "%" + term + "%"),
                cb.like(root.get("TERALI"), "%" + term + "%")
            );
            
            return cb.and(entPredicate, terbloPredicate, searchPredicate);
        };
    }

    // filtering by ternif and ternom and terali
    public static Specification<Ter> searchTodos(Integer ent, String term) {
        return (root, query, cb) -> {
            Predicate entPredicate = cb.equal(root.get("ENT"), ent);
            
            Predicate searchPredicate = cb.or(
                cb.like(root.get("TERNIF"), "%" + term + "%"),
                cb.like(root.get("TERNOM"), "%" + term + "%"),
                cb.like(root.get("TERALI"), "%" + term + "%")
            );
            
            return cb.and(entPredicate, searchPredicate); // ✅ No TERBLO filter
        };
    }
}
