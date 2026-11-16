package com.interviewai.backend.controller;

import com.interviewai.backend.dto.request.OnboardingRequest;
import com.interviewai.backend.dto.response.OnboardingResponse;
import com.interviewai.backend.integration.document.CvTextExtractor;
import com.interviewai.backend.security.AccountPrincipal;
import com.interviewai.backend.service.AccountService;
import com.interviewai.backend.service.OnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/me/onboarding")
public class OnboardingController {

    private final AccountService accounts;
    private final OnboardingService onboarding;
    private final CvTextExtractor cvExtractor;

    public OnboardingController(AccountService accounts, OnboardingService onboarding, CvTextExtractor cvExtractor) {
        this.accounts = accounts;
        this.onboarding = onboarding;
        this.cvExtractor = cvExtractor;
    }

    @GetMapping
    public ResponseEntity<OnboardingResponse> preferences(@AuthenticationPrincipal AccountPrincipal account) {
        int userId = account.getUserId();
        accounts.getActiveAccount(userId);
        return onboarding.find(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OnboardingResponse save(@AuthenticationPrincipal AccountPrincipal account,
            @Valid @RequestPart("preferences") OnboardingRequest preferences,
            @RequestPart(value = "cv", required = false) MultipartFile cv) {
        int userId = account.getUserId();
        accounts.getActiveAccount(userId);
        String cvText = cv == null ? null : cvExtractor.extract(cv);
        return onboarding.save(userId, preferences, cvText);
    }
}
