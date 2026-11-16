package com.interviewai.backend.integration;

import com.interviewai.backend.integration.ai.CourseGenerator;
import com.interviewai.backend.integration.ai.OpenRouterGradingClient;
import com.interviewai.backend.integration.document.CvTextExtractor;
import com.interviewai.backend.model.AnswerGrade;
import com.interviewai.backend.model.CourseInput;
import com.interviewai.backend.model.GeneratedCourse;
import com.interviewai.backend.model.PracticeQuestion;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "interviewai.provider.acceptance", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext
class ProviderAcceptanceTest {

    @Autowired private CourseGenerator courses;
    @Autowired private OpenRouterGradingClient grading;
    @Autowired private CvTextExtractor cvExtractor;
    @Value("${interviewai.gemini.url}") private URI geminiUrl;
    @Value("${interviewai.openrouter.model}") private String gradingModel;

    @Test
    @Timeout(value = 20, unit = TimeUnit.MINUTES)
    void geminiGeneratesBothCoursesFromSyntheticCvText() throws Exception {
        String resume = "Synthetic candidate: Java programmer with experience in REST and SQL.";
        String cvText = cvExtractor.extract(syntheticPdf(resume));
        assertEquals(resume, cvText);
        CourseInput input = new CourseInput("JOB", "ENGLISH", "LATER", "Junior Java developer",
                "Practice explaining work on a small Spring Boot application.", cvText);
        Instant start = Instant.now();
        String path = geminiUrl.getPath();
        System.out.println("Gemini endpoint path: " + path);
        GeneratedCourse technical = courses.generate(input, true, step -> {});
        GeneratedCourse softSkills = courses.generate(input, false, step -> {});
        assertCompleteCourse(technical);
        assertCompleteCourse(softSkills);
        System.out.println("Gemini accepted both courses after " + Duration.between(start, Instant.now()).toSeconds()
                + " seconds.");
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void openRouterGradesAndExplainsSyntheticAnswers() throws Exception {
        PracticeQuestion question = new PracticeQuestion(1, 1, 1, "What does HTTP 404 mean?",
                "SHORT_ANSWER", "NOT_STARTED", "The server could not find the requested resource.", "");
        String answer = "The requested resource was not found.";
        System.out.println("OpenRouter model: " + gradingModel);
        Instant start = Instant.now();
        AnswerGrade grade = grading.grade(question, answer);
        assertTrue(grade.score() >= 0 && grade.score() <= 100);
        assertFalse(grade.explanation().isBlank());
        assertFalse(grading.explain(question, answer).isBlank());
        System.out.println("OpenRouter returned a score and explanation after "
                + Duration.between(start, Instant.now()).toSeconds() + " seconds.");
    }

    private void assertCompleteCourse(GeneratedCourse course) {
        assertFalse(course.getTitle().isBlank());
        assertEquals(12, course.getChapters().size());
        course.getChapters().forEach(chapter -> assertEquals(40, chapter.getQuestions().size()));
    }

    private MockMultipartFile syntheticPdf(String text) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 720);
                content.showText(text);
                content.endText();
            }
            document.save(data);
        }
        return new MockMultipartFile("cv", "synthetic-cv.pdf", "application/pdf", data.toByteArray());
    }
}
