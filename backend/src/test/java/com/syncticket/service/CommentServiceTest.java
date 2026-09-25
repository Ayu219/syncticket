package com.syncticket.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.syncticket.dto.UpdateCommentRequest;
import com.syncticket.exception.CommentNotFoundException;
import com.syncticket.mapper.CommentMapper;
import com.syncticket.model.Ticket;
import com.syncticket.model.TicketPriority;
import com.syncticket.repository.CommentRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TicketService ticketService;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, ticketService, new CommentMapper());
    }

    @Test
    void updateShouldThrowWhenCommentNotOnTicket() {
        Ticket ticket = Ticket.create("Sample ticket", "Body", TicketPriority.MEDIUM, null);
        ReflectionTestUtils.setField(ticket, "id", 1L);
        when(ticketService.findTicket(1L)).thenReturn(ticket);
        when(commentRepository.findByIdAndTicketId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.update(1L, 10L, new UpdateCommentRequest("Nope")))
                .isInstanceOf(CommentNotFoundException.class);
    }
}
