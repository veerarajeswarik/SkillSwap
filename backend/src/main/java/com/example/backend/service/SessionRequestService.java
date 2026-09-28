package com.example.backend.service;

import com.example.backend.dto.ConfirmSessionRequest;
import com.example.backend.dto.SessionRequestDto;
import com.example.backend.entity.Member;
import com.example.backend.entity.SessionRequest;
import com.example.backend.entity.SessionStatus;
import com.example.backend.entity.SkillOffer;
import com.example.backend.exception.InsufficientCreditException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.MemberRepository;
import com.example.backend.repository.SessionRequestRepository;
import com.example.backend.repository.SkillOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SessionRequestService {

    private final SessionRequestRepository sessionRequestRepository;
    private final MemberRepository memberRepository;
    private final SkillOfferRepository skillOfferRepository;
    private final MemberService memberService;
    private final SkillOfferService skillOfferService;
    private final CreditLedgerService creditLedgerService;

    public SessionRequestService(SessionRequestRepository sessionRequestRepository,
                                 MemberRepository memberRepository,
                                 SkillOfferRepository skillOfferRepository,
                                 MemberService memberService,
                                 SkillOfferService skillOfferService,
                                 CreditLedgerService creditLedgerService) {
        this.sessionRequestRepository = sessionRequestRepository;
        this.memberRepository = memberRepository;
        this.skillOfferRepository = skillOfferRepository;
        this.memberService = memberService;
        this.skillOfferService = skillOfferService;
        this.creditLedgerService = creditLedgerService;
    }

    // ------------------------------------------------------------------
    // CREATE: no credits move here, we only check the requester COULD pay.
    // ------------------------------------------------------------------
    @Transactional
    public SessionRequest createSessionRequest(SessionRequestDto dto) {
        // 1. Find requester.
        Member requester = memberService.getMemberById(dto.getRequesterId());
        // 2. Find skill offer.
        SkillOffer offer = skillOfferService.getSkillById(dto.getSkillOfferId());
        // 3. The provider comes from the offer, never from the client.
        Member provider = offer.getProvider();

        // Rule 3: a member cannot request their own skill.
        if (requester.getId().equals(provider.getId())) {
            throw new IllegalStateException("You cannot request a session for your own skill");
        }

        if (!SkillOffer.STATUS_ACTIVE.equals(offer.getStatus())) {
            throw new IllegalStateException("This skill offer is not active");
        }

        Double hours = dto.getRequestedHours();

        // Rule 4: requested hours must be greater than zero.
        if (hours == null || hours <= 0) {
            throw new IllegalStateException("Requested hours must be greater than 0");
        }

        // Rule 6: requested hours cannot exceed the offer's available hours.
        if (hours > offer.getAvailableHours()) {
            throw new IllegalStateException("Requested " + hours + " hours, but only "
                    + offer.getAvailableHours() + " hours are available");
        }

        // Rule 5: requester must have enough credits.
        if (requester.getCreditBalance() < hours) {
            throw new InsufficientCreditException("Insufficient credits: you have "
                    + requester.getCreditBalance() + " but requested " + hours + " hours");
        }

        // 7-9. Create, mark PENDING, save.
        SessionRequest session = new SessionRequest();
        session.setRequester(requester);
        session.setProvider(provider);
        session.setSkillOffer(offer);
        session.setRequestedHours(hours);
        session.setMessage(dto.getMessage());
        session.setStatus(SessionStatus.PENDING);

        return sessionRequestRepository.save(session);
    }

    // ------------------------------------------------------------------
    // CONFIRM: the only place credits move. All-or-nothing (Rule 11).
    // ------------------------------------------------------------------
    @Transactional
    public SessionRequest confirmSession(Long sessionId, ConfirmSessionRequest dto) {
        // 1. Find session.
        SessionRequest session = getSessionById(sessionId);

        // 2. Rule 7: only PENDING sessions can be confirmed.
        if (session.getStatus() != SessionStatus.PENDING) {
            throw new IllegalStateException("Only PENDING sessions can be confirmed; this one is "
                    + session.getStatus());
        }

        Double actualHours = dto.getActualHoursDelivered();

        // 3. Actual hours must be greater than 0.
        if (actualHours == null || actualHours <= 0) {
            throw new IllegalStateException("Actual hours delivered must be greater than 0");
        }

        // 4. Rule 8: cannot deliver more than was requested.
        if (actualHours > session.getRequestedHours()) {
            throw new IllegalStateException("Actual hours (" + actualHours
                    + ") cannot exceed requested hours (" + session.getRequestedHours() + ")");
        }

        Member requester = session.getRequester();
        Member provider = session.getProvider();
        SkillOffer offer = session.getSkillOffer();

        // 5. Rule 14: requester's balance must not go negative.
        //    (Checked again here: the balance may have dropped since the request was created.)
        if (requester.getCreditBalance() < actualHours) {
            throw new InsufficientCreditException("Requester has only "
                    + requester.getCreditBalance() + " credits, but " + actualHours + " are needed");
        }

        // 6. Rule 15: skill hours must not go negative.
        if (offer.getAvailableHours() < actualHours) {
            throw new IllegalStateException("Skill offer has only "
                    + offer.getAvailableHours() + " hours left");
        }

        // 7-9. Rule 10: move credits and reduce available hours.
        requester.setCreditBalance(requester.getCreditBalance() - actualHours);
        provider.setCreditBalance(provider.getCreditBalance() + actualHours);
        offer.setAvailableHours(offer.getAvailableHours() - actualHours);

        // 10-12. Update the session.
        session.setActualHoursDelivered(actualHours);
        session.setStatus(SessionStatus.CONFIRMED);
        session.setCompletedAt(LocalDateTime.now());

        // 13-16. Save everything.
        memberRepository.save(requester);
        memberRepository.save(provider);
        skillOfferRepository.save(offer);
        SessionRequest saved = sessionRequestRepository.save(session);

        // 17-18. Rule 12: every transfer creates ledger entries.
        String skillName = offer.getSkillName();
        creditLedgerService.recordDebit(requester, saved, actualHours, "Completed " + skillName + " session");
        creditLedgerService.recordCredit(provider, saved, actualHours, "Provided " + skillName + " session");

        return saved;
    }

    // ------------------------------------------------------------------
    // REJECT: provider declines. No credits move.
    // ------------------------------------------------------------------
    @Transactional
    public SessionRequest rejectSession(Long sessionId) {
        SessionRequest session = getSessionById(sessionId);

        if (session.getStatus() != SessionStatus.PENDING) {
            throw new IllegalStateException("Only PENDING sessions can be rejected; this one is "
                    + session.getStatus());
        }

        session.setStatus(SessionStatus.REJECTED);
        return sessionRequestRepository.save(session);
    }

    // ------------------------------------------------------------------
    // READ
    // ------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<SessionRequest> getAllSessions() {
        return sessionRequestRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SessionRequest getSessionById(Long id) {
        return sessionRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id " + id));
    }

    @Transactional(readOnly = true)
    public List<SessionRequest> getSessionsByRequester(Long requesterId) {
        memberService.getMemberById(requesterId);
        return sessionRequestRepository.findByRequesterId(requesterId);
    }

    @Transactional(readOnly = true)
    public List<SessionRequest> getSessionsByProvider(Long providerId) {
        memberService.getMemberById(providerId);
        return sessionRequestRepository.findByProviderId(providerId);
    }

    @Transactional(readOnly = true)
    public List<SessionRequest> getSessionsByProviderAndStatus(Long providerId, SessionStatus status) {
        memberService.getMemberById(providerId);
        return sessionRequestRepository.findByProviderIdAndStatus(providerId, status);
    }
}
