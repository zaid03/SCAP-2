package com.example.backend.service;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.example.backend.sqlserver2.model.Fac;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class FacContabilizacionSpecification {
    public static Specification<Fac> searchContabilizacion(
        Integer ent,
        String eje,
        String cgecod,
        String fechaType,          
        LocalDateTime desde,
        LocalDateTime hasta,
        Integer facann,
        String search
    ) {
        return (root, query, cb) -> {
            if (query.getResultType() == Fac.class) {
                root.fetch("ter", JoinType.LEFT);
            }

            Predicate predicate = cb.conjunction();

            predicate = cb.and(predicate, cb.equal(root.get("ENT"), ent));
            predicate = cb.and(predicate, cb.equal(root.get("EJE"), eje));
            predicate = cb.and(predicate, cb.equal(root.get("CGECOD"), cgecod));

            predicate = cb.and(predicate, cb.isNull(root.get("FACADO")));

            Expression<Double> facimpRounded = cb.function("ROUND", Double.class, root.get("FACIMP"), cb.literal(2));
            Expression<Double> sumRounded = cb.function("ROUND", Double.class, 
                cb.sum(root.get("FACIEC"), root.get("FACIDI")), cb.literal(2));
            predicate = cb.and(predicate, cb.equal(facimpRounded, sumRounded));

            String dateField = "factura".equalsIgnoreCase(fechaType) ? "FACDAT" : "FACFRE";

            if (desde != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get(dateField), desde));
            }

            if (hasta != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get(dateField), hasta));
            }

            if (facann != null) {
                predicate = cb.and(predicate, cb.equal(root.get("FACANN"), facann));
            }

            if (search != null && !search.trim().isEmpty()) {
                String trimmed = search.trim();
                boolean onlyDigits = trimmed.matches("^\\d+$");
                boolean hasLetters = trimmed.matches(".*[a-zA-Z].*");
                int length = trimmed.length();

                if (onlyDigits && length <= 5) {
                    try {
                        Integer tercod = Integer.parseInt(trimmed);
                        predicate = cb.and(predicate, cb.equal(root.get("TERCOD"), tercod));
                    } catch (NumberFormatException e) {
                        Predicate searchPredicate = cb.or(
                            cb.like(cb.upper(root.get("ter").get("TERNOM")), "%" + trimmed.toUpperCase() + "%"),
                            cb.like(cb.upper(root.get("FACDOC")), "%" + trimmed.toUpperCase() + "%")
                        );
                        predicate = cb.and(predicate, searchPredicate);
                    }
                } else if (onlyDigits && length > 5) {
                    predicate = cb.and(predicate, 
                        cb.like(root.get("ter").get("TERNIF"), "%" + trimmed + "%"));
                } else if (hasLetters && length > 5) {
                    Predicate searchPredicate = cb.or(
                        cb.like(cb.upper(root.get("ter").get("TERNIF")), "%" + trimmed.toUpperCase() + "%"),
                        cb.like(cb.upper(root.get("ter").get("TERNOM")), "%" + trimmed.toUpperCase() + "%"),
                        cb.like(cb.upper(root.get("FACDOC")), "%" + trimmed.toUpperCase() + "%")
                    );
                    predicate = cb.and(predicate, searchPredicate);
                } else {
                    Predicate searchPredicate = cb.or(
                        cb.like(cb.upper(root.get("ter").get("TERNOM")), "%" + trimmed.toUpperCase() + "%"),
                        cb.like(cb.upper(root.get("FACDOC")), "%" + trimmed.toUpperCase() + "%")
                    );
                    predicate = cb.and(predicate, searchPredicate);
                }
            }

            return predicate;
        };
    }
}
