package com.interviewai.backend.dto.response;

public record QuestResponse(String type, int current, int target, int reward, boolean claimed, boolean available,
                            String resetsAt) {
}
