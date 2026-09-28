package com.example.backend.exception;

import com.example.backend.dto.ConfirmSessionRequest;
import com.example.backend.dto.SessionRequestDto;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks that every kind of failure becomes the same clean JSON error shape.
 */
@WebMvcTest
class GlobalExceptionHandlerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MemberService memberService;
    @MockitoBean private SkillOfferService skillOfferService;
    @MockitoBean private SessionRequestService sessionRequestService;
    @MockitoBean private CreditLedgerService creditLedgerService;

    @Test
    void unknownMemberGives404() throws Exception {
        when(memberService.getMemberById(99L)).thenThrow(new ResourceNotFoundException("Member not found with id 99"));

        mockMvc.perform(get("/api/members/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Member not found with id 99"))
                .andExpect(jsonPath("$.path").value("/api/members/99"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void insufficientCreditGives400() throws Exception {
        when(sessionRequestService.createSessionRequest(any(SessionRequestDto.class)))
                .thenThrow(new InsufficientCreditException("Insufficient credits: you have 3.0 but requested 5.0 hours"));

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requesterId": 2, "skillOfferId": 1, "requestedHours": 5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient credits: you have 3.0 but requested 5.0 hours"));
    }

    @Test
    void businessRuleViolationGives409() throws Exception {
        when(sessionRequestService.confirmSession(eq(7L), any(ConfirmSessionRequest.class)))
                .thenThrow(new IllegalStateException("Only PENDING sessions can be confirmed; this one is CONFIRMED"));

        mockMvc.perform(put("/api/sessions/7/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actualHoursDelivered": 2}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void validationErrorsListEachField() throws Exception {
        mockMvc.perform(post("/api/members/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "email": "not-an-email", "password": "123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("Name is required"))
                .andExpect(jsonPath("$.errors.email").value("Email must be a valid email address"))
                .andExpect(jsonPath("$.errors.password").value("Password must be at least 6 characters"));
    }

    @Test
    void brokenJsonGives400() throws Exception {
        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
    }

    @Test
    void wrongTypeInUrlGives400() throws Exception {
        mockMvc.perform(get("/api/members/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'"));
    }

    @Test
    void wrongHttpMethodKeeps405() throws Exception {
        mockMvc.perform(delete("/api/members/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void unexpectedBugGives500WithoutLeakingDetails() throws Exception {
        when(memberService.getAllMembers()).thenThrow(new RuntimeException("secret internal detail"));

        mockMvc.perform(get("/api/members"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
