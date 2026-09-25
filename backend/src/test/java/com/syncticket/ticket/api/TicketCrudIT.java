package com.syncticket.ticket.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketCrudIT extends AbstractPostgresIT {

    @Test
    void shouldReturnNotFoundForMissingTicket() throws Exception {
        mockMvc.perform(get("/api/tickets/{id}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }

    @Test
    void shouldRejectStaleVersionOnUpdate() throws Exception {
        long id = createTicket();

        mockMvc.perform(patch("/api/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"First edit title\",\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(patch("/api/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Stale edit title\",\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
    }

    @Test
    void shouldFilterListByStatusAndKeyword() throws Exception {
        String token = "filter-" + System.nanoTime();
        long openId = createTicket("Ticket " + token + " open");
        long inProgressId = createTicket("Ticket " + token + " progress");
        transitionOk(inProgressId, 0, "IN_PROGRESS", null);

        mockMvc.perform(get("/api/tickets").param("q", token).param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(openId))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));
    }

    @Test
    void shouldExposeAccurateCommentCountsOnListAndGet() throws Exception {
        String token = "counts-" + System.nanoTime();
        long withTwoComments = createTicket("Ticket " + token + " alpha");
        long withOneComment = createTicket("Ticket " + token + " beta");
        addComment(withTwoComments, "Alice", "One");
        addComment(withTwoComments, "Alice", "Two");
        addComment(withOneComment, "Bob", "Only");

        JsonNode list = objectMapper.readTree(mockMvc.perform(get("/api/tickets").param("q", token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        assertThat(list.get("content")).hasSize(2);
        assertThat(commentCountFor(list.get("content"), withTwoComments)).isEqualTo(2);
        assertThat(commentCountFor(list.get("content"), withOneComment)).isEqualTo(1);

        mockMvc.perform(get("/api/tickets/{id}", withTwoComments))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentCount").value(2));
        mockMvc.perform(get("/api/tickets/{id}", withOneComment))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentCount").value(1));
    }

    private static long commentCountFor(JsonNode content, long ticketId) {
        for (JsonNode row : content) {
            if (row.get("id").asLong() == ticketId) {
                return row.get("commentCount").asLong();
            }
        }
        throw new AssertionError("Ticket " + ticketId + " not found in list content");
    }
}
