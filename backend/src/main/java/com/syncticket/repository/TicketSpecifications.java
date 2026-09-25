package com.syncticket.repository;

import com.syncticket.model.Ticket;
import com.syncticket.model.TicketStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> withFilters(String keyword, TicketStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + escapeLike(keyword.toLowerCase()) + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern, '\\');
                Predicate descriptionMatch = cb.like(cb.lower(root.get("description")), pattern, '\\');
                predicates.add(cb.or(titleMatch, descriptionMatch));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    static String escapeLike(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
