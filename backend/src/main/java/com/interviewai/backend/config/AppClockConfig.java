package com.interviewai.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppClockConfig {

    @Bean
    Clock applicationClock(@Value("${interviewai.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
