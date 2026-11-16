package com.interviewai.desktop.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiClient {

    private final URI baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiClient(URI baseUrl, HttpClient httpClient) {
        String scheme = baseUrl.getScheme();
        String path = baseUrl.getPath();
        if ((!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                || baseUrl.getHost() == null
                || baseUrl.getUserInfo() != null
                || baseUrl.getQuery() != null
                || baseUrl.getFragment() != null
                || (!path.isEmpty() && !"/".equals(path))) {
            throw new IllegalArgumentException(
                    "Enter a backend address such as http://localhost:8080.");
        }
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
    }

    public JsonNode getJson(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(baseUrl.resolve(path))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .GET()
                .build();

        return objectMapper.readTree(send(request));
    }

    public <T> T readJson(String path, Class<T> responseType) throws IOException, InterruptedException {
        return objectMapper.treeToValue(getJson(path), responseType);
    }

    public <T> T writeJson(String method, String path, Object body, Class<T> responseType)
            throws IOException, InterruptedException {
        JsonNode csrf = getJson("/api/v1/auth/csrf");
        String csrfHeader = csrf.path("headerName").asText();
        String csrfToken = csrf.path("token").asText();
        if (csrfHeader.isBlank() || csrfToken.isBlank()) {
            throw new IOException("The backend did not return a request token.");
        }
        String requestBody = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder(baseUrl.resolve(path))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header(csrfHeader, csrfToken)
                .method(method, HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        String response = send(request);
        if (responseType == Void.class) {
            return null;
        }
        return objectMapper.readValue(response, responseType);
    }

    private String send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return response.body();
        }
        String message = "Backend returned HTTP " + status + ".";
        if (status >= 400 && status < 500) {
            try {
                JsonNode details = objectMapper.readTree(response.body());
                if (details != null) {
                    message = details.path("message").asText(message);
                }
            } catch (IOException ignored) {
                // A proxy can return an HTML error page.
            }
        }
        throw new ApiException(status, message);
    }
}
