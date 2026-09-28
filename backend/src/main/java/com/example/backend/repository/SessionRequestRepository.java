package com.example.backend.repository;

import com.example.backend.entity.SessionRequest;
import com.example.backend.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRequestRepository extends JpaRepository<SessionRequest, Long> {

    // Sessions I asked for (I am the learner).
    // SELECT * FROM session_requests WHERE requester_id = ?
    List<SessionRequest> findByRequesterId(Long requesterId);

    // Sessions asked of me (I am the teacher).
    // SELECT * FROM session_requests WHERE provider_id = ?
    List<SessionRequest> findByProviderId(Long providerId);

    // e.g. "my PENDING requests waiting for my confirmation".
    // SELECT * FROM session_requests WHERE provider_id = ? AND status = ?
    List<SessionRequest> findByProviderIdAndStatus(Long providerId, SessionStatus status);
}
