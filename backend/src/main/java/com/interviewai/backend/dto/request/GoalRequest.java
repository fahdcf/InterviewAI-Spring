package com.interviewai.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record GoalRequest(@NotBlank @Size(max = 255) String name,
                          @NotBlank @Pattern(regexp = "QUESTIONS|XP|CHAPTERS|TIME|COURSES|STREAK") String type,
                          @Positive int target) {
}
