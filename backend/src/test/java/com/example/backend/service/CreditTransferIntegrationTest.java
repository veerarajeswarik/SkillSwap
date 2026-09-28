package com.example.backend.service;

import com.example.backend.dto.ConfirmSessionRequest;
import com.example.backend.entity.CreditLedger;
import com.example.backend.entity.LedgerType;
import com.example.backend.entity.Member;
import com.example.backend.entity.SessionRequest;
import com.example.backend.entity.SessionStatus;
import com.example.backend.entity.SkillOffer;
import com.example.backend.repository.CreditLedgerRepository;
import com.example.backend.repository.MemberRepository;
import com.example.backend.repository.SessionRequestRepository;
import com.example.backend.repository.SkillOfferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

/**
 * Integration tests: the REAL services, repositories, Hibernate and MySQL
 * (the skillswap_test_db database from src/test/resources/application.properties).
 *
 * Deliberately NOT annotated @Transactional: every read below goes to the database,
 * so we see exactly what was committed or rolled back.
 */
@SpringBootTest
class CreditTransferIntegrationTest {

    @Autowired private SessionRequestService sessionRequestService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private SkillOfferRepository skillOfferRepository;
    @Autowired private SessionRequestRepository sessionRequestRepository;
    @Autowired private CreditLedgerRepository creditLedgerRepository;

    // A "spy" is the real bean, but we can make individual methods misbehave.
    @MockitoSpyBean private CreditLedgerService creditLedgerService;

    private Member teacher;
    private Member learner;
    private SkillOffer skill;
    private SessionRequest session;

    @BeforeEach
    void createData() {
        reset(creditLedgerService);
        creditLedgerRepository.deleteAll();
        sessionRequestRepository.deleteAll();
        skillOfferRepository.deleteAll();
        memberRepository.deleteAll();

        teacher = memberRepository.save(member("Teacher", "teacher@test.com"));
        learner = memberRepository.save(member("Learner", "learner@test.com"));

        SkillOffer offer = new SkillOffer();
        offer.setProvider(teacher);
        offer.setSkillName("Java");
        offer.setAvailableHours(10.0);
        skill = skillOfferRepository.save(offer);

        SessionRequest request = new SessionRequest();
        request.setRequester(learner);
        request.setProvider(teacher);
        request.setSkillOffer(skill);
        request.setRequestedHours(2.0);
        session = sessionRequestRepository.save(request);
    }

    @Test
    void confirmCommitsEverythingTogether() {
        sessionRequestService.confirmSession(session.getId(), hours(2.0));

        assertEquals(3.0, balanceOf(learner));   // 5 - 2
        assertEquals(7.0, balanceOf(teacher));   // 5 + 2
        assertEquals(8.0, skillOfferRepository.findById(skill.getId()).orElseThrow().getAvailableHours());

        SessionRequest saved = sessionRequestRepository.findById(session.getId()).orElseThrow();
        assertEquals(SessionStatus.CONFIRMED, saved.getStatus());
        assertEquals(2.0, saved.getActualHoursDelivered());
        assertNotNull(saved.getCompletedAt());
        assertNotNull(saved.getCreatedAt()); // set by @PrePersist

        List<CreditLedger> learnerLedger = creditLedgerRepository.findByMemberIdOrderByCreatedAtDesc(learner.getId());
        List<CreditLedger> teacherLedger = creditLedgerRepository.findByMemberIdOrderByCreatedAtDesc(teacher.getId());
        assertEquals(1, learnerLedger.size());
        assertEquals(LedgerType.DEBIT, learnerLedger.get(0).getTransactionType());
        assertEquals(1, teacherLedger.size());
        assertEquals(LedgerType.CREDIT, teacherLedger.get(0).getTransactionType());
    }

    @Test
    void crashHalfwayThroughRollsBackEverything() {
        // Steps 1-17 run for real (balances changed, 4 saves, DEBIT row written)...
        // ...then step 18 (CREDIT row) crashes.
        doThrow(new RuntimeException("Simulated database crash while writing CREDIT row"))
                .when(creditLedgerService).recordCredit(any(), any(), any(), any());

        RuntimeException crash = assertThrows(RuntimeException.class,
                () -> sessionRequestService.confirmSession(session.getId(), hours(2.0)));
        System.out.println("Crash thrown as expected: " + crash.getMessage());

        // Read everything back from MySQL: it must be exactly as before the confirm.
        assertEquals(5.0, balanceOf(learner), "learner's debit was rolled back");
        assertEquals(5.0, balanceOf(teacher), "teacher never got paid");
        assertEquals(10.0, skillOfferRepository.findById(skill.getId()).orElseThrow().getAvailableHours());

        SessionRequest after = sessionRequestRepository.findById(session.getId()).orElseThrow();
        assertEquals(SessionStatus.PENDING, after.getStatus(), "session is still PENDING");
        assertNull(after.getActualHoursDelivered());
        assertNull(after.getCompletedAt());

        assertTrue(creditLedgerRepository.findAll().isEmpty(), "the DEBIT row written in step 17 was rolled back");

        // And because nothing was committed, the provider can simply try again.
        reset(creditLedgerService);
        sessionRequestService.confirmSession(session.getId(), hours(2.0));
        assertEquals(3.0, balanceOf(learner));
        assertEquals(7.0, balanceOf(teacher));
        assertEquals(2, creditLedgerRepository.findAll().size());
    }

    @Test
    void emailUniqueConstraintIsEnforcedByMySql() {
        Member duplicate = member("Copy", "teacher@test.com");
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> memberRepository.save(duplicate));
    }

    // ---------------- helpers ----------------

    private Double balanceOf(Member member) {
        return memberRepository.findById(member.getId()).orElseThrow().getCreditBalance();
    }

    private static Member member(String name, String email) {
        Member m = new Member();
        m.setName(name);
        m.setEmail(email);
        m.setPassword("secret1");
        return m;
    }

    private static ConfirmSessionRequest hours(Double value) {
        ConfirmSessionRequest dto = new ConfirmSessionRequest();
        dto.setActualHoursDelivered(value);
        return dto;
    }
}
