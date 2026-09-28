package com.skillswap.service;

import com.skillswap.dto.SkillOfferRequest;
import com.skillswap.entity.Member;
import com.skillswap.entity.SkillOffer;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.repository.MemberRepository;
import com.skillswap.repository.SkillOfferRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SkillOfferService {

    private final SkillOfferRepository skillOfferRepository;
    private final MemberRepository memberRepository;

    public SkillOfferService(
            SkillOfferRepository skillOfferRepository,
            MemberRepository memberRepository) {

        this.skillOfferRepository = skillOfferRepository;
        this.memberRepository = memberRepository;
    }

    public SkillOffer createSkill(SkillOfferRequest request) {

        Member provider = memberRepository
                .findById(request.getProviderId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Provider not found"
                        )
                );

        SkillOffer skill = new SkillOffer();

        skill.setProvider(provider);
        skill.setSkillName(request.getSkillName());
        skill.setDescription(request.getDescription());
        skill.setAvailableHours(request.getAvailableHours());
        skill.setStatus("ACTIVE");

        return skillOfferRepository.save(skill);
    }

    public List<SkillOffer> getAllSkills() {
        return skillOfferRepository.findByStatus("ACTIVE");
    }

    public List<SkillOffer> getProviderSkills(Long providerId) {
        return skillOfferRepository.findByProviderId(providerId);
    }

    public SkillOffer getSkill(Long id) {

        return skillOfferRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Skill offer not found"
                        )
                );
    }
}