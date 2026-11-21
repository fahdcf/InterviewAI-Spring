package com.interviewai.backend.controller;

import java.net.URI;
import java.util.Locale;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext // Shut down the background workers before another suite uses the test database.
class HealthControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RequestMappingHandlerMapping routes;

    @Test
    void openApiDescribesEveryControllerRouteAndSecurityBoundary() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:" + port + "/v3/api-docs")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body());
            JsonNode api = objectMapper.readTree(response.body());
            JsonNode paths = api.path("paths");
            routes.getHandlerMethods().forEach((mapping, handler) -> {
                for (String path : mapping.getPatternValues()) {
                    if (path.startsWith("/api/v1/")) {
                        mapping.getMethodsCondition().getMethods().forEach(method ->
                                assertTrue(paths.path(path).has(method.name().toLowerCase(Locale.ROOT)),
                                        "Missing route " + method + " " + path));
                    }
                }
            });
            assertTrue(paths.path("/api/v1/auth/logout").path("post").path("responses").has("204"));
            assertTrue(paths.path("/api/v1/questions/{id}/attempts").path("post").path("responses").has("202"));
            assertTrue(paths.path("/api/v1/me/onboarding").path("post").path("requestBody")
                    .path("content").has("multipart/form-data"));
            JsonNode security = paths.path("/api/v1/goals/{id}").path("patch").path("security").get(0);
            assertTrue(security.has("session"));
            assertTrue(security.has("csrf"));
            assertTrue(paths.path("/api/v1/health").path("get").path("security").isEmpty());
            assertFalse(response.body().contains("password_hash"));
            assertTrue(api.path("components").path("schemas").path("CourseJob").path("properties")
                    .path("status").path("enum").toString().contains("INTERRUPTED"));
        }
    }

    @Test
    void healthEndpointReportsThatTheBackendIsUp() throws Exception {
        URI url = URI.create("http://127.0.0.1:" + port + "/api/v1/health");
        HttpRequest request = HttpRequest.newBuilder(url).GET().build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertEquals("UP", objectMapper.readTree(response.body()).path("status").asText());
        }
    }
}
