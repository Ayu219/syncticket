package com.syncticket.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.syncticket.model.TicketPriority;
import org.junit.jupiter.api.Test;

class CreateTicketRequestTest {

    @Test
    void shouldTrimFieldsAndDropBlankAssignee() {
        CreateTicketRequest request =
                new CreateTicketRequest("  My ticket  ", "  Body text  ", TicketPriority.HIGH, "   ");

        assertThat(request.title()).isEqualTo("My ticket");
        assertThat(request.description()).isEqualTo("Body text");
        assertThat(request.assignee()).isNull();
    }
}
