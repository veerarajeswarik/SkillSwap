package com.skillswap.repository;

import com.skillswap.entity.SkillOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillOfferRepository extends JpaRepository<SkillOffer, Long> {

    List<SkillOffer> findByStatus(String status);

    List<SkillOffer> findByProviderId(Long providerId);
}