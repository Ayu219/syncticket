package com.syncticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.syncticket.dto.CreateTicketRequest;
import com.syncticket.dto.TransitionRequest;
import com.syncticket.dto.UpdateTicketRequest;
import com.syncticket.exception.ConcurrentModificationException;
import com.syncticket.exception.TicketNotFoundException;
import com.syncticket.exception.ValidationFailedException;
import com.syncticket.mapper.TicketMapper;
import com.syncticket.model.Ticket;
import com.syncticket.model.TicketPriority;
import com.syncticket.model.TicketStatus;
import com.syncticket.model.TicketStatusHistory;
import com.syncticket.repository.CommentRepository;
import com.syncticket.repository.TicketRepository;
import com.syncticket.repository.TicketStatusHistoryRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketStatusHistoryRepository historyRepository;

    @Mock
    private CommentRepository commentRepository;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(
                ticketRepository, historyRepository, commentRepository, new TicketMapper());
    }

    @Test
    void createShouldPersistTicketAndInitialHistory() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        ticketService.create(new CreateTicketRequest("New ticket", "Body", TicketPriority.HIGH, "alice"));

        ArgumentCaptor<TicketStatusHistory> historyCaptor = ArgumentCaptor.forClass(TicketStatusHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getToStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(historyCaptor.getValue().getFromStatus()).isNull();
    }

    @Test
    void getByIdShouldThrowWhenMissing() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getById(99L)).isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void updateShouldRejectStatusField() {
        UpdateTicketRequest request = new UpdateTicketRequest(null, null, null, null, 0L, "CLOSED");

        assertThatThrownBy(() -> ticketService.update(1L, request))
                .isInstanceOf(ValidationFailedException.class)
                .satisfies(ex -> assertThat(((ValidationFailedException) ex).getErrors().get(0).field())
                        .isEqualTo("status"));
    }

    @Test
    void updateShouldRejectStaleVersion() {
        Ticket ticket = persistedOpenTicket(1L, 2L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        UpdateTicketRequest request = new UpdateTicketRequest("Updated title", null, null, null, 1L, null);

        assertThatThrownBy(() -> ticketService.update(1L, request))
                .isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void transitionShouldRequireResolutionNote() {
        TransitionRequest request = new TransitionRequest(TicketStatus.RESOLVED, 0L, null);

        assertThatThrownBy(() -> ticketService.transition(1L, request))
                .isInstanceOf(ValidationFailedException.class)
                .satisfies(ex -> assertThat(((ValidationFailedException) ex).getErrors().get(0).field())
                        .isEqualTo("note"));
    }

    @Test
    void transitionShouldRequireNoteWhenCancelling() {
        TransitionRequest request = new TransitionRequest(TicketStatus.CANCELLED, 0L, "  ");

        assertThatThrownBy(() -> ticketService.transition(1L, request))
                .isInstanceOf(ValidationFailedException.class)
                .satisfies(ex -> assertThat(((ValidationFailedException) ex).getErrors().get(0).field())
                        .isEqualTo("note"));
    }

    @Test
    void updateShouldRejectTitleShorterThanThreeCharacters() {
        Ticket ticket = persistedOpenTicket(1L, 0L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        UpdateTicketRequest request = new UpdateTicketRequest("ab", null, null, null, 0L, null);

        assertThatThrownBy(() -> ticketService.update(1L, request))
                .isInstanceOf(ValidationFailedException.class)
                .satisfies(ex -> assertThat(((ValidationFailedException) ex).getErrors().get(0).field())
                        .isEqualTo("title"));
    }

    @Test
    void listShouldRejectUnknownSortField() {
        assertThatThrownBy(() -> ticketService.list(null, null, 0, 20, "unknown,asc"))
                .isInstanceOf(ValidationFailedException.class)
                .satisfies(ex -> assertThat(((ValidationFailedException) ex).getErrors().get(0).field())
                        .isEqualTo("sort"));
    }

    @Test
    void transitionShouldPersistStatusChange() {
        Ticket ticket = persistedOpenTicket(5L, 0L);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(commentRepository.countByTicketId(5L)).thenReturn(0L);

        ticketService.transition(5L, new TransitionRequest(TicketStatus.IN_PROGRESS, 0L, null));

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        verify(historyRepository).save(any(TicketStatusHistory.class));
    }

    private static Ticket persistedOpenTicket(long id, long version) {
        Ticket ticket = Ticket.create("Sample ticket", "Body", TicketPriority.MEDIUM, null);
        ReflectionTestUtils.setField(ticket, "id", id);
        ReflectionTestUtils.setField(ticket, "version", version);
        return ticket;
    }
}
