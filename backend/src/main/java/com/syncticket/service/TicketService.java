package com.syncticket.service;

import com.syncticket.dto.CreateTicketRequest;
import com.syncticket.dto.PageResponse;
import com.syncticket.dto.StatusHistoryResponse;
import com.syncticket.dto.TicketResponse;
import com.syncticket.dto.TicketSummaryResponse;
import com.syncticket.dto.TransitionRequest;
import com.syncticket.dto.UpdateTicketRequest;
import com.syncticket.exception.ConcurrentModificationException;
import com.syncticket.exception.FieldErrorDetail;
import com.syncticket.exception.TicketNotFoundException;
import com.syncticket.exception.ValidationFailedException;
import com.syncticket.mapper.TicketMapper;
import com.syncticket.model.Ticket;
import com.syncticket.model.TicketStatus;
import com.syncticket.model.TicketStatusHistory;
import com.syncticket.repository.CommentRepository;
import com.syncticket.repository.TicketRepository;
import com.syncticket.repository.TicketSpecifications;
import com.syncticket.repository.TicketStatusHistoryRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("createdAt", "updatedAt", "priority", "title");

    private final TicketRepository ticketRepository;
    private final TicketStatusHistoryRepository historyRepository;
    private final CommentRepository commentRepository;
    private final TicketMapper ticketMapper;

    public TicketService(
            TicketRepository ticketRepository,
            TicketStatusHistoryRepository historyRepository,
            CommentRepository commentRepository,
            TicketMapper ticketMapper) {
        this.ticketRepository = ticketRepository;
        this.historyRepository = historyRepository;
        this.commentRepository = commentRepository;
        this.ticketMapper = ticketMapper;
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest request) {
        Ticket ticket = Ticket.create(
                request.title(), request.description(), request.priority(), request.assignee());
        Ticket saved = ticketRepository.save(ticket);
        historyRepository.save(TicketStatusHistory.record(saved, null, TicketStatus.OPEN, null));
        return ticketMapper.toResponse(saved, 0);
    }

    @Transactional(readOnly = true)
    public TicketResponse getById(Long id) {
        Ticket ticket = findTicket(id);
        return ticketMapper.toResponse(ticket, commentRepository.countByTicketId(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketSummaryResponse> list(
            String keyword, TicketStatus status, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Specification<Ticket> spec = TicketSpecifications.withFilters(keyword, status);
        Page<Ticket> result = ticketRepository.findAll(spec, pageable);
        Map<Long, Long> commentCountsByTicketId = commentCountsForTickets(result.getContent());
        List<TicketSummaryResponse> content = result.getContent().stream()
                .map(t -> ticketMapper.toSummary(t, commentCountsByTicketId.getOrDefault(t.getId(), 0L)))
                .toList();
        return new PageResponse<>(
                content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public TicketResponse update(Long id, UpdateTicketRequest request) {
        if (request.status() != null) {
            throw new ValidationFailedException(
                    "One or more fields are invalid.",
                    List.of(new FieldErrorDetail(
                            "status", "Use the transitions endpoint to change status.")));
        }
        Ticket ticket = findTicket(id);
        assertVersion(ticket, request.version());
        if (request.title() != null) {
            if (request.title().length() < 3 || request.title().length() > 200) {
                throw fieldError("title", "Title must be between 3 and 200 characters.");
            }
            ticket.updateTitle(request.title());
        }
        if (request.description() != null) {
            if (request.description().length() < 1 || request.description().length() > 5000) {
                throw fieldError("description", "Description must be between 1 and 5000 characters.");
            }
            ticket.updateDescription(request.description());
        }
        if (request.priority() != null) {
            ticket.updatePriority(request.priority());
        }
        if (request.assignee() != null) {
            String assignee = request.assignee().isEmpty() ? null : request.assignee();
            if (assignee != null && assignee.length() > 100) {
                throw fieldError("assignee", "Assignee must be at most 100 characters.");
            }
            ticket.updateAssignee(assignee);
        }
        Ticket saved = ticketRepository.save(ticket);
        return ticketMapper.toResponse(saved, commentRepository.countByTicketId(id));
    }

    @Transactional
    public TicketResponse transition(Long id, TransitionRequest request) {
        validateTransitionNote(request);
        Ticket ticket = findTicket(id);
        assertVersion(ticket, request.version());
        TicketStatus from = ticket.getStatus();
        TicketStatus to = request.targetStatus();
        ticket.transitionTo(to);
        String note = to.requiresResolutionNote() ? request.note() : null;
        Ticket saved = ticketRepository.save(ticket);
        historyRepository.save(TicketStatusHistory.record(saved, from, to, note));
        return ticketMapper.toResponse(saved, commentRepository.countByTicketId(id));
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> history(Long id) {
        findTicket(id);
        return historyRepository.findByTicketIdOrderByChangedAtAsc(id).stream()
                .map(ticketMapper::toHistoryRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public Ticket findTicket(Long id) {
        return ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    private void validateTransitionNote(TransitionRequest request) {
        if (!request.targetStatus().requiresResolutionNote()) {
            return;
        }
        if (request.note() == null || request.note().isBlank()) {
            throw new ValidationFailedException(
                    "One or more fields are invalid.",
                    List.of(new FieldErrorDetail(
                            "note", "Resolution note is required when moving to "
                                    + request.targetStatus() + ".")));
        }
        if (request.note().length() > 2000) {
            throw fieldError("note", "Resolution note must be at most 2000 characters.");
        }
    }

    private void assertVersion(Ticket ticket, long expectedVersion) {
        if (ticket.getVersion() != expectedVersion) {
            throw new ConcurrentModificationException();
        }
    }

    private ValidationFailedException fieldError(String field, String message) {
        return new ValidationFailedException(
                "One or more fields are invalid.", List.of(new FieldErrorDetail(field, message)));
    }

    private Map<Long, Long> commentCountsForTickets(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            return Map.of();
        }
        List<Long> ticketIds = tickets.stream().map(Ticket::getId).toList();
        return commentRepository.countGroupedByTicketId(ticketIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sortParam.split(",", 2);
        String field = parts[0].trim();
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new ValidationFailedException(
                    "One or more fields are invalid.",
                    List.of(new FieldErrorDetail("sort", "Invalid sort field '" + field + "'.")));
        }
        Sort.Direction direction =
                parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }
}
