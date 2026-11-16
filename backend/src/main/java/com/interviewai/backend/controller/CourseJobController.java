package com.interviewai.backend.controller;

import com.interviewai.backend.model.CourseJob;
import com.interviewai.backend.security.AccountPrincipal;
import com.interviewai.backend.service.AccountService;
import com.interviewai.backend.service.CourseJobService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/course-jobs")
public class CourseJobController {

    private final AccountService accounts;
    private final CourseJobService jobs;

    public CourseJobController(AccountService accounts, CourseJobService jobs) {
        this.accounts = accounts;
        this.jobs = jobs;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CourseJob start(@AuthenticationPrincipal AccountPrincipal account, @Valid @RequestBody JobRequest request) {
        return jobs.start(activeUserId(account), request.getRequestId().toString(), null);
    }

    @PostMapping("/{id}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CourseJob retry(@AuthenticationPrincipal AccountPrincipal account, @PathVariable long id,
            @Valid @RequestBody JobRequest request) {
        return jobs.start(activeUserId(account), request.getRequestId().toString(), id);
    }

    @GetMapping("/latest")
    public ResponseEntity<CourseJob> latest(@AuthenticationPrincipal AccountPrincipal account) {
        return jobs.latest(activeUserId(account)).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public CourseJob get(@AuthenticationPrincipal AccountPrincipal account, @PathVariable long id) {
        return jobs.get(activeUserId(account), id);
    }

    private int activeUserId(AccountPrincipal account) {
        accounts.getActiveAccount(account.getUserId());
        return account.getUserId();
    }

    public static class JobRequest {
        @NotNull
        private UUID requestId;

        public UUID getRequestId() { return requestId; }
        public void setRequestId(UUID requestId) { this.requestId = requestId; }
    }
}
