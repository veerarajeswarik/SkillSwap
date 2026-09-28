package com.skillswap.service;

import com.skillswap.dto.ConfirmSessionRequest;
import com.skillswap.dto.SessionRequestDto;
import com.skillswap.entity.*;
import com.skillswap.exception.InsufficientCreditException;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.repository.MemberRepository;
import com.skillswap.repository.SessionRequestRepository;
import com.skillswap.repository.SkillOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SessionRequestService {

    private final SessionRequestRepository sessionRepository;
    private final MemberRepository memberRepository;
    private final SkillOfferRepository skillOfferRepository;
    private final CreditLedgerService creditLedgerService;

    public SessionRequestService(
            SessionRequestRepository sessionRepository,
            MemberRepository memberRepository,
            SkillOfferRepository skillOfferRepository,
            CreditLedgerService creditLedgerService) {

        this.sessionRepository = sessionRepository;
        this.memberRepository = memberRepository;
        this.skillOfferRepository = skillOfferRepository;
        this.creditLedgerService = creditLedgerService;
    }

    // ----------------------------------------------------
    // REQUEST A SESSION
    // ----------------------------------------------------

    public SessionRequest createRequest(
            SessionRequestDto request) {

        Member requester = memberRepository
                .findById(request.getRequesterId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Requester not found"
                        )
                );

        SkillOffer skill = skillOfferRepository
                .findById(request.getSkillOfferId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Skill offer not found"
                        )
                );

        Member provider = skill.getProvider();

        // A member cannot request their own skill
        if (requester.getId().equals(provider.getId())) {
            throw new IllegalStateException(
                    "You cannot request your own skill"
            );
        }

        // Check skill availability
        if (request.getRequestedHours()
                > skill.getAvailableHours()) {

            throw new IllegalStateException(
                    "Requested hours exceed available skill hours"
            );
        }

        // BUSINESS RULE:
        // requester cannot book if balance would become negative
        if (requester.getCreditBalance()
                < request.getRequestedHours()) {

            throw new InsufficientCreditException(
                    "Insufficient time credits. " +
                    "Your balance is "
                    + requester.getCreditBalance()
                    + " hours."
            );
        }

        SessionRequest session = new SessionRequest();

        session.setRequester(requester);
        session.setProvider(provider);
        session.setSkillOffer(skill);
        session.setRequestedHours(
                request.getRequestedHours()
        );
        session.setMessage(request.getMessage());
        session.setStatus(SessionStatus.PENDING);

        return sessionRepository.save(session);
    }

    // ----------------------------------------------------
    // GET ALL REQUESTS
    // ----------------------------------------------------

    public List<SessionRequest> getAllRequests() {
        return sessionRepository.findAll();
    }

    // ----------------------------------------------------
    // REQUESTS MADE BY MEMBER
    // ----------------------------------------------------

    public List<SessionRequest> getRequesterRequests(
            Long requesterId) {

        return sessionRepository
                .findByRequesterId(requesterId);
    }

    // ----------------------------------------------------
    // REQUESTS RECEIVED BY PROVIDER
    // ----------------------------------------------------

    public List<SessionRequest> getProviderRequests(
            Long providerId) {

        return sessionRepository
                .findByProviderId(providerId);
    }

    // ----------------------------------------------------
    // CONFIRM SESSION
    // ----------------------------------------------------

    @Transactional
    public SessionRequest confirmSession(
            Long sessionId,
            ConfirmSessionRequest request) {

        SessionRequest session =
                sessionRepository.findById(sessionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Session request not found"
                                )
                        );

        // Only pending sessions can be confirmed
        if (session.getStatus()
                != SessionStatus.PENDING) {

            throw new IllegalStateException(
                    "This session has already been processed"
            );
        }

        int actualHours =
                request.getActualHoursDelivered();

        // Actual delivered hours cannot exceed requested hours
        if (actualHours
                > session.getRequestedHours()) {

            throw new IllegalStateException(
                    "Actual delivered hours cannot exceed " +
                    "requested hours"
            );
        }

        Member requester =
                session.getRequester();

        Member provider =
                session.getProvider();

        SkillOffer skill =
                session.getSkillOffer();

        // Check requester's current balance AGAIN.
        // This is important because the balance could have
        // changed after the request was created.
        if (requester.getCreditBalance()
                < actualHours) {

            throw new InsufficientCreditException(
                    "Requester no longer has enough credits"
            );
        }

        // Check remaining skill hours
        if (skill.getAvailableHours()
                < actualHours) {

            throw new IllegalStateException(
                    "Not enough hours remaining for this skill"
            );
        }

        // ------------------------------------------------
        // TRANSFER CREDITS
        // ------------------------------------------------

        requester.setCreditBalance(
                requester.getCreditBalance()
                        - actualHours
        );

        provider.setCreditBalance(
                provider.getCreditBalance()
                        + actualHours
        );

        // Reduce available skill hours
        skill.setAvailableHours(
                skill.getAvailableHours()
                        - actualHours
        );

        if (skill.getAvailableHours() == 0) {
            skill.setStatus("COMPLETED");
        }

        // Store actual delivered hours
        session.setActualHoursDelivered(actualHours);

        session.setStatus(SessionStatus.CONFIRMED);

        session.setCompletedAt(
                LocalDateTime.now()
        );

        // Save members
        memberRepository.save(requester);
        memberRepository.save(provider);

        // Save skill
        skillOfferRepository.save(skill);

        // Save session
        sessionRepository.save(session);

        // ------------------------------------------------
        // CREDIT LEDGER
        // ------------------------------------------------

        // Requester loses credits
        creditLedgerService.createDebitEntry(
                requester,
                session,
                actualHours
        );

        // Provider gains credits
        creditLedgerService.createCreditEntry(
                provider,
                session,
                actualHours
        );

        return session;
    }

    // ----------------------------------------------------
    // REJECT SESSION
    // ----------------------------------------------------

    public SessionRequest rejectSession(Long sessionId) {

        SessionRequest session =
                sessionRepository.findById(sessionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Session request not found"
                                )
                        );

        if (session.getStatus()
                != SessionStatus.PENDING) {

            throw new IllegalStateException(
                    "This session has already been processed"
            );
        }

        session.setStatus(SessionStatus.REJECTED);

        return sessionRepository.save(session);
    }
}