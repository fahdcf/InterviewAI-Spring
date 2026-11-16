package com.interviewai.backend.service;

import com.interviewai.backend.model.CourseJob;
import com.interviewai.backend.repository.CourseJobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class CourseJobService {

    private final CourseJobRepository jobs;
    private final AccountService accounts;

    public CourseJobService(CourseJobRepository jobs, AccountService accounts) {
        this.jobs = jobs;
        this.accounts = accounts;
    }

    @Transactional
    public CourseJob start(int userId, String requestId, Long retryJobId) {
        accounts.lockActiveAccount(userId);
        Optional<CourseJob> existing = jobs.findByRequest(userId, requestId);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (retryJobId != null) {
            String status = get(userId, retryJobId).getStatus();
            if (!"FAILED".equals(status) && !"INTERRUPTED".equals(status)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a stopped generation can be retried.");
            }
        }
        Optional<CourseJob> running = jobs.findActive(userId);
        if (running.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A course is already being generated. Open Generation status from the dashboard.");
        }
        if (!jobs.queue(userId, requestId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Save interview preferences first.");
        }
        return jobs.findByRequest(userId, requestId).orElseThrow();
    }

    public CourseJob get(int userId, long jobId) {
        return jobs.find(userId, jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Generation job unavailable."));
    }

    public Optional<CourseJob> latest(int userId) {
        return jobs.findLatest(userId);
    }
}
