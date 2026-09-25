package com.syncticket.ticket.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.syncticket.model.TicketStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketStatusTransitionIT extends AbstractPostgresIT {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "OPEN, IN_PROGRESS, ",
        "OPEN, CANCELLED, Cancelled from open",
        "IN_PROGRESS, RESOLVED, Fixed issue",
        "IN_PROGRESS, CANCELLED, No longer needed",
        "RESOLVED, CLOSED, ",
        "RESOLVED, IN_PROGRESS, "
    })
    void shouldAllowValidTransition(String from, String to, String note) throws Exception {
        long id = givenTicketInStatus(TicketStatus.valueOf(from.trim()));
        long version = ticketVersion(id);
        int historyBefore = historySize(id);

        postTransition(id, version, to.trim(), note == null || note.isBlank() ? null : note.trim())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(to.trim()));

        assertThat(ticketStatus(id)).isEqualTo(TicketStatus.valueOf(to.trim()));
        assertThat(historySize(id)).isEqualTo(historyBefore + 1);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "OPEN, OPEN",
        "OPEN, RESOLVED",
        "OPEN, CLOSED",
        "IN_PROGRESS, OPEN",
        "IN_PROGRESS, IN_PROGRESS",
        "IN_PROGRESS, CLOSED",
        "RESOLVED, OPEN",
        "RESOLVED, RESOLVED",
        "RESOLVED, CANCELLED",
        "CLOSED, OPEN",
        "CLOSED, IN_PROGRESS",
        "CLOSED, RESOLVED",
        "CLOSED, CLOSED",
        "CLOSED, CANCELLED",
        "CANCELLED, OPEN",
        "CANCELLED, IN_PROGRESS",
        "CANCELLED, RESOLVED",
        "CANCELLED, CLOSED",
        "CANCELLED, CANCELLED"
    })
    void shouldRejectInvalidTransition(String from, String to) throws Exception {
        long id = givenTicketInStatus(TicketStatus.valueOf(from.trim()));
        TicketStatus fromStatus = TicketStatus.valueOf(from.trim());
        TicketStatus toStatus = TicketStatus.valueOf(to.trim());
        long version = ticketVersion(id);
        int historyBefore = historySize(id);

        postTransition(id, version, to.trim(), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"))
                .andExpect(jsonPath("$.currentStatus").value(fromStatus.name()))
                .andExpect(jsonPath("$.requestedStatus").value(toStatus.name()));

        assertThat(ticketStatus(id)).isEqualTo(fromStatus);
        assertThat(historySize(id)).isEqualTo(historyBefore);
    }

    @Test
    void shouldCompleteOpenToClosedHappyPath() throws Exception {
        long id = createTicket();
        transitionOk(id, 0, "IN_PROGRESS", null);
        transitionOk(id, 1, "RESOLVED", "Shipped fix.");
        transitionOk(id, 2, "CLOSED", null);

        assertThat(ticketStatus(id)).isEqualTo(TicketStatus.CLOSED);
        assertThat(historySize(id)).isEqualTo(4);
    }

    @Test
    void shouldReopenResolvedBeforeClosingAgain() throws Exception {
        long id = createTicket();
        transitionOk(id, 0, "IN_PROGRESS", null);
        transitionOk(id, 1, "RESOLVED", "First resolution.");
        transitionOk(id, 2, "IN_PROGRESS", null);
        transitionOk(id, 3, "RESOLVED", "Second resolution.");
        transitionOk(id, 4, "CLOSED", null);

        assertThat(ticketStatus(id)).isEqualTo(TicketStatus.CLOSED);
        assertThat(historySize(id)).isEqualTo(6);
    }

    @Test
    void shouldRequireNoteWhenResolving() throws Exception {
        long id = createTicket();
        transitionOk(id, 0, "IN_PROGRESS", null);

        mockMvc.perform(post("/api/tickets/{id}/transitions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"RESOLVED\",\"version\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("note"));
    }

    @Test
    void shouldRequireNoteWhenCancelling() throws Exception {
        long id = createTicket();

        mockMvc.perform(post("/api/tickets/{id}/transitions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"CANCELLED\",\"version\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("note"));
    }

    @Test
    void shouldRejectResolvedToOpen() throws Exception {
        long id = givenTicketInStatus(TicketStatus.RESOLVED);

        postTransition(id, ticketVersion(id), "OPEN", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void shouldRejectCancelledToOpen() throws Exception {
        long id = givenTicketInStatus(TicketStatus.CANCELLED);

        postTransition(id, ticketVersion(id), "OPEN", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void shouldRejectClosedToOpen() throws Exception {
        long id = givenTicketInStatus(TicketStatus.CLOSED);

        postTransition(id, ticketVersion(id), "OPEN", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void shouldRejectUnknownTargetStatus() throws Exception {
        long id = createTicket();

        mockMvc.perform(post("/api/tickets/{id}/transitions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"DONE\",\"version\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldRejectMissingTargetStatus() throws Exception {
        long id = createTicket();

        mockMvc.perform(post("/api/tickets/{id}/transitions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldReturnNotFoundWhenTransitioningMissingTicket() throws Exception {
        mockMvc.perform(post("/api/tickets/{id}/transitions", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"IN_PROGRESS\",\"version\":0}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }

    @Test
    void shouldRejectStatusFieldOnPatch() throws Exception {
        long id = createTicket();

        mockMvc.perform(patch("/api/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\",\"version\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("status"));

        assertThat(ticketStatus(id)).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void shouldRejectStaleVersionOnTransition() throws Exception {
        long id = createTicket();
        transitionOk(id, 0, "IN_PROGRESS", null);

        postTransition(id, 0, "RESOLVED", "Stale.")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
    }

    @Test
    void shouldRejectUpdateOnClosedTicket() throws Exception {
        long id = givenTicketInStatus(TicketStatus.CLOSED);

        mockMvc.perform(patch("/api/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New title\",\"version\":" + ticketVersion(id) + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_EDITABLE"));
    }

    @Test
    void shouldRejectUpdateOnCancelledTicket() throws Exception {
        long id = givenTicketInStatus(TicketStatus.CANCELLED);

        mockMvc.perform(patch("/api/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New title\",\"version\":" + ticketVersion(id) + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_EDITABLE"));
    }

    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    void shouldExposeAllowedTransitionsMatchingMatrix(TicketStatus status) throws Exception {
        long id = givenTicketInStatus(status);

        mockMvc.perform(get("/api/tickets/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedTransitions")
                        .value(status.allowedTransitions().stream()
                                .map(Enum::name)
                                .sorted()
                                .toList()));
    }
}
