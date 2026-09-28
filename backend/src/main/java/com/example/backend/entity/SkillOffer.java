package com.example.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * A skill that a member offers to teach, e.g. "Java Programming, 10 hours".
 * Maps to the "skill_offers" table.
 */
@Entity
@Table(name = "skill_offers")
public class SkillOffer {

    public static final String STATUS_ACTIVE = "ACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many skill offers can belong to one member.
    // Creates a "provider_id" foreign-key column pointing to members.id.
    @NotNull
    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private Member provider;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String skillName;

    @Column(length = 1000)
    private String description;

    // Must be > 0 when created (checked by the DTO in Phase 5).
    // Can drop to exactly 0 after sessions, but never below (Business Rule 15).
    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Double availableHours;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String status = STATUS_ACTIVE;

    public SkillOffer() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Member getProvider() {
        return provider;
    }

    public void setProvider(Member provider) {
        this.provider = provider;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
