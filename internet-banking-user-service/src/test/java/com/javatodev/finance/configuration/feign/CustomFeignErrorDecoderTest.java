package com.javatodev.finance.configuration.feign;

import com.javatodev.finance.exception.GlobalErrorCode;
import com.javatodev.finance.exception.SimpleBankingGlobalException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class CustomFeignErrorDecoderTest {

    private final CustomFeignErrorDecoder decoder = new CustomFeignErrorDecoder();

    @Test
    void decodesCoreFourHundredBodyToBankingException() {
        Response response = Response.builder()
            .status(400)
            .reason("Bad Request")
            .request(Request.create(Request.HttpMethod.POST, "/transfer", Collections.emptyMap(), null,
                StandardCharsets.UTF_8, null))
            .body("{\"code\":\"" + GlobalErrorCode.ERROR_ENTITY_NOT_FOUND + "\",\"message\":\"missing\"}",
                StandardCharsets.UTF_8)
            .build();

        Exception exception = decoder.decode("transfer", response);

        assertThat(exception).isInstanceOf(SimpleBankingGlobalException.class);
        SimpleBankingGlobalException bankingException = (SimpleBankingGlobalException) exception;
        assertThat(bankingException.getCode()).isEqualTo(GlobalErrorCode.ERROR_ENTITY_NOT_FOUND);
        assertThat(bankingException.getMessage()).isEqualTo("missing");
    }

    @Test
    void translatesOtherStatusesToGenericFeignException() {
        Response response = Response.builder()
            .status(500)
            .reason("Server Error")
            .request(Request.create(Request.HttpMethod.GET, "/transfer", Collections.emptyMap(), null,
                StandardCharsets.UTF_8, null))
            .body("not-json", StandardCharsets.UTF_8)
            .build();

        assertThat(decoder.decode("transfer", response))
            .hasMessage("Common Feign Exception");
    }
}
