package com.javatodev.finance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.config.enabled=false",
    "spring.cloud.bootstrap.enabled=false",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/jwks"
})
class InternetBankingApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
