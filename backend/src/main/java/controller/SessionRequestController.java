package com.skillswap.controller;

import com.skillswap.dto.ConfirmSessionRequest;
import com.skillswap.dto.SessionRequestDto;
import com.skillswap.entity.SessionRequest;
import com.skillswap.service.SessionRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "*")
public class SessionRequestController {

    private final SessionRequestService sessionService;

    public SessionRequestController(
            SessionRequestService sessionService) {

        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<SessionRequest> createRequest(
            @Valid @RequestBody SessionRequestDto request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        sessionService.createRequest(request)
                );
    }

    @GetMapping
    public ResponseEntity<List<SessionRequest>>
    getAllRequests() {

        return ResponseEntity.ok(
                sessionService.getAllRequests()
        );
    }

    @GetMapping("/requester/{requesterId}")
    public ResponseEntity<List<SessionRequest>>
    getRequesterRequests(
            @PathVariable Long requesterId) {

        return ResponseEntity.ok(
                sessionService
                        .getRequesterRequests(requesterId)
        );
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<SessionRequest>>
    getProviderRequests(
            @PathVariable Long providerId) {

        return ResponseEntity.ok(
                sessionService
                        .getProviderRequests(providerId)
        );
    }

    @PutMapping("/{sessionId}/confirm")
    public ResponseEntity<SessionRequest> confirmSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody ConfirmSessionRequest request) {

        return ResponseEntity.ok(
                sessionService.confirmSession(
                        sessionId,
                        request
                )
        );
    }

    @PutMapping("/{sessionId}/reject")
    public ResponseEntity<SessionRequest> rejectSession(
            @PathVariable Long sessionId) {

        return ResponseEntity.ok(
                sessionService.rejectSession(sessionId)
        );
    }
}