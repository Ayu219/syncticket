package com.syncticket.dto;

import java.time.Instant;

public record CommentResponse(Long id, Long ticketId, String author, String body, Instant createdAt) {
}
