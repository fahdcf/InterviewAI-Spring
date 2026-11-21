package com.interviewai.backend.integration.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.backend.model.PracticeQuestion;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderHttpClientTest {

    @Test
    void retriesTransientResponsesOnceAndRespectsLongCooldowns() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/", exchange -> {
            try (exchange) {
                int count = calls.incrementAndGet();
                int status = exchange.getRequestURI().getPath().equals("/cooldown") ? 429 : count == 1 ? 503 : 200;
                exchange.getResponseHeaders().set("Retry-After", status == 429 ? "999" : "0");
                byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
        try {
            URI origin = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            ProviderHttpClient http = new ProviderHttpClient();
            assertEquals(200, http.send(HttpRequest.newBuilder(origin).GET().build()).statusCode());
            assertEquals(2, calls.get());
            assertEquals(429, http.send(HttpRequest.newBuilder(origin.resolve("/cooldown")).GET().build()).statusCode());
            assertEquals(3, calls.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void providerParsersHandleFencesAndRejectIncompleteOrInvalidData() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ObjectMapper json = new ObjectMapper();
        server.createContext("/", exchange -> {
            try (exchange) {
                String path = exchange.getRequestURI().getPath();
                String content;
                if (path.equals("/gemini")) {
                    content = json.writeValueAsString(Map.of("candidates", java.util.List.of(Map.of(
                            "finishReason", "STOP", "content", Map.of("parts", java.util.List.of(
                                    Map.of("text", "```json\n{\"chapters\": []}\n```")))))));
                } else if (path.equals("/partial")) {
                    content = json.writeValueAsString(Map.of("candidates",
                            java.util.List.of(Map.of("finishReason", "MAX_TOKENS"))));
                } else {
                    int score = path.equals("/invalid") ? 101 : 70;
                    String grade = json.writeValueAsString(Map.of("score", score,
                            "strengths", "Clear answer.", "improvements", "Add an example.", "explanation", "The concept."));
                    content = json.writeValueAsString(Map.of("choices", java.util.List.of(
                            Map.of("message", Map.of("content", "```json\n" + grade + "\n```")))));
                }
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
        try {
            URI origin = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            ProviderHttpClient http = new ProviderHttpClient();
            GeminiCourseClient gemini = new GeminiCourseClient(json, origin.resolve("/gemini"), "fixture", http);
            assertTrue(gemini.generate("Synthetic prompt").path("chapters").isArray());
            GeminiCourseClient partial = new GeminiCourseClient(json, origin.resolve("/partial"), "fixture", http);
            assertThrows(CourseGenerationException.class, () -> partial.generate("Synthetic prompt"));
            PracticeQuestion question = new PracticeQuestion(1, 1, 1, "Question?", "SHORT_ANSWER",
                    "NOT_STARTED", "Answer", "");
            OpenRouterGradingClient grader = new OpenRouterGradingClient(json, origin.resolve("/grade"), "fixture", "fixture", http);
            assertEquals(70, grader.grade(question, "Answer").score());
            OpenRouterGradingClient invalid = new OpenRouterGradingClient(json, origin.resolve("/invalid"), "fixture", "fixture", http);
            assertThrows(IOException.class, () -> invalid.grade(question, "Answer"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void requestTimeoutIsBoundedAndRetriesOnlyOnce() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        server.start();
        try {
            URI url = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            HttpRequest request = HttpRequest.newBuilder(url).timeout(Duration.ofMillis(100)).GET().build();
            assertThrows(IOException.class, () -> new ProviderHttpClient().send(request));
        } finally {
            server.stop(0);
        }
    }
}
