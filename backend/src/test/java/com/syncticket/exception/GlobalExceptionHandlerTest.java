package com.syncticket.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.syncticket.model.TicketStatus;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldMapTicketNotFoundToProblemDetail() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tickets/42");
        request.setRequestURI("/api/tickets/42");

        ResponseEntity<ProblemDetail> response =
                handler.handleNotFound(new TicketNotFoundException(42L), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProperties().get("code")).isEqualTo(ErrorCode.TICKET_NOT_FOUND);
        assertThat(response.getBody().getDetail()).contains("42");
    }

    @Test
    void shouldEnrichInvalidTransitionResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tickets/1/transitions");
        request.setRequestURI("/api/tickets/1/transitions");
        InvalidStatusTransitionException ex = new InvalidStatusTransitionException(
                TicketStatus.CLOSED, TicketStatus.OPEN, Set.of());

        ResponseEntity<ProblemDetail> response = handler.handleInvalidTransition(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getProperties().get("code")).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        assertThat(response.getBody().getProperties().get("currentStatus")).isEqualTo("CLOSED");
        assertThat(response.getBody().getProperties().get("requestedStatus")).isEqualTo("OPEN");
    }

    @Test
    void shouldMapConcurrentModificationToConflict() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/tickets/1");
        request.setRequestURI("/api/tickets/1");

        ResponseEntity<ProblemDetail> response =
                handler.handleOptimisticLock(new ConcurrentModificationException(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getProperties().get("code")).isEqualTo(ErrorCode.CONCURRENT_MODIFICATION);
    }
}
