package com.syncticket.mapper;

import com.syncticket.dto.StatusHistoryResponse;
import com.syncticket.dto.TicketResponse;
import com.syncticket.dto.TicketSummaryResponse;
import com.syncticket.model.Ticket;
import com.syncticket.model.TicketStatusHistory;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    public TicketResponse toResponse(Ticket ticket, long commentCount) {
        List<String> allowed = ticket.getStatus().allowedTransitions().stream()
                .map(Enum::name)
                .sorted()
                .toList();
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssignee(),
                allowed,
                commentCount,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getVersion());
    }

    public TicketSummaryResponse toSummary(Ticket ticket, long commentCount) {
        return new TicketSummaryResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssignee(),
                commentCount,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getVersion());
    }

    public StatusHistoryResponse toHistoryRow(TicketStatusHistory history) {
        return new StatusHistoryResponse(
                history.getFromStatus(), history.getToStatus(), history.getChangedAt(), history.getNote());
    }
}
