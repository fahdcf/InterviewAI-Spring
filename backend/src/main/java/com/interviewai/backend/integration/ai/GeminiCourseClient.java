package com.interviewai.backend.integration.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class GeminiCourseClient {

    private final ObjectMapper json;
    private final URI endpoint;
    private final String apiKey;
    private final ProviderHttpClient http;

    public GeminiCourseClient(ObjectMapper json, @Value("${interviewai.gemini.url}") URI endpoint,
            @Value("${interviewai.gemini.key}") String apiKey, ProviderHttpClient http) {
        this.json = json;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.http = http;
    }

    public JsonNode generate(String prompt) throws InterruptedException {
        if (apiKey.isBlank()) {
            throw new CourseGenerationException("Set GEMINI_API_KEY on the backend to generate courses.");
        }
        try {
            String body = json.writeValueAsString(Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("responseMimeType", "application/json")));
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request);
            int status = response.statusCode();
            if (status == 429) {
                throw new CourseGenerationException("The course provider is rate limited. Try again later.");
            }
            if (status != 200) {
                throw new CourseGenerationException("The course provider returned HTTP " + status + ".");
            }
            return readContent(json.readTree(response.body()));
        } catch (IOException error) {
            throw new CourseGenerationException("The course provider did not return readable content.");
        }
    }

    private JsonNode readContent(JsonNode response) throws IOException {
        if (response == null) {
            throw new CourseGenerationException("The course provider returned an empty answer.");
        }
        JsonNode candidate = response.path("candidates").path(0);
        if (!"STOP".equals(candidate.path("finishReason").asText())) {
            throw new CourseGenerationException("The course provider did not finish generating its answer.");
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : candidate.path("content").path("parts")) {
            if (!part.path("thought").asBoolean()) {
                text.append(part.path("text").asText());
            }
        }
        String content = text.toString().strip();
        if (content.startsWith("```") && content.endsWith("```") && content.indexOf('\n') >= 0) {
            content = content.substring(content.indexOf('\n') + 1, content.length() - 3).strip();
        }
        JsonNode result = json.readTree(content);
        if (result == null || !result.isObject()) {
            throw new CourseGenerationException("The course provider returned an empty or invalid answer.");
        }
        return result;
    }
}
