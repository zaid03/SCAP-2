package com.example.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;
import com.example.backend.sqlserver2.model.Fac;
import com.example.backend.sqlserver2.model.Ter;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.*;

public class FacSpecification {
    public static Specification<Fac> searchFacturas(
        Integer ent, String eje, String cgecod, String estado, 
        String dateType, LocalDate fromDate, LocalDate toDate,
        String facannMode, String facann, String search, String searchType
    ) {
        return (root, query, cb) -> {
            Join<Fac, Ter> terJoin = root.join("ter", JoinType.INNER);
            
            Predicate predicate = cb.conjunction();
            
            predicate = cb.and(predicate, cb.equal(root.get("ENT"), ent));
            predicate = cb.and(predicate, cb.equal(root.get("EJE"), eje));
            predicate = cb.and(predicate, cb.equal(root.get("CGECOD"), cgecod));
            
            if (fromDate != null || toDate != null) {
                Expression<LocalDateTime> dateField;
                switch (dateType) {
                    case "FACTURA" -> dateField = root.get("FACDAT");
                    case "CONTABLE" -> dateField = root.get("FACFCO");
                    default -> dateField = root.get("FACFRE");
                }
                
                if (fromDate != null) {
                    predicate = cb.and(predicate, 
                        cb.greaterThanOrEqualTo(dateField, fromDate.atStartOfDay()));
                }
                if (toDate != null) {
                    predicate = cb.and(predicate, 
                        cb.lessThanOrEqualTo(dateField, toDate.atTime(23, 59, 59)));
                }
            }
            
            if (!"TODAS".equals(estado)) {
                Predicate estadoPredicate = switch (estado) {
                    case "CONT" -> cb.isNotNull(root.get("FACADO"));
                    case "NO_CONT" -> cb.isNull(root.get("FACADO"));
                    case "PTE_APL" -> cb.and(
                        cb.isNull(root.get("FACADO")),
                        cb.equal(
                            cb.function("ROUND", Double.class, root.get("FACIMP"), cb.literal(2)),
                            cb.function("ROUND", Double.class,
                                cb.sum(
                                    cb.coalesce(root.get("FACIEC"), 0.0),
                                    cb.coalesce(root.get("FACIDI"), 0.0)
                                ),
                                cb.literal(2)
                            )
                        )
                    );
                    case "PTE_SIN" -> cb.and(
                        cb.isNull(root.get("FACADO")),
                        cb.notEqual(
                            cb.function("ROUND", Double.class, root.get("FACIMP"), cb.literal(2)),
                            cb.function("ROUND", Double.class,
                                cb.sum(
                                    cb.coalesce(root.get("FACIEC"), 0.0),
                                    cb.coalesce(root.get("FACIDI"), 0.0)
                                ),
                                cb.literal(2)
                            )
                        )
                    );
                    default -> cb.conjunction();
                };
                predicate = cb.and(predicate, estadoPredicate);
            }
            
            if (!"ANY".equals(facannMode)) {
                Predicate facannPredicate = switch (facannMode) {
                    case "NULL" -> cb.isNull(root.get("FACANN"));
                    case "NOT_NULL" -> cb.isNotNull(root.get("FACANN"));
                    case "VALUE" -> cb.equal(root.get("FACANN"), Integer.parseInt(facann));
                    default -> cb.conjunction();
                };
                predicate = cb.and(predicate, facannPredicate);
            }
            
            if (search != null && !search.isEmpty()) {
                String searchUpper = search.toUpperCase();
                Predicate searchPredicate = switch (searchType) {
                    case "TERCOD" -> {
                        try {
                            int tercod = Integer.parseInt(search);
                            yield cb.equal(root.get("TERCOD"), tercod);
                        } catch (NumberFormatException e) {
                            yield cb.disjunction(); // No match
                        }
                    }
                    case "NIF" -> cb.like(
                        cb.upper(cb.coalesce(terJoin.get("TERNIF"), "")),
                        "%" + searchUpper + "%"
                    );
                    case "NIF_LETTERS" -> cb.or(
                        cb.like(cb.upper(cb.coalesce(terJoin.get("TERNIF"), "")), "%" + searchUpper + "%"),
                        cb.like(cb.upper(cb.coalesce(terJoin.get("TERNOM"), "")), "%" + searchUpper + "%"),
                        cb.like(cb.upper(cb.coalesce(root.get("FACDOC"), "")), "%" + searchUpper + "%")
                    );
                    case "OTROS" -> cb.or(
                        cb.like(cb.upper(cb.coalesce(terJoin.get("TERNOM"), "")), "%" + searchUpper + "%"),
                        cb.like(cb.upper(cb.coalesce(root.get("FACDOC"), "")), "%" + searchUpper + "%")
                    );
                    default -> cb.conjunction();
                };
                predicate = cb.and(predicate, searchPredicate);
            }
            
            return predicate;
        };
    }
}