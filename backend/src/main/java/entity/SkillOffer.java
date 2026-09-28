package com.skillswap.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
@Table(name = "skill_offers")
public class SkillOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Member provider;

    @NotBlank
    private String skillName;

    @NotBlank
    @Column(length = 1000)
    private String description;

    @PositiveOrZero
    private Integer availableHours;

    private String status = "ACTIVE";

    public SkillOffer() {
    }

    public Long getId() {
        return id;
    }

    public Member getProvider() {
        return provider;
    }

    public void setProvider(Member provider) {
        this.provider = provider;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getAvailableHours() {
        return availableHours;
    }

    public void setAvailableHours(Integer availableHours) {
        this.availableHours = availableHours;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}