package com.interviewai.backend.service;

import com.interviewai.backend.dto.request.OnboardingRequest;
import com.interviewai.backend.dto.response.OnboardingResponse;
import com.interviewai.backend.repository.OnboardingCvRepository;
import com.interviewai.backend.repository.OnboardingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class OnboardingService {

    private final OnboardingRepository repository;
    private final OnboardingCvRepository cvs;

    public OnboardingService(OnboardingRepository repository, OnboardingCvRepository cvs) {
        this.repository = repository;
        this.cvs = cvs;
    }

    public Optional<OnboardingResponse> find(int userId) {
        return repository.find(userId);
    }

    @Transactional
    public OnboardingResponse save(int userId, OnboardingRequest request, String cvText) {
        repository.savePreferences(userId, request.getInterviewType(), request.getLanguage(),
                request.getTimeline(), request.getContext().strip(), request.getDescription());
        if (cvText != null) {
            cvs.saveCv(userId, cvText);
        } else if (request.isRemoveCv()) {
            cvs.removeCv(userId);
        }
        return repository.find(userId).orElseThrow();
    }
}
