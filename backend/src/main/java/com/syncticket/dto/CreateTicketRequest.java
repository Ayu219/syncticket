package com.syncticket.dto;

import com.syncticket.model.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(min = 3, max = 200) String title,
        @NotBlank @Size(min = 1, max = 5000) String description,
        @NotNull TicketPriority priority,
        @Size(max = 100) String assignee) {

    public CreateTicketRequest {
        title = title != null ? title.trim() : null;
        description = description != null ? description.trim() : null;
        if (assignee != null) {
            assignee = assignee.trim();
            if (assignee.isEmpty()) {
                assignee = null;
            }
        }
    }
}
