package com.skillswap.controller;

import com.skillswap.dto.RegisterMemberRequest;
import com.skillswap.entity.Member;
import com.skillswap.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
@CrossOrigin(origins = "*")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("/register")
    public ResponseEntity<Member> register(
            @Valid @RequestBody RegisterMemberRequest request) {

        Member member =
                memberService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(member);
    }

    @GetMapping
    public ResponseEntity<List<Member>> getAllMembers() {

        return ResponseEntity.ok(
                memberService.getAllMembers()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Member> getMember(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                memberService.getMember(id)
        );
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(
            @PathVariable Long id) {

        Map<String, Object> response =
                new HashMap<>();

        response.put("memberId", id);
        response.put(
                "creditBalance",
                memberService.getBalance(id)
        );

        return ResponseEntity.ok(response);
    }
}