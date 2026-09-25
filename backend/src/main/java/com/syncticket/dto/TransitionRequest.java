package com.syncticket.dto;

import com.syncticket.model.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransitionRequest(
        @NotNull TicketStatus targetStatus,
        @NotNull Long version,
        @Size(max = 2000) String note) {

    public TransitionRequest {
        if (note != null) {
            note = note.trim();
        }
    }
}
