package com.syncticket.model;

import com.syncticket.exception.InvalidStatusTransitionException;
import com.syncticket.exception.TicketNotEditableException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.OPEN;

    @Column(length = 100)
    private String assignee;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Ticket() {
    }

    public static Ticket create(String title, String description, TicketPriority priority, String assignee) {
        Ticket ticket = new Ticket();
        ticket.title = title;
        ticket.description = description;
        ticket.priority = priority;
        ticket.assignee = assignee;
        ticket.status = TicketStatus.OPEN;
        return ticket;
    }

    public void transitionTo(TicketStatus target) {
        ensureEditable();
        if (!status.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(status, target, status.allowedTransitions());
        }
        this.status = target;
    }

    public void ensureEditable() {
        if (status.isTerminal()) {
            throw new TicketNotEditableException(id, status);
        }
    }

    public void updateTitle(String title) {
        ensureEditable();
        this.title = title;
    }

    public void updateDescription(String description) {
        ensureEditable();
        this.description = description;
    }

    public void updatePriority(TicketPriority priority) {
        ensureEditable();
        this.priority = priority;
    }

    public void updateAssignee(String assignee) {
        ensureEditable();
        this.assignee = assignee;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public String getAssignee() {
        return assignee;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
