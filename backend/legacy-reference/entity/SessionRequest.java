package com.skillswap.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_requests")
public class SessionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private Member requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Member provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_offer_id", nullable = false)
    private SkillOffer skillOffer;

    @Positive
    @Column(nullable = false)
    private Integer requestedHours;

    private Integer actualHoursDelivered;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.PENDING;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    public SessionRequest() {
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
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

    public Integer getRequestedHours() {
        return requestedHours;
    }

    public void setRequestedHours(Integer requestedHours) {
        this.requestedHours = requestedHours;
    }

    public Integer getActualHoursDelivered() {
        return actualHoursDelivered;
    }

    public void setActualHoursDelivered(Integer actualHoursDelivered) {
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