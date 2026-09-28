package com.example.backend.controller;

import com.example.backend.dto.ConfirmSessionRequest;
import com.example.backend.dto.SessionRequestDto;
import com.example.backend.entity.SessionRequest;
import com.example.backend.entity.SessionStatus;
import com.example.backend.service.SessionRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "*") // DEV ONLY
public class SessionRequestController {

    private final SessionRequestService sessionRequestService;

    public SessionRequestController(SessionRequestService sessionRequestService) {
        this.sessionRequestService = sessionRequestService;
    }

    // POST /api/sessions
    @PostMapping
    public ResponseEntity<SessionRequest> createSession(@Valid @RequestBody SessionRequestDto dto) {
        SessionRequest created = sessionRequestService.createSessionRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET /api/sessions
    @GetMapping
    public List<SessionRequest> getAllSessions() {
        return sessionRequestService.getAllSessions();
    }

    // GET /api/sessions/requester/{id}   (sessions I asked for)
    @GetMapping("/requester/{id}")
    public List<SessionRequest> getByRequester(@PathVariable Long id) {
        return sessionRequestService.getSessionsByRequester(id);
    }

    // GET /api/sessions/provider/{id}                  (sessions asked of me)
    // GET /api/sessions/provider/{id}?status=PENDING   (only those waiting for me)
    @GetMapping("/provider/{id}")
    public List<SessionRequest> getByProvider(@PathVariable Long id,
                                              @RequestParam(required = false) SessionStatus status) {
        if (status == null) {
            return sessionRequestService.getSessionsByProvider(id);
        }
        return sessionRequestService.getSessionsByProviderAndStatus(id, status);
    }

    // PUT /api/sessions/{id}/confirm   body: { "actualHoursDelivered": 2 }
    @PutMapping("/{id}/confirm")
    public SessionRequest confirmSession(@PathVariable Long id,
                                         @Valid @RequestBody ConfirmSessionRequest dto) {
        return sessionRequestService.confirmSession(id, dto);
    }

    // PUT /api/sessions/{id}/reject   (no body)
    @PutMapping("/{id}/reject")
    public SessionRequest rejectSession(@PathVariable Long id) {
        return sessionRequestService.rejectSession(id);
    }
}
