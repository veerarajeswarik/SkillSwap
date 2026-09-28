package com.example.backend.controller;

import com.example.backend.dto.ConfirmSessionRequest;
import com.example.backend.dto.RegisterMemberRequest;
import com.example.backend.entity.Member;
import com.example.backend.entity.SessionRequest;
import com.example.backend.entity.SessionStatus;
import com.example.backend.service.CreditLedgerService;
import com.example.backend.service.MemberService;
import com.example.backend.service.SessionRequestService;
import com.example.backend.service.SkillOfferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the HTTP layer only: URL -> controller method, JSON in/out, @Valid, status codes.
 * @WebMvcTest starts just the web part of Spring (no Tomcat, no database);
 * services are replaced with Mockito mocks via @MockitoBean.
 */
@WebMvcTest
class ControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MemberService memberService;
    @MockitoBean private SkillOfferService skillOfferService;
    @MockitoBean private SessionRequestService sessionRequestService;
    @MockitoBean private CreditLedgerService creditLedgerService;

    @Test
    void registerReturns201AndNeverExposesPassword() throws Exception {
        Member saved = new Member();
        saved.setId(1L);
        saved.setName("Veera");
        saved.setEmail("veera@gmail.com");
        saved.setPassword("123456");
        when(memberService.registerMember(any(RegisterMemberRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/members/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Veera", "email": "veera@gmail.com", "password": "123456"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.creditBalance").value(5.0))
                .andExpect(jsonPath("$.password").doesNotExist()); // Rule 13
    }

    @Test
    void invalidRegistrationIsRejectedBeforeReachingService() throws Exception {
        mockMvc.perform(post("/api/members/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "email": "not-an-email", "password": "123456"}
                                """))
                .andExpect(status().isBadRequest());

        verify(memberService, never()).registerMember(any());
    }

    @Test
    void balanceEndpointReturnsNumber() throws Exception {
        when(memberService.getBalance(1L)).thenReturn(5.0);

        mockMvc.perform(get("/api/members/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.creditBalance").value(5.0));
    }

    @Test
    void confirmPassesPathIdAndBodyToService() throws Exception {
        SessionRequest confirmed = new SessionRequest();
        confirmed.setId(7L);
        confirmed.setStatus(SessionStatus.CONFIRMED);
        confirmed.setActualHoursDelivered(2.0);
        when(sessionRequestService.confirmSession(eq(7L), any(ConfirmSessionRequest.class))).thenReturn(confirmed);

        mockMvc.perform(put("/api/sessions/7/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actualHoursDelivered": 2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.actualHoursDelivered").value(2.0));
    }

    @Test
    void providerEndpointUsesStatusQueryParameter() throws Exception {
        when(sessionRequestService.getSessionsByProviderAndStatus(1L, SessionStatus.PENDING)).thenReturn(List.of());

        mockMvc.perform(get("/api/sessions/provider/1").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(sessionRequestService).getSessionsByProviderAndStatus(1L, SessionStatus.PENDING);
    }
}
