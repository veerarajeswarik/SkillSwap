package com.example.backend.exception;

/**
 * Thrown when a requested record (member, skill offer, session) does not exist.
 * Phase 8 turns this into an HTTP 404 Not Found response.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
