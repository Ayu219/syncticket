package com.syncticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank @Size(min = 1, max = 100) String author,
        @NotBlank @Size(min = 1, max = 2000) String body) {

    public CreateCommentRequest {
        author = author != null ? author.trim() : null;
        body = body != null ? body.trim() : null;
    }
}
