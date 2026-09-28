package com.skillswap.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "credit_ledger")
public class CreditLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_request_id")
    private SessionRequest sessionRequest;

    @Enumerated(EnumType.STRING)
    private LedgerType transactionType;

    private Integer hours;

    private String description;

    private LocalDateTime createdAt;

    public CreditLedger() {
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public SessionRequest getSessionRequest() {
        return sessionRequest;
    }

    public void setSessionRequest(SessionRequest sessionRequest) {
        this.sessionRequest = sessionRequest;
    }

    public LedgerType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(LedgerType transactionType) {
        this.transactionType = transactionType;
    }

    public Integer getHours() {
        return hours;
    }

    public void setHours(Integer hours) {
        this.hours = hours;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}