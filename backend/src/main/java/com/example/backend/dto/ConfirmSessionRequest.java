package com.example.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * JSON body for PUT /api/sessions/{id}/confirm.
 * The session id comes from the URL; only the delivered hours come in the body.
 */
public class ConfirmSessionRequest {

    @NotNull(message = "Actual hours delivered is required")
    @Positive(message = "Actual hours delivered must be greater than 0")
    private Double actualHoursDelivered;

    public ConfirmSessionRequest() {
    }

    public Double getActualHoursDelivered() {
        return actualHoursDelivered;
    }

    public void setActualHoursDelivered(Double actualHoursDelivered) {
        this.actualHoursDelivered = actualHoursDelivered;
    }
}
