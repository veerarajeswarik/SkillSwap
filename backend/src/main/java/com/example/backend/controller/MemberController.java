package com.example.backend.controller;

import com.example.backend.dto.RegisterMemberRequest;
import com.example.backend.entity.Member;
import com.example.backend.service.MemberService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/members")
@CrossOrigin(origins = "*") // DEV ONLY: allow the frontend from any origin. Restrict in production.
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // POST /api/members/register
    // Password is never in the response: Member.password has @JsonIgnore.
    @PostMapping("/register")
    public ResponseEntity<Member> register(@Valid @RequestBody RegisterMemberRequest request) {
        Member created = memberService.registerMember(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET /api/members
    @GetMapping
    public List<Member> getAllMembers() {
        return memberService.getAllMembers();
    }

    // GET /api/members/{id}
    @GetMapping("/{id}")
    public Member getMemberById(@PathVariable Long id) {
        return memberService.getMemberById(id);
    }

    // GET /api/members/{id}/balance  ->  { "memberId": 1, "creditBalance": 5.0 }
    @GetMapping("/{id}/balance")
    public Map<String, Object> getBalance(@PathVariable Long id) {
        return Map.of(
                "memberId", id,
                "creditBalance", memberService.getBalance(id)
        );
    }
}
