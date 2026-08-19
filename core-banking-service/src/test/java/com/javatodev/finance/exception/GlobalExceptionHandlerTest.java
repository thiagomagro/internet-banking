package com.javatodev.finance.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void documentsExceptionConstructorArgumentOrderInErrorResponse() {
        ResponseEntity<?> response = new GlobalExceptionHandler()
            .handleGlobalException(new EntityNotFoundException(), java.util.Locale.ENGLISH);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body.getCode()).isEqualTo("Requested entity not present in the DB.");
        assertThat(body.getMessage()).isEqualTo(GlobalErrorCode.ERROR_ENTITY_NOT_FOUND);
    }

    @Test
    void documentsInsufficientFundsCodeAndMessageAreReversedToday() {
        ResponseEntity<?> response = new GlobalExceptionHandler()
            .handleGlobalException(new InsufficientFundsException("no funds", GlobalErrorCode.INSUFFICIENT_FUNDS),
                java.util.Locale.ENGLISH);

        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body.getCode()).isEqualTo("no funds");
        assertThat(body.getMessage()).isEqualTo(GlobalErrorCode.INSUFFICIENT_FUNDS);
    }
}
