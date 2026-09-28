package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * A registered user of SkillSwap.
 * Maps to the "members" table in MySQL.
 */
@Entity
@Table(name = "members")
public class Member {

    /** Every new member starts with this many credits (Business Rule 1). */
    public static final double INITIAL_CREDITS = 5.0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Email
    @Column(unique = true, nullable = false, length = 150)
    private String email;

    // MVP ONLY: stored as plain text so login can be added later.
    // PRODUCTION: store a BCrypt hash instead, never the raw password.
    // @JsonIgnore keeps the password out of every JSON response (Business Rule 13).
    @JsonIgnore
    @NotBlank
    @Column(nullable = false)
    private String password;

    // @PositiveOrZero is a safety net for Business Rule 14 (no negative balances).
    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Double creditBalance = INITIAL_CREDITS;

    // JPA requires a no-argument constructor to create objects from database rows.
    public Member() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Double getCreditBalance() {
        return creditBalance;
    }

    public void setCreditBalance(Double creditBalance) {
        this.creditBalance = creditBalance;
    }
}
