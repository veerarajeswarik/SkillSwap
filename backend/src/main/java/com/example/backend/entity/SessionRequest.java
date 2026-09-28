package com.example.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

/**
 * One member asking another member for a learning session.
 * Maps to the "session_requests" table.
 */
@Entity
@Table(name = "session_requests")
public class SessionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The learner: the member who pays credits.
    @NotNull
    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private Member requester;

    // The teacher: the member who earns credits.
    @NotNull
    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private Member provider;

    // Which skill offer this session is for.
    @NotNull
    @ManyToOne
    @JoinColumn(name = "skill_offer_id", nullable = false)
    private SkillOffer skillOffer;

    // What the requester asked for; credits are checked against this at request time.
    @NotNull
    @Positive
    @Column(nullable = false)
    private Double requestedHours;

    // What the provider actually delivered; stays null until confirmation.
    // Credits are transferred based on THIS value.
    @Positive
    private Double actualHoursDelivered;

    @Column(length = 500)
    private String message;

    // EnumType.STRING stores the name ("PENDING") instead of the position (0).
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.PENDING;

    // updatable = false: once set, Hibernate never changes it in an UPDATE.
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    public SessionRequest() {
    }

    // Runs automatically just before Hibernate INSERTs a new row.
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = SessionStatus.PENDING;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Member getRequester() {
        return requester;
    }

    public void setRequester(Member requester) {
        this.requester = requester;
    }

    public Member getProvider() {
        return provider;
    }

    public void setProvider(Member provider) {
        this.provider = provider;
    }

    public SkillOffer getSkillOffer() {
        return skillOffer;
    }

    public void setSkillOffer(SkillOffer skillOffer) {
        this.skillOffer = skillOffer;
    }

    public Double getRequestedHours() {
        return requestedHours;
    }

    public void setRequestedHours(Double requestedHours) {
        this.requestedHours = requestedHours;
    }

    public Double getActualHoursDelivered() {
        return actualHoursDelivered;
    }

    public void setActualHoursDelivered(Double actualHoursDelivered) {
        this.actualHoursDelivered = actualHoursDelivered;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
