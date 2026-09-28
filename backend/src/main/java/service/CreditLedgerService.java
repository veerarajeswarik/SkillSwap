package com.skillswap.service;

import com.skillswap.entity.CreditLedger;
import com.skillswap.entity.LedgerType;
import com.skillswap.entity.Member;
import com.skillswap.entity.SessionRequest;
import com.skillswap.repository.CreditLedgerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CreditLedgerService {

    private final CreditLedgerRepository creditLedgerRepository;

    public CreditLedgerService(
            CreditLedgerRepository creditLedgerRepository) {

        this.creditLedgerRepository = creditLedgerRepository;
    }

    public void createCreditEntry(
            Member member,
            SessionRequest session,
            Integer hours) {

        CreditLedger ledger = new CreditLedger();

        ledger.setMember(member);
        ledger.setSessionRequest(session);
        ledger.setTransactionType(LedgerType.CREDIT);
        ledger.setHours(hours);
        ledger.setDescription(
                "Credits earned from completed skill session"
        );

        creditLedgerRepository.save(ledger);
    }

    public void createDebitEntry(
            Member member,
            SessionRequest session,
            Integer hours) {

        CreditLedger ledger = new CreditLedger();

        ledger.setMember(member);
        ledger.setSessionRequest(session);
        ledger.setTransactionType(LedgerType.DEBIT);
        ledger.setHours(hours);
        ledger.setDescription(
                "Credits spent on skill session"
        );

        creditLedgerRepository.save(ledger);
    }

    public List<CreditLedger> getMemberHistory(Long memberId) {

        return creditLedgerRepository
                .findByMemberIdOrderByCreatedAtDesc(memberId);
    }
}