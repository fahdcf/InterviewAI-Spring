package com.interviewai.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class ApiDocumentationConfig {

    @Bean
    OpenAPI apiDescription() {
        return new OpenAPI().info(new Info().title("InterviewAI API").version("v1")
                .description("""
                        JavaFX calls this API using a session cookie. Fetch /api/v1/auth/csrf before each write,
                        send its token in the returned header, and retain cookies after sign-in.
                        Course jobs use QUEUED, RUNNING, SUCCEEDED, FAILED and INTERRUPTED states.
                        Attempts use QUEUED, RUNNING, GRADED and FAILED states. Poll until a terminal state.
                        Reuse the same requestId when retrying an operation after a lost response.
                        Admin lists use zero-based pages of 20 items ordered by descending ID.
                        Resource IDs are restricted to the signed-in owner or an authorized administrator.
                        """))
                .components(new Components()
                        .addSecuritySchemes("session", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE).name("JSESSIONID"))
                        .addSecuritySchemes("csrf", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER).name("X-CSRF-TOKEN"))
                        .addSchemas("ApiError", new ObjectSchema()
                                .addProperty("message", new StringSchema())
                                .required(List.of("message"))));
    }

    @Bean
    OpenApiCustomizer apiBehavior() {
        return api -> {
            api.getPaths().addPathItem("/api/v1/auth/logout", new PathItem().post(new Operation()
                    .operationId("logout").summary("End the current session")
                    .responses(new io.swagger.v3.oas.models.responses.ApiResponses()
                            .addApiResponse("204", new ApiResponse().description("Signed out")))));
            for (Map.Entry<String, PathItem> path : api.getPaths().entrySet()) {
                boolean anonymous = path.getKey().equals("/api/v1/health")
                        || path.getKey().equals("/api/v1/auth/csrf")
                        || path.getKey().equals("/api/v1/auth/login")
                        || path.getKey().equals("/api/v1/auth/register");
                for (Map.Entry<PathItem.HttpMethod, Operation> entry : path.getValue().readOperationsMap().entrySet()) {
                    Operation operation = entry.getValue();
                    SecurityRequirement credentials = new SecurityRequirement();
                    if (!anonymous) {
                        credentials.addList("session");
                    }
                    if (entry.getKey() != PathItem.HttpMethod.GET) {
                        credentials.addList("csrf");
                        operation.getResponses().putIfAbsent("400", error("Invalid request"));
                        operation.getResponses().putIfAbsent("409", error("The operation conflicts with current state"));
                    }
                    operation.setSecurity(credentials.isEmpty() ? new ArrayList<>() : List.of(credentials));
                    operation.getResponses().putIfAbsent("401", error("Sign-in is required or credentials are invalid"));
                    operation.getResponses().putIfAbsent("403", error("Permission or CSRF token is missing"));
                    operation.getResponses().putIfAbsent("404", error("The resource is unavailable"));
                }
            }
            Operation attempt = api.getPaths().get("/api/v1/questions/{id}/attempts").getPost();
            attempt.getResponses().addApiResponse("202", response("Answer queued for grading", "AttemptResponse"));
            api.getPaths().get("/api/v1/course-jobs/latest").getGet().getResponses()
                    .addApiResponse("204", new ApiResponse().description("No job has been created"));
            api.getPaths().get("/api/v1/me/onboarding").getGet().getResponses()
                    .addApiResponse("204", new ApiResponse().description("Preferences have not been saved"));
            setStates(api, "CourseJob", List.of("QUEUED", "RUNNING", "SUCCEEDED", "FAILED", "INTERRUPTED"));
            setStates(api, "AttemptResponse", List.of("QUEUED", "RUNNING", "GRADED", "FAILED"));
        };
    }

    private void setStates(OpenAPI api, String name, List<String> states) {
        Schema<?> schema = api.getComponents().getSchemas().get(name);
        schema.getProperties().put("status", new StringSchema()._enum(states));
    }

    private ApiResponse error(String message) {
        return response(message, "ApiError");
    }

    private ApiResponse response(String message, String schema) {
        return new ApiResponse().description(message).content(new Content().addMediaType("application/json",
                new io.swagger.v3.oas.models.media.MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/" + schema))));
    }
}
