package com.interviewai.backend.integration.ai;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class ProviderHttpClient {

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException transientFailure) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        if (response.statusCode() != 429 && response.statusCode() < 500) {
            return response;
        }
        long delay = retryDelayMillis(response);
        // Leave a long provider cooldown to an explicit user retry so the worker remains available.
        if (delay > 10_000) {
            return response;
        }
        Thread.sleep(delay);
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private long retryDelayMillis(HttpResponse<?> response) {
        String value = response.headers().firstValue("Retry-After").orElse("");
        try {
            return Math.multiplyExact(Math.max(0, Long.parseLong(value)), 1000);
        } catch (NumberFormatException invalidSeconds) {
            try {
                Instant next = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
                return Math.max(0, Duration.between(Instant.now(), next).toMillis());
            } catch (DateTimeParseException invalidDate) {
                return 1500;
            }
        } catch (ArithmeticException tooLong) {
            return Long.MAX_VALUE;
        }
    }
}
