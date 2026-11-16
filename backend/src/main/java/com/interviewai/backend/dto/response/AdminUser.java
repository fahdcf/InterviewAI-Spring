package com.interviewai.backend.dto.response;

import java.util.List;

public record AdminUser(int id, String username, String email, boolean active, long xp,
                        int courseCount, String joined, List<String> roles) {
}
