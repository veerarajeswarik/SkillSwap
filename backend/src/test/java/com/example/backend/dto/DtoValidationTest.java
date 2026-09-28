package com.example.backend.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain unit tests for the DTO validation rules.
 * No Spring context and no database: we call the Validator directly,
 * which is exactly what @Valid will do for us inside a controller.
 */
class DtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void validRegistrationHasNoErrors() {
        RegisterMemberRequest request = new RegisterMemberRequest();
        request.setName("Veera");
        request.setEmail("veera@gmail.com");
        request.setPassword("123456");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void blankNameAndBadEmailAreRejected() {
        RegisterMemberRequest request = new RegisterMemberRequest();
        request.setName("   ");
        request.setEmail("not-an-email");
        request.setPassword("123456");

        Set<ConstraintViolation<RegisterMemberRequest>> errors = validator.validate(request);

        assertEquals(2, errors.size());
        printErrors(errors);
    }

    @Test
    void skillOfferNeedsPositiveHours() {
        SkillOfferRequest request = new SkillOfferRequest();
        request.setProviderId(1L);
        request.setSkillName("Java");
        request.setAvailableHours(0.0);

        Set<ConstraintViolation<SkillOfferRequest>> errors = validator.validate(request);

        assertEquals(1, errors.size());
        assertEquals("availableHours", errors.iterator().next().getPropertyPath().toString());
    }

    @Test
    void sessionRequestRejectsMissingIdsAndNegativeHours() {
        SessionRequestDto request = new SessionRequestDto();
        request.setRequestedHours(-2.0);

        Set<ConstraintViolation<SessionRequestDto>> errors = validator.validate(request);

        // requesterId null + skillOfferId null + requestedHours negative
        assertEquals(3, errors.size());
        printErrors(errors);
    }

    @Test
    void confirmRequiresHours() {
        ConfirmSessionRequest request = new ConfirmSessionRequest();

        assertEquals(1, validator.validate(request).size());

        request.setActualHoursDelivered(2.0);
        assertTrue(validator.validate(request).isEmpty());
    }

    private static <T> void printErrors(Set<ConstraintViolation<T>> errors) {
        for (ConstraintViolation<T> error : errors) {
            System.out.println("  " + error.getPropertyPath() + ": " + error.getMessage());
        }
    }
}
