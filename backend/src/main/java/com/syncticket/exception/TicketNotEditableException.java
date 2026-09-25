package com.syncticket.exception;

import com.syncticket.model.TicketStatus;

public class TicketNotEditableException extends RuntimeException {

    private final Long ticketId;
    private final TicketStatus status;

    public TicketNotEditableException(Long ticketId, TicketStatus status) {
        super("Ticket #" + ticketId + " is " + status + " and can no longer be changed.");
        this.ticketId = ticketId;
        this.status = status;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public TicketStatus getStatus() {
        return status;
    }
}
