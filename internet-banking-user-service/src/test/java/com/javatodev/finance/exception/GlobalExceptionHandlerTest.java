package com.javatodev.finance.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void returnsTheGlobalErrorCodeAndMessageInTheExpectedFields() {
        ResponseEntity<?> response = new GlobalExceptionHandler()
            .handleGlobalException(
                new EntityNotFoundException("missing user"),
                java.util.Locale.ENGLISH
            );

        ErrorResponse body = (ErrorResponse) response.getBody();

        assertThat(body.getCode()).isEqualTo(GlobalErrorCode.ERROR_ENTITY_NOT_FOUND);
        assertThat(body.getMessage()).isEqualTo("missing user");
    }

    @Test
    void preservesCodeAndMessageForOtherUserErrors() {
        ResponseEntity<?> response = new GlobalExceptionHandler()
            .handleGlobalException(
                new InvalidEmailException("invalid email", GlobalErrorCode.ERROR_INVALID_EMAIL),
                java.util.Locale.ENGLISH
            );

        ErrorResponse body = (ErrorResponse) response.getBody();

        assertThat(body.getCode()).isEqualTo(GlobalErrorCode.ERROR_INVALID_EMAIL);
        assertThat(body.getMessage()).isEqualTo("invalid email");
    }
}
