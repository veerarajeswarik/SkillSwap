package com.example.backend.controller;

import com.example.backend.dto.SkillOfferRequest;
import com.example.backend.entity.SkillOffer;
import com.example.backend.service.SkillOfferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@CrossOrigin(origins = "*") // DEV ONLY
public class SkillOfferController {

    private final SkillOfferService skillOfferService;

    public SkillOfferController(SkillOfferService skillOfferService) {
        this.skillOfferService = skillOfferService;
    }

    // POST /api/skills
    @PostMapping
    public ResponseEntity<SkillOffer> createSkillOffer(@Valid @RequestBody SkillOfferRequest request) {
        SkillOffer created = skillOfferService.createSkillOffer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET /api/skills  (active offers only, for the Browse page)
    @GetMapping
    public List<SkillOffer> getActiveSkills() {
        return skillOfferService.getActiveSkills();
    }

    // GET /api/skills/{id}
    @GetMapping("/{id}")
    public SkillOffer getSkillById(@PathVariable Long id) {
        return skillOfferService.getSkillById(id);
    }

    // GET /api/skills/provider/{providerId}
    @GetMapping("/provider/{providerId}")
    public List<SkillOffer> getSkillsByProvider(@PathVariable Long providerId) {
        return skillOfferService.getSkillsByProvider(providerId);
    }
}
