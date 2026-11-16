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

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Backend returned HTTP " + response.statusCode() + ".");
        }

        return objectMapper.readTree(response.body());
    }
}
