package com.syncticket.exception;

public class CommentNotFoundException extends RuntimeException {

    public CommentNotFoundException(Long ticketId, Long commentId) {
        super("Comment #" + commentId + " was not found on ticket #" + ticketId + ".");
    }
}
