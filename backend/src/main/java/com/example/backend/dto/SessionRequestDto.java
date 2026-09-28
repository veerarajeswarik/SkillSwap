package com.example.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * JSON body for POST /api/sessions.
 * No providerId: the provider is taken from the skill offer, so it can't be faked.
 */
public class SessionRequestDto {

    @NotNull(message = "Requester id is required")
    private Long requesterId;

    @NotNull(message = "Skill offer id is required")
    private Long skillOfferId;

    // Business Rule 4: requested hours must be greater than zero.
    @NotNull(message = "Requested hours is required")
    @Positive(message = "Requested hours must be greater than 0")
    private Double requestedHours;

    @Size(max = 500, message = "Message must be at most 500 characters")
    private String message;

    public SessionRequestDto() {
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public Long getSkillOfferId() {
        return skillOfferId;
    }

    public void setSkillOfferId(Long skillOfferId) {
        this.skillOfferId = skillOfferId;
    }

    public Double getRequestedHours() {
        return requestedHours;
    }

    public void setRequestedHours(Double requestedHours) {
        this.requestedHours = requestedHours;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
