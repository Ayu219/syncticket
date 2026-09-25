package com.syncticket.exception;

public class TicketNotFoundException extends RuntimeException {

    private final Long ticketId;

    public TicketNotFoundException(Long ticketId) {
        super("Ticket #" + ticketId + " does not exist.");
        this.ticketId = ticketId;
    }

    public Long getTicketId() {
        return ticketId;
    }
}
