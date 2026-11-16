package com.interviewai.backend.dto.response;

import java.util.List;

public record AdminPage<T>(List<T> items, int page, int total) {
}
