package com.syncticket.dto;

import com.syncticket.model.TicketStatus;
import java.time.Instant;

public record StatusHistoryResponse(TicketStatus fromStatus, TicketStatus toStatus, Instant changedAt, String note) {
}
