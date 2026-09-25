package com.syncticket.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.syncticket.dto.TicketResponse;
import com.syncticket.model.Ticket;
import com.syncticket.model.TicketPriority;
import com.syncticket.model.TicketStatus;
import com.syncticket.model.TicketStatusHistory;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TicketMapperTest {

    private final TicketMapper mapper = new TicketMapper();

    @Test
    void toResponseShouldIncludeSortedAllowedTransitions() {
        Ticket ticket = Ticket.create("Sample ticket", "Body", TicketPriority.LOW, "bob");
        stamp(ticket, 7L, 2L);

        TicketResponse response = mapper.toResponse(ticket, 4);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.commentCount()).isEqualTo(4);
        assertThat(response.allowedTransitions()).containsExactly("CANCELLED", "IN_PROGRESS");
    }

    @Test
    void toHistoryRowShouldMapFields() {
        Ticket ticket = Ticket.create("Sample ticket", "Body", TicketPriority.MEDIUM, null);
        stamp(ticket, 1L, 0L);
        TicketStatusHistory history =
                TicketStatusHistory.record(ticket, TicketStatus.OPEN, TicketStatus.IN_PROGRESS, null);

        var row = mapper.toHistoryRow(history);

        assertThat(row.fromStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(row.toStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(row.note()).isNull();
        assertThat(row.changedAt()).isNotNull();
    }

    private static void stamp(Ticket ticket, long id, long version) {
        Instant now = Instant.parse("2026-01-15T10:00:00Z");
        ReflectionTestUtils.setField(ticket, "id", id);
        ReflectionTestUtils.setField(ticket, "version", version);
        ReflectionTestUtils.setField(ticket, "createdAt", now);
        ReflectionTestUtils.setField(ticket, "updatedAt", now);
    }
}
