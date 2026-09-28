package com.skillswap.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ConfirmSessionRequest {

    @NotNull(message = "Actual hours delivered are required")
    @Positive(message = "Actual hours must be greater than zero")
    private Integer actualHoursDelivered;

    public Integer getActualHoursDelivered() {
        return actualHoursDelivered;
    }

    public void setActualHoursDelivered(Integer actualHoursDelivered) {
        this.actualHoursDelivered = actualHoursDelivered;
    }
}