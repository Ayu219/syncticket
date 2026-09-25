package com.syncticket.ticket.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncticket.model.TicketStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractPostgresIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected long createTicket() throws Exception {
        return createTicket("Test ticket");
    }

    protected long createTicket(String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"title\":\"%s\",\"description\":\"Body\",\"priority\":\"MEDIUM\"}",
                                title.replace("\"", "\\\""))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    protected long addComment(long ticketId, String author, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("author", author, "body", body))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    protected void transitionOk(long id, long version, String target, String note) throws Exception {
        postTransition(id, version, target, note).andExpect(status().isOk());
    }

    protected org.springframework.test.web.servlet.ResultActions postTransition(
            long id, long version, String target, String note) throws Exception {
        String payload = note == null
                ? String.format("{\"targetStatus\":\"%s\",\"version\":%d}", target, version)
                : String.format(
                        "{\"targetStatus\":\"%s\",\"version\":%d,\"note\":%s}",
                        target,
                        version,
                        objectMapper.writeValueAsString(note));
        return mockMvc.perform(post("/api/tickets/{id}/transitions", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload));
    }

    protected long givenTicketInStatus(TicketStatus status) throws Exception {
        long id = createTicket();
        switch (status) {
            case OPEN:
                return id;
            case IN_PROGRESS:
                transitionOk(id, 0, "IN_PROGRESS", null);
                return id;
            case RESOLVED:
                transitionOk(id, 0, "IN_PROGRESS", null);
                transitionOk(id, 1, "RESOLVED", "Resolved.");
                return id;
            case CLOSED:
                transitionOk(id, 0, "IN_PROGRESS", null);
                transitionOk(id, 1, "RESOLVED", "Resolved.");
                transitionOk(id, 2, "CLOSED", null);
                return id;
            case CANCELLED:
                transitionOk(id, 0, "CANCELLED", "Cancelled.");
                return id;
            default:
                throw new IllegalArgumentException("Unsupported status: " + status);
        }
    }

    protected JsonNode fetchTicket(long id) throws Exception {
        MvcResult result =
                mockMvc.perform(get("/api/tickets/{id}", id)).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected long ticketVersion(long id) throws Exception {
        return fetchTicket(id).get("version").asLong();
    }

    protected TicketStatus ticketStatus(long id) throws Exception {
        return TicketStatus.valueOf(fetchTicket(id).get("status").asText());
    }

    protected int historySize(long id) throws Exception {
        MvcResult result =
                mockMvc.perform(get("/api/tickets/{id}/history", id)).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).size();
    }
}
