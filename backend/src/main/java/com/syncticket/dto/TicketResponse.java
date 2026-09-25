package com.syncticket.dto;

import com.syncticket.model.TicketPriority;
import com.syncticket.model.TicketStatus;
import java.time.Instant;
import java.util.List;

public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketPriority priority,
        TicketStatus status,
        String assignee,
        List<String> allowedTransitions,
        long commentCount,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
