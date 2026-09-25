package com.syncticket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.syncticket.model.TicketPriority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateTicketRequest(
        @Size(min = 3, max = 200) String title,
        @Size(min = 1, max = 5000) String description,
        TicketPriority priority,
        String assignee,
        @NotNull Long version,
        String status) {

    public UpdateTicketRequest {
        if (title != null) {
            title = title.trim();
        }
        if (description != null) {
            description = description.trim();
        }
        if (assignee != null) {
            assignee = assignee.trim();
        }
    }
}
