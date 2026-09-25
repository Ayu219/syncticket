package com.syncticket.service;

import com.syncticket.dto.CommentResponse;
import com.syncticket.dto.CreateCommentRequest;
import com.syncticket.dto.UpdateCommentRequest;
import com.syncticket.exception.CommentNotFoundException;
import com.syncticket.mapper.CommentMapper;
import com.syncticket.model.Comment;
import com.syncticket.model.Ticket;
import com.syncticket.repository.CommentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketService ticketService;
    private final CommentMapper commentMapper;

    public CommentService(
            CommentRepository commentRepository, TicketService ticketService, CommentMapper commentMapper) {
        this.commentRepository = commentRepository;
        this.ticketService = ticketService;
        this.commentMapper = commentMapper;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long ticketId) {
        ticketService.findTicket(ticketId);
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(commentMapper::toResponse)
                .toList();
    }

    @Transactional
    public CommentResponse add(Long ticketId, CreateCommentRequest request) {
        Ticket ticket = ticketService.findTicket(ticketId);
        ticket.ensureEditable();
        Comment comment = Comment.create(ticket, request.author(), request.body());
        Comment saved = commentRepository.save(comment);
        return commentMapper.toResponse(saved);
    }

    @Transactional
    public CommentResponse update(Long ticketId, Long commentId, UpdateCommentRequest request) {
        Ticket ticket = ticketService.findTicket(ticketId);
        ticket.ensureEditable();
        Comment comment = commentRepository
                .findByIdAndTicketId(commentId, ticketId)
                .orElseThrow(() -> new CommentNotFoundException(ticketId, commentId));
        comment.updateBody(request.body());
        return commentMapper.toResponse(comment);
    }

    @Transactional
    public void delete(Long ticketId, Long commentId) {
        Ticket ticket = ticketService.findTicket(ticketId);
        ticket.ensureEditable();
        Comment comment = commentRepository
                .findByIdAndTicketId(commentId, ticketId)
                .orElseThrow(() -> new CommentNotFoundException(ticketId, commentId));
        commentRepository.delete(comment);
    }
}
