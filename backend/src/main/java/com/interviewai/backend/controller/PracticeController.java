package com.interviewai.backend.controller;

import com.interviewai.backend.dto.request.AttemptRequest;
import com.interviewai.backend.dto.response.AttemptResponse;
import com.interviewai.backend.dto.response.PracticeQuestionResponse;
import com.interviewai.backend.security.AccountPrincipal;
import com.interviewai.backend.service.AccountService;
import com.interviewai.backend.service.PracticeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PracticeController {

    private final AccountService accounts;
    private final PracticeService practice;

    public PracticeController(AccountService accounts, PracticeService practice) {
        this.accounts = accounts;
        this.practice = practice;
    }

    @GetMapping("/chapters/{id}/questions")
    public List<PracticeQuestionResponse> lesson(@AuthenticationPrincipal AccountPrincipal account, @PathVariable int id) {
        return practice.questions(activeUserId(account), id, false);
    }

    @GetMapping("/chapters/{id}/review")
    public List<PracticeQuestionResponse> chapterReview(@AuthenticationPrincipal AccountPrincipal account,
            @PathVariable int id) {
        return practice.questions(activeUserId(account), id, true);
    }

    @GetMapping("/courses/{id}/review")
    public List<PracticeQuestionResponse> courseReview(@AuthenticationPrincipal AccountPrincipal account,
            @PathVariable int id) {
        return practice.courseReview(activeUserId(account), id);
    }

    @PostMapping("/questions/{id}/attempts")
    public ResponseEntity<AttemptResponse> submit(@AuthenticationPrincipal AccountPrincipal account,
            @PathVariable int id, @Valid @RequestBody AttemptRequest request) {
        AttemptResponse result = practice.submit(activeUserId(account), id, request);
        if ("QUEUED".equals(result.status()) || "RUNNING".equals(result.status())) {
            return ResponseEntity.accepted().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/attempts/{id}")
    public AttemptResponse attempt(@AuthenticationPrincipal AccountPrincipal account, @PathVariable long id) {
        return practice.attempt(activeUserId(account), id);
    }

    private int activeUserId(AccountPrincipal account) {
        accounts.getActiveAccount(account.getUserId());
        return account.getUserId();
    }
}
