package com.syncticket.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.syncticket.model.TicketStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class TicketStatusTest {

    @Test
    void shouldHaveFiveStatuses() {
        assertThat(TicketStatus.values()).hasSize(5);
    }

    @ParameterizedTest(name = "{0} -> {1} allowed={2}")
    @CsvSource({
        "OPEN,IN_PROGRESS,true", "OPEN,CANCELLED,true",
        "IN_PROGRESS,RESOLVED,true", "IN_PROGRESS,CANCELLED,true",
        "RESOLVED,CLOSED,true", "RESOLVED,IN_PROGRESS,true",
        "OPEN,OPEN,false", "OPEN,RESOLVED,false", "OPEN,CLOSED,false",
        "IN_PROGRESS,OPEN,false", "IN_PROGRESS,IN_PROGRESS,false", "IN_PROGRESS,CLOSED,false",
        "RESOLVED,OPEN,false", "RESOLVED,RESOLVED,false", "RESOLVED,CANCELLED,false",
        "CLOSED,OPEN,false", "CLOSED,IN_PROGRESS,false", "CLOSED,RESOLVED,false", "CLOSED,CLOSED,false", "CLOSED,CANCELLED,false",
        "CANCELLED,OPEN,false", "CANCELLED,IN_PROGRESS,false", "CANCELLED,RESOLVED,false", "CANCELLED,CLOSED,false", "CANCELLED,CANCELLED,false"
    })
    void transitionMatrix(TicketStatus from, TicketStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    @ParameterizedTest
    @CsvSource({
        "OPEN,false", "IN_PROGRESS,false", "RESOLVED,false",
        "CLOSED,true", "CANCELLED,true"
    })
    void terminalStatuses(TicketStatus status, boolean terminal) {
        assertThat(status.isTerminal()).isEqualTo(terminal);
    }

    @ParameterizedTest
    @CsvSource({
        "OPEN,false", "IN_PROGRESS,false", "CLOSED,false",
        "RESOLVED,true", "CANCELLED,true"
    })
    void resolutionNoteRequired(TicketStatus status, boolean required) {
        assertThat(status.requiresResolutionNote()).isEqualTo(required);
    }
}
