package com.interviewai.desktop.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthApiTest {

    private HttpServer server;
    private HttpClient httpClient;
    private HealthApi healthApi;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        httpClient = HttpClient.newHttpClient();
        URI baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        healthApi = new HealthApi(new ApiClient(baseUrl, httpClient));
    }

    @AfterEach
    void stopServer() {
        httpClient.close();
        server.stop(0);
    }

    @Test
    void typedResponsesAllowFutureOptionalFields() throws Exception {
        respondWith(200, "{\"type\":\"daily\",\"current\":3,\"target\":20,\"reward\":900,\"futureField\":true}");
        ApiClient client = new ApiClient(URI.create("http://127.0.0.1:" + server.getAddress().getPort()), httpClient);
        ProgressApi.Quest quest = client.readJson("/api/v1/health", ProgressApi.Quest.class);
        assertEquals("daily", quest.type());
        assertEquals(3, quest.current());
    }

    @Test
    void preservesActionableErrorsAndHandlesHtmlProxyErrors() throws Exception {
        int[] statuses = {401, 403, 409, 500};
        for (int status : statuses) {
            respondWith(status, "{\"message\":\"Try signing in again.\"}");
            ApiClient client = new ApiClient(URI.create("http://127.0.0.1:" + server.getAddress().getPort()), httpClient);
            ApiException error = assertThrows(ApiException.class, () -> client.getJson("/api/v1/health"));
            assertEquals(status, error.getStatus());
            server.removeContext("/api/v1/health");
        }
        respondWith(503, "<html>Unavailable</html>");
        assertThrows(ApiException.class, healthApi::isBackendUp);
    }

    @Test
    void reportsReadyWhenBackendReturnsUp() throws Exception {
        respondWith(200, "{\"status\":\"UP\"}");

        assertTrue(healthApi.isBackendUp());
    }

    @Test
    void reportsNotReadyWhenBackendReturnsDown() throws Exception {
        respondWith(200, "{\"status\":\"DOWN\"}");

        assertFalse(healthApi.isBackendUp());
    }

    @Test
    void rejectsAnHttpError() throws Exception {
        respondWith(503, "{\"status\":\"DOWN\"}");

        assertThrows(IOException.class, healthApi::isBackendUp);
    }

    @Test
    void rejectsAnInvalidJsonResponse() throws Exception {
        respondWith(200, "not JSON");

        assertThrows(IOException.class, healthApi::isBackendUp);
    }

    private void respondWith(int statusCode, String responseBody) {
        server.createContext("/api/v1/health", exchange -> {
            try (exchange) {
                byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(statusCode, response.length);
                exchange.getResponseBody().write(response);
            }
        });
    }
}
