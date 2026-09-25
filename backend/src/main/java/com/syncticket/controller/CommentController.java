package com.syncticket.controller;

import com.syncticket.dto.CommentResponse;
import com.syncticket.dto.CreateCommentRequest;
import com.syncticket.dto.UpdateCommentRequest;
import com.syncticket.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentResponse> list(@PathVariable Long ticketId) {
        return commentService.list(ticketId);
    }

    @PostMapping
    public ResponseEntity<CommentResponse> add(
            @PathVariable Long ticketId, @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse created = commentService.add(ticketId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{commentId}")
    public CommentResponse update(
            @PathVariable Long ticketId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        return commentService.update(ticketId, commentId, request);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long ticketId, @PathVariable Long commentId) {
        commentService.delete(ticketId, commentId);
        return ResponseEntity.noContent().build();
    }
}
