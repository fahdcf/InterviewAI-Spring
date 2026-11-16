package com.interviewai.backend.service;

import com.interviewai.backend.dto.request.OnboardingRequest;
import com.interviewai.backend.dto.response.OnboardingResponse;
import com.interviewai.backend.repository.OnboardingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class OnboardingService {

    private final OnboardingRepository repository;

    public OnboardingService(OnboardingRepository repository) {
        this.repository = repository;
    }

    public Optional<OnboardingResponse> find(int userId) {
        return repository.find(userId);
    }

    @Transactional
    public OnboardingResponse save(int userId, OnboardingRequest request, String cvText) {
        repository.save(userId, request);
        if (cvText != null) {
            repository.saveCv(userId, cvText);
        } else if (request.isRemoveCv()) {
            repository.removeCv(userId);
        }
        return repository.find(userId).orElseThrow();
    }
}
