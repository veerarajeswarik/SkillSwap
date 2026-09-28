package com.skillswap.controller;

import com.skillswap.dto.SkillOfferRequest;
import com.skillswap.entity.SkillOffer;
import com.skillswap.service.SkillOfferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@CrossOrigin(origins = "*")
public class SkillOfferController {

    private final SkillOfferService skillOfferService;

    public SkillOfferController(
            SkillOfferService skillOfferService) {

        this.skillOfferService = skillOfferService;
    }

    @PostMapping
    public ResponseEntity<SkillOffer> createSkill(
            @Valid @RequestBody SkillOfferRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        skillOfferService.createSkill(request)
                );
    }

    @GetMapping
    public ResponseEntity<List<SkillOffer>> getAllSkills() {

        return ResponseEntity.ok(
                skillOfferService.getAllSkills()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<SkillOffer> getSkill(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                skillOfferService.getSkill(id)
        );
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<SkillOffer>>
    getProviderSkills(
            @PathVariable Long providerId) {

        return ResponseEntity.ok(
                skillOfferService
                        .getProviderSkills(providerId)
        );
    }
}