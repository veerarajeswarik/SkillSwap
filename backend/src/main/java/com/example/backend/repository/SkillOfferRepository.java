package com.example.backend.repository;

import com.example.backend.entity.SkillOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillOfferRepository extends JpaRepository<SkillOffer, Long> {

    // SELECT * FROM skill_offers WHERE status = ?
    List<SkillOffer> findByStatus(String status);

    // SELECT * FROM skill_offers WHERE provider_id = ?
    // "ProviderId" = the provider field, then its id field.
    List<SkillOffer> findByProviderId(Long providerId);
}
