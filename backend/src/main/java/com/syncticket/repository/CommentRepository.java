package com.syncticket.repository;

import com.syncticket.model.Comment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    long countByTicketId(Long ticketId);

    @Query(
            """
            SELECT c.ticket.id, COUNT(c)
            FROM Comment c
            WHERE c.ticket.id IN :ticketIds
            GROUP BY c.ticket.id
            """)
    List<Object[]> countGroupedByTicketId(@Param("ticketIds") Collection<Long> ticketIds);

    @EntityGraph(attributePaths = "ticket")
    List<Comment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    @EntityGraph(attributePaths = "ticket")
    Optional<Comment> findByIdAndTicketId(Long id, Long ticketId);
}
