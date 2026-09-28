package com.skillswap.service;

import com.skillswap.dto.RegisterMemberRequest;
import com.skillswap.entity.Member;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member register(RegisterMemberRequest request) {

        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException(
                    "Email is already registered"
            );
        }

        Member member = new Member();

        member.setName(request.getName());
        member.setEmail(request.getEmail());
        member.setPassword(request.getPassword());

        // Every new member gets 5 time credits
        member.setCreditBalance(5);

        return memberRepository.save(member);
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    public Member getMember(Long id) {

        return memberRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Member not found with ID: " + id
                        )
                );
    }

    public Integer getBalance(Long id) {

        Member member = getMember(id);

        return member.getCreditBalance();
    }
}