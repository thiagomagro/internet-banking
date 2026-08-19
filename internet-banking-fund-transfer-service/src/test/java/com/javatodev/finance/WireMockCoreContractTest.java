package com.javatodev.finance;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

class WireMockCoreContractTest {

    private WireMockServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void coreContractCanRepresentSuccessClientErrorServerErrorMalformedBodyAndTimeout() throws Exception {
        server = new WireMockServer(0);
        server.start();
        server.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/v1/transaction/fund-transfer"))
            .willReturn(aResponse().withStatus(200).withBody("{\"transactionId\":\"tx-1\"}")));
        server.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/insufficient"))
            .willReturn(aResponse().withStatus(400).withBody("{\"code\":\"BANKING-CORE-SERVICE-1001\"}")));
        server.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/broken"))
            .willReturn(aResponse().withStatus(500).withBody("not-json")));
        server.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/timeout"))
            .willReturn(aResponse().withFixedDelay(200).withStatus(200)));

        HttpClient client = HttpClient.newHttpClient();
        assertThat(sendPost(client, "/api/v1/transaction/fund-transfer").statusCode()).isEqualTo(200);
        assertThat(sendPost(client, "/insufficient").statusCode()).isEqualTo(400);
        assertThat(sendPost(client, "/broken").statusCode()).isEqualTo(500);
        assertThat(sendPost(client, "/timeout").statusCode()).isEqualTo(200);
        server.verify(4, postRequestedFor(anyUrl()));
    }

    private HttpResponse<String> sendPost(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(server.baseUrl() + path))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build(), HttpResponse.BodyHandlers.ofString());
    }
}
