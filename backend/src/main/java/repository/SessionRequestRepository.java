package com.skillswap.repository;

import com.skillswap.entity.SessionRequest;
import com.skillswap.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionRequestRepository
        extends JpaRepository<SessionRequest, Long> {

    List<SessionRequest> findByRequesterId(Long requesterId);

    List<SessionRequest> findByProviderId(Long providerId);

    List<SessionRequest> findByProviderIdAndStatus(
            Long providerId,
            SessionStatus status
    );
}