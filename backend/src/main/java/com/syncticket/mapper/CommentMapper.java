package com.syncticket.mapper;

import com.syncticket.dto.CommentResponse;
import com.syncticket.model.Comment;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                comment.getAuthor(),
                comment.getBody(),
                comment.getCreatedAt());
    }
}
