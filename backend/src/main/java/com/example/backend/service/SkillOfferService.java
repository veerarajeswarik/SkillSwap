package com.example.backend.service;

import com.example.backend.dto.SkillOfferRequest;
import com.example.backend.entity.Member;
import com.example.backend.entity.SkillOffer;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.SkillOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SkillOfferService {

    private final SkillOfferRepository skillOfferRepository;
    private final MemberService memberService;

    public SkillOfferService(SkillOfferRepository skillOfferRepository, MemberService memberService) {
        this.skillOfferRepository = skillOfferRepository;
        this.memberService = memberService;
    }

    @Transactional
    public SkillOffer createSkillOffer(SkillOfferRequest request) {
        // Business rule: provider must exist (throws ResourceNotFoundException if not).
        Member provider = memberService.getMemberById(request.getProviderId());

        // The DTO already checks this with @Positive; the service checks again because
        // services can also be called from places that skip DTO validation.
        if (request.getAvailableHours() == null || request.getAvailableHours() <= 0) {
            throw new IllegalStateException("Available hours must be greater than 0");
        }

        SkillOffer offer = new SkillOffer();
        offer.setProvider(provider);
        offer.setSkillName(request.getSkillName().trim());
        offer.setDescription(request.getDescription());
        offer.setAvailableHours(request.getAvailableHours());
        offer.setStatus(SkillOffer.STATUS_ACTIVE);

        return skillOfferRepository.save(offer);
    }

    @Transactional(readOnly = true)
    public List<SkillOffer> getActiveSkills() {
        return skillOfferRepository.findByStatus(SkillOffer.STATUS_ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<SkillOffer> getSkillsByProvider(Long providerId) {
        // Gives a clear 404 for an unknown member instead of an empty list.
        memberService.getMemberById(providerId);
        return skillOfferRepository.findByProviderId(providerId);
    }

    @Transactional(readOnly = true)
    public SkillOffer getSkillById(Long id) {
        return skillOfferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill offer not found with id " + id));
    }
}
