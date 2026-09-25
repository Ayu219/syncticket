package com.syncticket.exception;

import java.util.List;

public class ValidationFailedException extends RuntimeException {

    private final List<FieldErrorDetail> errors;

    public ValidationFailedException(String message, List<FieldErrorDetail> errors) {
        super(message);
        this.errors = errors;
    }

    public List<FieldErrorDetail> getErrors() {
        return errors;
    }
}
