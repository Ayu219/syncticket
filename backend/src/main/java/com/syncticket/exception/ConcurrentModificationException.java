package com.syncticket.exception;

public class ConcurrentModificationException extends RuntimeException {

    public ConcurrentModificationException() {
        super("This ticket was changed by someone else. Reload to see the latest version.");
    }
}
