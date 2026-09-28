package com.example.backend.exception;

/**
 * Thrown when a requester does not have enough credits for the hours involved.
 * Phase 8 turns this into an HTTP 400 Bad Request response.
 */
public class InsufficientCreditException extends RuntimeException {

    public InsufficientCreditException(String message) {
        super(message);
    }
}
