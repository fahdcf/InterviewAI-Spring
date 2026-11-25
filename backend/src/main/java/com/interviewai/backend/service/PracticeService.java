package com.interviewai.backend.service;

import com.interviewai.backend.dto.request.AttemptRequest;
import com.interviewai.backend.dto.response.AttemptResponse;
import com.interviewai.backend.dto.response.PracticeQuestionResponse;
import com.interviewai.backend.integration.ai.OpenRouterGradingClient;
import com.interviewai.backend.model.AnswerGrade;
import com.interviewai.backend.model.PracticeAttempt;
import com.interviewai.backend.model.PracticeQuestion;
import com.interviewai.backend.model.entity.AnswerAttempt;
import com.interviewai.backend.model.entity.ProgressEvent;
import com.interviewai.backend.repository.ChapterRepository;
import com.interviewai.backend.repository.ChoiceRepository;
import com.interviewai.backend.repository.CourseRepository;
import com.interviewai.backend.repository.PracticeRepository;
import com.interviewai.backend.repository.ProgressRepository;
import com.interviewai.backend.repository.QuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PracticeService {

    private static final int XP_PER_CORRECT_ANSWER = 10;
    private static final List<String> ANSWERED = List.of("COMPLETED", "INCORRECT");
    private final PracticeRepository practice;
    private final QuestionRepository questions;
    private final ChapterRepository chapters;
    private final ChoiceRepository choices;
    private final ProgressRepository progress;
    private final CourseRepository courses;
    private final AccountService accounts;
    private final OpenRouterGradingClient grading;

    public PracticeService(PracticeRepository practice, CourseRepository courses, AccountService accounts,
            OpenRouterGradingClient grading, QuestionRepository questions, ChapterRepository chapters,
            ChoiceRepository choices, ProgressRepository progress) {
        this.practice = practice;
        this.courses = courses;
        this.accounts = accounts;
        this.grading = grading;
        this.questions = questions;
        this.chapters = chapters;
        this.choices = choices;
        this.progress = progress;
    }

    @Transactional(readOnly = true)
    public List<PracticeQuestionResponse> questions(int userId, int chapterId, boolean review) {
        if (!chapters.ownsChapter(userId, chapterId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter unavailable.");
        }
        return responses(userId, questions.listQuestions(userId, chapterId, review));
    }

    @Transactional(readOnly = true)
    public List<PracticeQuestionResponse> courseReview(int userId, int courseId) {
        if (courses.findAvailableCourse(userId, courseId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course unavailable.");
        }
        return responses(userId, questions.listCourseReview(userId, courseId));
    }

    @Transactional
    public AttemptResponse submit(int userId, int questionId, AttemptRequest request) {
        accounts.lockActiveAccount(userId);
        PracticeQuestion question = findQuestion(userId, questionId);
        String answer = request.getAnswer() == null ? null : request.getAnswer().strip();
        if (request.isSkip()) {
            answer = null;
        } else if (answer == null || answer.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter or select an answer.");
        }

        String requestId = request.getRequestId().toString();
        PracticeAttempt previous = practice.findByUserIdAndRequestId(userId, requestId).orElse(null);
        if (previous != null) {
            if (previous.questionId() != questionId || !previous.mode().equals(request.getMode())
                    || !Objects.equals(previous.answer(), answer)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This request ID belongs to another answer.");
            }
            return AttemptResponse.from(previous);
        }
        if (practice.countByUserIdAndStatusIn(userId, List.of("QUEUED", "RUNNING")) >= 10) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wait for your pending answers before submitting more.");
        }
        if ("REVIEW".equals(request.getMode()) && !"INCORRECT".equals(question.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This question is no longer in review.");
        }
        if (question.correctAnswer() == null || question.correctAnswer().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An expected answer is missing for this question.");
        }

        boolean local = request.isSkip() || "MULTIPLE_CHOICE".equals(question.type());
        if (!request.isSkip() && local && !choices.findChoices(questionId).contains(answer)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose one of the answer options.");
        }
        long attemptId = practice.save(new AnswerAttempt(userId, questionId, requestId, request.getMode(), answer,
                local ? "RUNNING" : "QUEUED")).getId();
        PracticeAttempt attempt = practice.findByUserIdAndId(userId, attemptId).orElseThrow();
        if (local) {
            boolean correct = answer != null && answer.equalsIgnoreCase(question.correctAnswer().strip());
            String explanation = question.explanation() == null ? "" : "\n" + question.explanation();
            AnswerGrade grade = new AnswerGrade(correct ? 100 : 0, null, null,
                    "Expected answer: " + question.correctAnswer() + explanation);
            saveGrade(attempt, question, grade);
            attempt = practice.findByUserIdAndId(userId, attemptId).orElseThrow();
        }
        return AttemptResponse.from(attempt);
    }

    @Transactional
    public void completeGrading(PracticeAttempt attempt, AnswerGrade grade) {
        accounts.lockActiveAccount(attempt.userId());
        PracticeQuestion current = findQuestion(attempt.userId(), attempt.questionId());
        saveGrade(attempt, current, grade);
    }

    public AttemptResponse attempt(int userId, long id) {
        PracticeAttempt attempt = practice.findByUserIdAndId(userId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Answer unavailable."));
        findQuestion(userId, attempt.questionId());
        return AttemptResponse.from(attempt);
    }

    public PracticeQuestion findQuestion(int userId, int questionId) {
        return questions.findQuestion(userId, questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question unavailable."));
    }

    public String explain(int userId, int questionId, long attemptId) {
        PracticeQuestion question = findQuestion(userId, questionId);
        PracticeAttempt attempt = practice.findByUserIdAndId(userId, attemptId)
                .filter(saved -> saved.questionId() == questionId && "GRADED".equals(saved.status())
                        && saved.answer() != null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Graded answer unavailable."));
        if ("SHORT_ANSWER".equals(question.type()) && attempt.explanation() != null
                && !attempt.explanation().isBlank()) {
            return attempt.explanation();
        }
        try {
            return grading.explain(question, attempt.answer());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Explanation interrupted. Try again.");
        } catch (IOException unavailable) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The explanation service is unavailable. Try again.");
        }
    }

    private void saveGrade(PracticeAttempt attempt, PracticeQuestion question, AnswerGrade grade) {
        int xp = 0;
        if (grade.passed() && !"COMPLETED".equals(question.status()) && "LESSON".equals(attempt.mode())) {
            xp = XP_PER_CORRECT_ANSWER;
            progress.save(new ProgressEvent(attempt.userId(), question.courseId(), xp));
        }
        questions.updateStatus(question.id(), grade.passed() ? "COMPLETED" : "INCORRECT");
        int chapterId = question.chapterId();
        if (questions.countByChapterIdAndStatusNotIn(chapterId, ANSWERED) == 0) {
            chapters.completeChapter(chapterId);
        } else {
            chapters.startChapter(chapterId);
        }
        courses.completeIfReady(question.courseId());
        if (practice.complete(attempt.id(), grade.score(), grade.strengths(), grade.improvements(),
                grade.explanation(), xp) != 1) {
            throw new IllegalStateException("The answer attempt is no longer running.");
        }
    }

    private List<PracticeQuestionResponse> responses(int userId, List<PracticeQuestion> questions) {
        if (questions.isEmpty()) {
            return List.of();
        }
        int courseId = questions.getFirst().courseId();
        Map<Integer, List<String>> choicesByQuestion = new HashMap<>();
        for (ChoiceRepository.QuestionChoice choice : choices.findCourseChoices(courseId)) {
            choicesByQuestion.computeIfAbsent(choice.getQuestionId(), id -> new ArrayList<>()).add(choice.getText());
        }
        Map<Integer, PracticeAttempt> attempts = new HashMap<>();
        for (PracticeAttempt attempt : practice.latestAttempts(userId, courseId)) {
            attempts.put(attempt.questionId(), attempt);
        }
        List<PracticeQuestionResponse> responses = new ArrayList<>();
        for (PracticeQuestion question : questions) {
            PracticeAttempt saved = attempts.get(question.id());
            responses.add(new PracticeQuestionResponse(question.id(), question.chapterId(), question.text(),
                    question.type(), question.status(), choicesByQuestion.getOrDefault(question.id(), List.of()),
                    saved == null ? null : AttemptResponse.from(saved)));
        }
        return responses;
    }
}
