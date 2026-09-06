package com.acme.claims.controller;

import com.acme.claims.domain.ClaimNotFoundException;
import com.acme.claims.domain.IdempotencyConflictException;
import com.acme.claims.domain.InvalidClaimTransitionException;
import com.acme.claims.domain.ClaimStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void claimNotFoundMapsTo404() {
        ResponseEntity<?> response = handler.notFound(new ClaimNotFoundException("CLM-X"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void invalidTransitionAndIdempotencyConflictsMapTo409() {
        assertThat(handler.conflict(new InvalidClaimTransitionException(ClaimStatus.FNOL, ClaimStatus.CLOSED))
            .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(handler.conflict(new IdempotencyConflictException("key")).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void optimisticLockMapsTo409() {
        assertThat(handler.concurrentModification(new ObjectOptimisticLockingFailureException("Claim", 1L))
            .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void validationErrorsMapTo400() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "policyNumber", "must not be blank"));
        bindingResult.addError(new FieldError("request", "claimType", "must not be null"));
        MethodArgumentNotValidException exception =
            new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<?> response = handler.validation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().toString()).contains("policyNumber").contains("claimType");
    }

    @Test
    void unreadableBodyMapsTo400() {
        ResponseEntity<?> response = handler.badRequest(new HttpMessageNotReadableException("bad json"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
