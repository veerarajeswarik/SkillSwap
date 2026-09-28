package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * JSON body for POST /api/skills.
 * The frontend sends providerId (a number); the service turns it into a Member.
 */
public class SkillOfferRequest {

    @NotNull(message = "Provider id is required")
    private Long providerId;

    @NotBlank(message = "Skill name is required")
    @Size(max = 100, message = "Skill name must be at most 100 characters")
    private String skillName;

    // Optional, so no @NotBlank.
    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    @NotNull(message = "Available hours is required")
    @Positive(message = "Available hours must be greater than 0")
    private Double availableHours;

    public SkillOfferRequest() {
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getAvailableHours() {
        return availableHours;
    }

    public void setAvailableHours(Double availableHours) {
        this.availableHours = availableHours;
    }
}
