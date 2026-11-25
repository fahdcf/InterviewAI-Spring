package com.interviewai.backend.repository;

import java.time.LocalDate;

public interface DailyCount {
    LocalDate getDay();
    long getTotal();
}
