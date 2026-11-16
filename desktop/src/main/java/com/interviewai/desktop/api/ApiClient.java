package com.interviewai.desktop.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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
        JsonNode json = getJson(path);
        return json == null || json.isMissingNode() ? null : objectMapper.treeToValue(json, responseType);
    }

    public <T> T writeJson(String method, String path, Object body, Class<T> responseType)
            throws IOException, InterruptedException {
        HttpRequest request = writeRequest(path)
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        String response = send(request);
        if (responseType == Void.class) {
            return null;
        }
        return objectMapper.readValue(response, responseType);
    }

    public <T> T uploadPreferences(String path, Object preferences, Path cv, Class<T> responseType)
            throws IOException, InterruptedException {
        String boundary = "interviewai-" + UUID.randomUUID();
        List<byte[]> parts = new ArrayList<>();
        parts.add(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"preferences\"\r\n"
                + "Content-Type: application/json\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        parts.add(objectMapper.writeValueAsBytes(preferences));
        if (cv != null) {
            String name = cv.getFileName().toString().toLowerCase(Locale.ROOT);
            int dot = name.lastIndexOf('.');
            String extension = dot < 0 ? "" : name.substring(dot);
            if (!List.of(".pdf", ".docx", ".doc").contains(extension)) {
                throw new ApiException(400, "Choose a PDF, DOCX, or DOC file.");
            }
            byte[] bytes;
            int maxBytes = 5 * 1024 * 1024;
            try (var input = Files.newInputStream(cv)) {
                bytes = input.readNBytes(maxBytes + 1);
            }
            if (bytes.length > maxBytes) {
                throw new ApiException(413, "Choose a CV that is 5 MB or smaller.");
            }
            parts.add(("\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"cv\"; filename=\"cv"
                    + extension + "\"\r\nContent-Type: application/octet-stream\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            parts.add(bytes);
        }
        parts.add(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest request = writeRequest(path)
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArrays(parts))
                .build();
        return objectMapper.readValue(send(request), responseType);
    }

    private HttpRequest.Builder writeRequest(String path) throws IOException, InterruptedException {
        JsonNode csrf = getJson("/api/v1/auth/csrf");
        String csrfHeader = csrf.path("headerName").asText();
        String csrfToken = csrf.path("token").asText();
        if (csrfHeader.isBlank() || csrfToken.isBlank()) {
            throw new IOException("The backend did not return a request token.");
        }
        return HttpRequest.newBuilder(baseUrl.resolve(path))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .header(csrfHeader, csrfToken);
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
