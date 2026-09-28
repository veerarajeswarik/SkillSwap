package com.example.backend.service;

import com.example.backend.dto.ConfirmSessionRequest;
import com.example.backend.dto.SessionRequestDto;
import com.example.backend.entity.Member;
import com.example.backend.entity.SessionRequest;
import com.example.backend.entity.SessionStatus;
import com.example.backend.entity.SkillOffer;
import com.example.backend.exception.InsufficientCreditException;
import com.example.backend.repository.MemberRepository;
import com.example.backend.repository.SessionRequestRepository;
import com.example.backend.repository.SkillOfferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the core business rules.
 * Every dependency is a Mockito "mock" (a fake object we control), so no
 * database or Spring context is needed.
 */
@ExtendWith(MockitoExtension.class)
class SessionRequestServiceTest {

    @Mock private SessionRequestRepository sessionRequestRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private SkillOfferRepository skillOfferRepository;
    @Mock private MemberService memberService;
    @Mock private SkillOfferService skillOfferService;
    @Mock private CreditLedgerService creditLedgerService;

    // Mockito builds the real service and passes the mocks into its constructor.
    @InjectMocks private SessionRequestService service;

    private Member veera;   // provider, 5 credits
    private Member ravi;    // requester, 10 credits
    private SkillOffer java;

    @BeforeEach
    void setUp() {
        veera = member(1L, "Veera", 5.0);
        ravi = member(2L, "Ravi", 10.0);

        java = new SkillOffer();
        java.setId(1L);
        java.setProvider(veera);
        java.setSkillName("Java");
        java.setAvailableHours(10.0);
    }

    // ---------------- create ----------------

    @Test
    void createsPendingRequestWithProviderTakenFromOffer() {
        when(memberService.getMemberById(2L)).thenReturn(ravi);
        when(skillOfferService.getSkillById(1L)).thenReturn(java);
        when(sessionRequestRepository.save(any(SessionRequest.class))).thenAnswer(call -> call.getArgument(0));

        SessionRequest created = service.createSessionRequest(dto(2L, 1L, 2.0));

        assertEquals(SessionStatus.PENDING, created.getStatus());
        assertSame(veera, created.getProvider());
        assertEquals(10.0, ravi.getCreditBalance()); // Rule 9: no credits move yet
    }

    @Test
    void cannotRequestOwnSkill() {
        when(memberService.getMemberById(1L)).thenReturn(veera);
        when(skillOfferService.getSkillById(1L)).thenReturn(java);

        assertThrows(IllegalStateException.class, () -> service.createSessionRequest(dto(1L, 1L, 2.0)));
        verify(sessionRequestRepository, never()).save(any());
    }

    @Test
    void cannotRequestMoreHoursThanAvailable() {
        when(memberService.getMemberById(2L)).thenReturn(ravi);
        when(skillOfferService.getSkillById(1L)).thenReturn(java);

        assertThrows(IllegalStateException.class, () -> service.createSessionRequest(dto(2L, 1L, 11.0)));
    }

    @Test
    void cannotRequestWithoutEnoughCredits() {
        ravi.setCreditBalance(3.0);
        when(memberService.getMemberById(2L)).thenReturn(ravi);
        when(skillOfferService.getSkillById(1L)).thenReturn(java);

        assertThrows(InsufficientCreditException.class, () -> service.createSessionRequest(dto(2L, 1L, 5.0)));
    }

    // ---------------- confirm ----------------

    @Test
    void confirmTransfersCreditsAndWritesLedger() {
        SessionRequest session = pendingSession(2.0);
        when(sessionRequestRepository.findById(7L)).thenReturn(Optional.of(session));
        when(sessionRequestRepository.save(any(SessionRequest.class))).thenAnswer(call -> call.getArgument(0));

        SessionRequest confirmed = service.confirmSession(7L, confirm(2.0));

        assertEquals(8.0, ravi.getCreditBalance());      // 10 -> 8
        assertEquals(7.0, veera.getCreditBalance());     // 5  -> 7
        assertEquals(8.0, java.getAvailableHours());     // 10 -> 8
        assertEquals(SessionStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(2.0, confirmed.getActualHoursDelivered());
        assertNotNull(confirmed.getCompletedAt());
        verify(creditLedgerService).recordDebit(ravi, session, 2.0, "Completed Java session");
        verify(creditLedgerService).recordCredit(veera, session, 2.0, "Provided Java session");
    }

    @Test
    void confirmUsesActualHoursNotRequestedHours() {
        SessionRequest session = pendingSession(2.0);
        when(sessionRequestRepository.findById(7L)).thenReturn(Optional.of(session));
        when(sessionRequestRepository.save(any(SessionRequest.class))).thenAnswer(call -> call.getArgument(0));

        service.confirmSession(7L, confirm(1.5));

        assertEquals(8.5, ravi.getCreditBalance());
        assertEquals(6.5, veera.getCreditBalance());
    }

    @Test
    void cannotConfirmTwice() {
        SessionRequest session = pendingSession(2.0);
        session.setStatus(SessionStatus.CONFIRMED);
        when(sessionRequestRepository.findById(7L)).thenReturn(Optional.of(session));

        assertThrows(IllegalStateException.class, () -> service.confirmSession(7L, confirm(2.0)));
        assertEquals(10.0, ravi.getCreditBalance()); // nothing moved
    }

    @Test
    void cannotConfirmMoreThanRequested() {
        SessionRequest session = pendingSession(2.0);
        when(sessionRequestRepository.findById(7L)).thenReturn(Optional.of(session));

        assertThrows(IllegalStateException.class, () -> service.confirmSession(7L, confirm(3.0)));
    }

    @Test
    void cannotConfirmIfRequesterBalanceDroppedTooLow() {
        SessionRequest session = pendingSession(2.0);
        ravi.setCreditBalance(1.0);
        when(sessionRequestRepository.findById(7L)).thenReturn(Optional.of(session));

        assertThrows(InsufficientCreditException.class, () -> service.confirmSession(7L, confirm(2.0)));
        assertEquals(5.0, veera.getCreditBalance()); // provider not paid
    }

    // ---------------- helpers ----------------

    private static Member member(Long id, String name, Double credits) {
        Member m = new Member();
        m.setId(id);
        m.setName(name);
        m.setCreditBalance(credits);
        return m;
    }

    private SessionRequest pendingSession(Double requestedHours) {
        SessionRequest s = new SessionRequest();
        s.setId(7L);
        s.setRequester(ravi);
        s.setProvider(veera);
        s.setSkillOffer(java);
        s.setRequestedHours(requestedHours);
        s.setStatus(SessionStatus.PENDING);
        return s;
    }

    private static SessionRequestDto dto(Long requesterId, Long skillOfferId, Double hours) {
        SessionRequestDto dto = new SessionRequestDto();
        dto.setRequesterId(requesterId);
        dto.setSkillOfferId(skillOfferId);
        dto.setRequestedHours(hours);
        return dto;
    }

    private static ConfirmSessionRequest confirm(Double hours) {
        ConfirmSessionRequest dto = new ConfirmSessionRequest();
        dto.setActualHoursDelivered(hours);
        return dto;
    }
}
