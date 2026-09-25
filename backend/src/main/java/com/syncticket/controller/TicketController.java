package com.syncticket.controller;

import com.syncticket.dto.CreateTicketRequest;
import com.syncticket.dto.PageResponse;
import com.syncticket.dto.StatusHistoryResponse;
import com.syncticket.dto.TicketResponse;
import com.syncticket.dto.TicketSummaryResponse;
import com.syncticket.dto.TransitionRequest;
import com.syncticket.dto.UpdateTicketRequest;
import com.syncticket.model.TicketStatus;
import com.syncticket.service.TicketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/tickets")
@Validated
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse created = ticketService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public PageResponse<TicketSummaryResponse> list(
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ticketService.list(q, status, page, size, sort);
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id) {
        return ticketService.getById(id);
    }

    @PatchMapping("/{id}")
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTicketRequest request) {
        return ticketService.update(id, request);
    }

    @PostMapping("/{id}/transitions")
    public TicketResponse transition(@PathVariable Long id, @Valid @RequestBody TransitionRequest request) {
        return ticketService.transition(id, request);
    }

    @GetMapping("/{id}/history")
    public List<StatusHistoryResponse> history(@PathVariable Long id) {
        return ticketService.history(id);
    }
}
