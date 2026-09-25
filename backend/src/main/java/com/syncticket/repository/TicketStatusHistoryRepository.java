package com.syncticket.repository;

import com.syncticket.model.TicketStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketStatusHistoryRepository extends JpaRepository<TicketStatusHistory, Long> {

    List<TicketStatusHistory> findByTicketIdOrderByChangedAtAsc(Long ticketId);
}
