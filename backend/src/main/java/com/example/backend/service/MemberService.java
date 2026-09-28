package com.example.backend.service;

import com.example.backend.dto.RegisterMemberRequest;
import com.example.backend.entity.Member;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    // Constructor injection: Spring passes in the MemberRepository bean.
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public Member registerMember(RegisterMemberRequest request) {
        // Normalise so "Veera@Gmail.com " and "veera@gmail.com" count as the same email.
        String email = request.getEmail().trim().toLowerCase();

        // Business Rule 2: email must be unique.
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email is already registered: " + email);
        }

        Member member = new Member();
        member.setName(request.getName().trim());
        member.setEmail(email);
        // MVP ONLY: plain text. PRODUCTION: store passwordEncoder.encode(password) (BCrypt).
        member.setPassword(request.getPassword());
        // Business Rule 1: every new member starts with 5 credits.
        member.setCreditBalance(Member.INITIAL_CREDITS);

        return memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Member getMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id " + id));
    }

    @Transactional(readOnly = true)
    public Double getBalance(Long id) {
        return getMemberById(id).getCreditBalance();
    }
}
