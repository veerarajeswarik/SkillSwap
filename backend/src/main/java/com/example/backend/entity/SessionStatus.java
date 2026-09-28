package com.example.backend.entity;

/**
 * The lifecycle of a session request.
 *
 * PENDING   -> created by the requester, waiting for the provider
 * CONFIRMED -> provider confirmed the session happened; credits were transferred
 * REJECTED  -> provider declined the request; no credits move
 * CANCELLED -> requester withdrew the request; no credits move
 */
public enum SessionStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELLED
}
