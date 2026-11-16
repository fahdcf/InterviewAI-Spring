package com.interviewai.backend.dto.response;

public record GoalResponse(int id, String name, String type, int target, int startValue, long currentValue) {
}
