package com.interviewai.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InterviewAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewAiApplication.class, args);
    }
}
