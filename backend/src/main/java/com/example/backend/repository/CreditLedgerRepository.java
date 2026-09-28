package com.example.backend.repository;

import com.example.backend.entity.CreditLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditLedgerRepository extends JpaRepository<CreditLedger, Long> {

    // A member's transaction history, newest first.
    // SELECT * FROM credit_ledger WHERE member_id = ? ORDER BY created_at DESC
    List<CreditLedger> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
