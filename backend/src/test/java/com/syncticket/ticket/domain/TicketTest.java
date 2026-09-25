package com.syncticket.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.syncticket.exception.InvalidStatusTransitionException;
import com.syncticket.exception.TicketNotEditableException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TicketTest {

    @Test
    void shouldApplyValidTransition() {
        Ticket ticket = openTicket();

        ticket.transitionTo(TicketStatus.IN_PROGRESS);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void shouldRejectInvalidTransition() {
        Ticket ticket = openTicket();

        assertThatThrownBy(() -> ticket.transitionTo(TicketStatus.CLOSED))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("OPEN")
                .hasMessageContaining("CLOSED");
    }

    @Test
    void shouldRejectEditsWhenTerminal() {
        Ticket ticket = openTicket();
        ticket.transitionTo(TicketStatus.CANCELLED);
        ReflectionTestUtils.setField(ticket, "id", 42L);

        assertThatThrownBy(() -> ticket.updateTitle("New title"))
                .isInstanceOf(TicketNotEditableException.class)
                .hasMessageContaining("42")
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void shouldRejectTransitionFromTerminalStatus() {
        Ticket ticket = openTicket();
        ticket.transitionTo(TicketStatus.CANCELLED);

        assertThatThrownBy(() -> ticket.transitionTo(TicketStatus.OPEN))
                .isInstanceOf(TicketNotEditableException.class);
    }

    private static Ticket openTicket() {
        return Ticket.create("Sample ticket", "Description", TicketPriority.MEDIUM, null);
    }
}
