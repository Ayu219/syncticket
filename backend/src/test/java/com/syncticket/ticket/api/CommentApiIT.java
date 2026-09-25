package com.syncticket.ticket.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentApiIT extends AbstractPostgresIT {

    @Test
    void shouldAddListUpdateAndDeleteComment() throws Exception {
        long ticketId = createTicket();
        long firstId = addComment(ticketId, "Alice", "First");
        long secondId = addComment(ticketId, "Bob", "Second");

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(firstId))
                .andExpect(jsonPath("$[0].body").value("First"))
                .andExpect(jsonPath("$[1].id").value(secondId))
                .andExpect(jsonPath("$[1].body").value("Second"));

        mockMvc.perform(patch("/api/tickets/{ticketId}/comments/{commentId}", ticketId, firstId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"First updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("First updated"));

        mockMvc.perform(delete("/api/tickets/{ticketId}/comments/{commentId}", ticketId, firstId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(secondId));
    }

    @Test
    void shouldRejectCommentMutationsOnClosedTicket() throws Exception {
        long ticketId = createTicket();
        long commentId = addComment(ticketId, "Alice", "Note");
        transitionOk(ticketId, 0, "IN_PROGRESS", null);
        transitionOk(ticketId, 1, "RESOLVED", "Done.");
        transitionOk(ticketId, 2, "CLOSED", null);

        mockMvc.perform(patch("/api/tickets/{ticketId}/comments/{commentId}", ticketId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Nope\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_EDITABLE"));

        mockMvc.perform(delete("/api/tickets/{ticketId}/comments/{commentId}", ticketId, commentId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_EDITABLE"));

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\":\"Bob\",\"body\":\"Nope\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_EDITABLE"));
    }

    @Test
    void shouldRejectCommentMutationsOnCancelledTicket() throws Exception {
        long ticketId = createTicket();
        long commentId = addComment(ticketId, "Alice", "Note");
        transitionOk(ticketId, 0, "CANCELLED", "No longer needed.");

        mockMvc.perform(patch("/api/tickets/{ticketId}/comments/{commentId}", ticketId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Nope\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_EDITABLE"));
    }

    @Test
    void shouldListCommentsOnClosedTicket() throws Exception {
        long ticketId = createTicket();
        addComment(ticketId, "Alice", "Visible after close");
        transitionOk(ticketId, 0, "IN_PROGRESS", null);
        transitionOk(ticketId, 1, "RESOLVED", "Done.");
        transitionOk(ticketId, 2, "CLOSED", null);

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].body").value("Visible after close"));
    }

    @Test
    void shouldReturnNotFoundWhenCommentNotOnTicket() throws Exception {
        long ticketId = createTicket();
        long otherTicketId = createTicket();
        long commentOnOther = addComment(otherTicketId, "Alice", "Elsewhere");

        mockMvc.perform(patch("/api/tickets/{ticketId}/comments/{commentId}", ticketId, commentOnOther)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Nope\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }
}
