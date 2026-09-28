package com.example.backend.service;

import com.example.backend.entity.CreditLedger;
import com.example.backend.entity.LedgerType;
import com.example.backend.entity.Member;
import com.example.backend.entity.SessionRequest;
import com.example.backend.repository.CreditLedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CreditLedgerService {

    private final CreditLedgerRepository creditLedgerRepository;
    private final MemberService memberService;

    public CreditLedgerService(CreditLedgerRepository creditLedgerRepository, MemberService memberService) {
        this.creditLedgerRepository = creditLedgerRepository;
        this.memberService = memberService;
    }

    // Credits were ADDED to this member (the provider earned them).
    @Transactional
    public CreditLedger recordCredit(Member member, SessionRequest session, Double hours, String description) {
        return createEntry(member, session, LedgerType.CREDIT, hours, description);
    }

    // Credits were REMOVED from this member (the requester spent them).
    @Transactional
    public CreditLedger recordDebit(Member member, SessionRequest session, Double hours, String description) {
        return createEntry(member, session, LedgerType.DEBIT, hours, description);
    }

    @Transactional(readOnly = true)
    public List<CreditLedger> getHistory(Long memberId) {
        memberService.getMemberById(memberId);
        return creditLedgerRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    private CreditLedger createEntry(Member member, SessionRequest session, LedgerType type,
                                     Double hours, String description) {
        CreditLedger entry = new CreditLedger();
        entry.setMember(member);
        entry.setSessionRequest(session);
        entry.setTransactionType(type);
        entry.setHours(hours);
        entry.setDescription(description);
        return creditLedgerRepository.save(entry);
    }
}
