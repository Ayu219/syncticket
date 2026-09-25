package com.syncticket.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED;

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED = Map.of(
            OPEN, EnumSet.of(IN_PROGRESS, CANCELLED),
            IN_PROGRESS, EnumSet.of(RESOLVED, CANCELLED),
            RESOLVED, EnumSet.of(CLOSED, IN_PROGRESS),
            CLOSED, EnumSet.noneOf(TicketStatus.class),
            CANCELLED, EnumSet.noneOf(TicketStatus.class));

    public boolean canTransitionTo(TicketStatus target) {
        return ALLOWED.get(this).contains(target);
    }

    public Set<TicketStatus> allowedTransitions() {
        return Collections.unmodifiableSet(ALLOWED.get(this));
    }

    public boolean isTerminal() {
        return ALLOWED.get(this).isEmpty();
    }

    public boolean requiresResolutionNote() {
        return this == RESOLVED || this == CANCELLED;
    }
}
