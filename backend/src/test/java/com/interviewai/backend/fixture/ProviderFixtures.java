package com.interviewai.backend.fixture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProviderFixtures {

    public static void main(String[] args) throws IOException {
        HttpServer server = start(Integer.parseInt(args[0]));
        System.out.println("Provider fixtures listening on port " + server.getAddress().getPort());
    }

    public static HttpServer start(int port) throws IOException {
        HttpServer provider = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        provider.createContext("/generate", exchange -> {
            try (exchange) {
                ObjectMapper json = new ObjectMapper();
                String prompt = json.readTree(exchange.getRequestBody())
                        .path("contents").path(0).path("parts").path(0).path("text").asText();
                ObjectNode content = json.createObjectNode();
                ArrayNode chapters = content.putArray("chapters");
                if (prompt.contains("Generate a comprehensive interview preparation course outline")) {
                    boolean failSoftSkills = prompt.contains("FAIL_SOFT_SKILLS") && prompt.contains("Behavioural");
                    content.put("course_title", "Generated preparation");
                    for (int number = 1; number <= (failSoftSkills ? 2 : 12); number++) {
                        chapters.addObject().put("chapter_number", number)
                                .put("name", "Chapter " + number).put("description", "Practice topics.");
                    }
                } else {
                    Matcher matcher = Pattern.compile("(?m)^Chapter (\\d+):").matcher(prompt);
                    while (matcher.find()) {
                        int number = Integer.parseInt(matcher.group(1));
                        ArrayNode questions = chapters.addObject().put("chapter_number", number).putArray("questions");
                        for (int count = 1; count <= 40; count++) {
                            boolean multipleChoice = count <= 20;
                            ObjectNode question = questions.addObject().put("question", "Question " + count + "?")
                                    .put("question_type", multipleChoice ? "MULTIPLE_CHOICE" : "SHORT_ANSWER")
                                    .put("correct_answer", "Example").put("explanation", "Sample explanation.");
                            if (multipleChoice) {
                                ArrayNode choices = question.putArray("choices");
                                for (String choice : List.of("Example", "Second", "Third", "Fourth")) {
                                    choices.add(choice);
                                }
                            }
                        }
                    }
                }
                byte[] response = json.writeValueAsBytes(Map.of("candidates", List.of(Map.of(
                        "finishReason", "STOP", "content", Map.of("parts", List.of(
                                Map.of("text", json.writeValueAsString(content))))))));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
        });
        provider.createContext("/grade", exchange -> {
            try (exchange) {
                ObjectMapper json = new ObjectMapper();
                JsonNode request = json.readTree(exchange.getRequestBody());
                String userMessage = request.path("messages").path(1).path("content").asText();
                String answer = json.readTree(userMessage).path("candidateAnswer").asText();
                boolean explanation = !request.has("response_format");
                if ("unavailable".equals(answer) || (explanation && "Other".equals(answer))) {
                    exchange.sendResponseHeaders(503, -1);
                    return;
                }
                String grade = json.writeValueAsString(Map.of(
                        "score", "mostly correct".equals(answer) ? 70 : 69,
                        "strengths", "You covered the basic idea.",
                        "improvements", "You can add an example.",
                        "explanation", "Sample grade."));
                byte[] response = json.writeValueAsBytes(Map.of("choices", List.of(
                        Map.of("message", Map.of("content", explanation ? "Sample explanation." : grade)))));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
        });
        provider.start();
        return provider;
    }
}
