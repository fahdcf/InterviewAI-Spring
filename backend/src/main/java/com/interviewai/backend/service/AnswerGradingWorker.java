package com.interviewai.backend.service;

import com.interviewai.backend.integration.ai.OpenRouterGradingClient;
import com.interviewai.backend.model.PracticeAttempt;
import com.interviewai.backend.model.PracticeQuestion;
import com.interviewai.backend.repository.PracticeRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AnswerGradingWorker {

    private static final Logger LOG = LoggerFactory.getLogger(AnswerGradingWorker.class);
    private final PracticeRepository attempts;
    private final PracticeService practice;
    private final OpenRouterGradingClient grader;

    public AnswerGradingWorker(PracticeRepository attempts, PracticeService practice, OpenRouterGradingClient grader) {
        this.attempts = attempts;
        this.practice = practice;
        this.grader = grader;
    }

    @PostConstruct
    void recover() {
        attempts.recoverInterruptedAttempts();
    }

    @Scheduled(fixedDelay = 750)
    public void processNextAttempt() {
        attempts.findFirstByStatusOrderByIdAsc("QUEUED").ifPresent(this::grade);
    }

    private void grade(PracticeAttempt attempt) {
        if (attempts.start(attempt.id()) != 1) {
            return;
        }
        try {
            PracticeQuestion question = practice.findQuestion(attempt.userId(), attempt.questionId());
            practice.completeGrading(attempt, grader.grade(question, attempt.answer()));
        } catch (IOException error) {
            attempts.fail(attempt.id(), "Grading is unavailable. Check the backend provider and try again.");
        } catch (InterruptedException error) {
            attempts.fail(attempt.id(), "Grading was interrupted. Try again.");
            Thread.currentThread().interrupt();
        } catch (RuntimeException error) {
            LOG.error("Saving answer attempt {} failed ({})", attempt.id(), error.getClass().getSimpleName());
            attempts.fail(attempt.id(), "Could not save feedback. Try again.");
        }
    }
}
