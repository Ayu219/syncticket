package com.syncticket.exception;

import com.syncticket.model.TicketStatus;
import java.util.Set;

public class InvalidStatusTransitionException extends RuntimeException {

    private final TicketStatus currentStatus;
    private final TicketStatus requestedStatus;
    private final Set<TicketStatus> allowedTransitions;

    public InvalidStatusTransitionException(
            TicketStatus currentStatus,
            TicketStatus requestedStatus,
            Set<TicketStatus> allowedTransitions) {
        super("Cannot change status from " + currentStatus + " to " + requestedStatus + ".");
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
        this.allowedTransitions = allowedTransitions;
    }

    public TicketStatus getCurrentStatus() {
        return currentStatus;
    }

    public TicketStatus getRequestedStatus() {
        return requestedStatus;
    }

    public Set<TicketStatus> getAllowedTransitions() {
        return allowedTransitions;
    }
}
