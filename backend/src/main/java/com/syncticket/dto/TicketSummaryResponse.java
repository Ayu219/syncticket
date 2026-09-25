package com.syncticket.dto;

import com.syncticket.model.TicketPriority;
import com.syncticket.model.TicketStatus;
import java.time.Instant;

public record TicketSummaryResponse(
        Long id,
        String title,
        TicketPriority priority,
        TicketStatus status,
        String assignee,
        long commentCount,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
