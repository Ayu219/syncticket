package com.syncticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(@NotBlank @Size(min = 1, max = 2000) String body) {

    public UpdateCommentRequest {
        body = body != null ? body.trim() : null;
    }
}
