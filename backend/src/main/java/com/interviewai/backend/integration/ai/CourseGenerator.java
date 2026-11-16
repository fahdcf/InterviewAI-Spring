package com.interviewai.backend.integration.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.interviewai.backend.model.CourseInput;
import com.interviewai.backend.model.GeneratedChapter;
import com.interviewai.backend.model.GeneratedCourse;
import com.interviewai.backend.model.GeneratedQuestion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

@Component
public class CourseGenerator {

    private static final int CHAPTERS = 12;
    private static final int QUESTIONS_PER_TYPE = 20;
    private final GeminiCourseClient client;
    private final CoursePrompts prompts = new CoursePrompts();

    public CourseGenerator(GeminiCourseClient client) {
        this.client = client;
    }

    public GeneratedCourse generate(CourseInput input, boolean technical, IntConsumer stepComplete)
            throws InterruptedException {
        String focus = technical ? "Technical knowledge and problem solving" : "Behavioural and soft skills";
        String context = input.getContext();
        if (input.getDescription() != null && !input.getDescription().isBlank()) {
            context += "\n" + input.getDescription();
        }
        String cv = input.getCvText() == null ? "" : input.getCvText();
        JsonNode outline = client.generate(prompts.buildChapterOutlinesPrompt(input.getInterviewType(),
                input.getLanguage(), input.getTimeline(), context, cv, focus,
                "Prepare the candidate for the interview with practical exercises."));
        String title = text(outline, "course_title", 255);
        List<ChapterOutline> chapters = readOutline(outline);
        stepComplete.accept(1);

        List<GeneratedChapter> completed = new ArrayList<>();
        for (int start = 1; start <= CHAPTERS; start += 2) {
            JsonNode answer = client.generate(prompts.buildChapterQuestionsPrompt(
                    title, chapters, start, start + 1, input.getLanguage()));
            completed.addAll(readBatch(answer, chapters, start));
            stepComplete.accept(2 + start / 2);
        }
        return new GeneratedCourse(title, completed);
    }

    private List<ChapterOutline> readOutline(JsonNode response) {
        JsonNode chapters = response.path("chapters");
        if (!chapters.isArray() || chapters.size() != CHAPTERS) {
            throw invalid("Expected 12 chapter outlines.");
        }
        Set<Integer> numbers = new HashSet<>();
        List<ChapterOutline> result = new ArrayList<>();
        for (JsonNode chapter : chapters) {
            int number = chapter.path("chapter_number").asInt();
            if (number < 1 || number > CHAPTERS || !numbers.add(number)) {
                throw invalid("The outline contains an invalid or repeated chapter number.");
            }
            result.add(new ChapterOutline(number, text(chapter, "name", 255),
                    text(chapter, "description", 4000)));
        }
        result.sort(Comparator.comparingInt(ChapterOutline::getChapterNumber));
        return result;
    }

    private List<GeneratedChapter> readBatch(JsonNode answer, List<ChapterOutline> outlines, int start) {
        JsonNode batch = answer.path("chapters");
        if (!batch.isArray() || batch.size() != 2) {
            throw invalid("Expected two chapters in a question batch.");
        }
        Set<Integer> numbers = new HashSet<>();
        List<GeneratedChapter> result = new ArrayList<>();
        for (JsonNode chapter : batch) {
            int number = chapter.path("chapter_number").asInt();
            if (number < start || number > start + 1 || !numbers.add(number)) {
                throw invalid("A question batch used the wrong chapter numbers.");
            }
            ChapterOutline outline = outlines.get(number - 1);
            result.add(new GeneratedChapter(number, outline.getName(), outline.getDescription(),
                    readQuestions(chapter.path("questions"))));
        }
        result.sort(Comparator.comparingInt(GeneratedChapter::getNumber));
        return result;
    }

    private List<GeneratedQuestion> readQuestions(JsonNode questions) {
        if (!questions.isArray() || questions.size() != QUESTIONS_PER_TYPE * 2) {
            throw invalid("Expected 40 questions for each chapter.");
        }
        List<GeneratedQuestion> result = new ArrayList<>();
        Set<String> prompts = new HashSet<>();
        int multipleChoiceCount = 0;
        for (JsonNode question : questions) {
            String prompt = text(question, "question", 4000);
            if (!prompts.add(prompt)) {
                throw invalid("A chapter contains repeated questions.");
            }
            String type = text(question, "question_type", 30);
            String correct = text(question, "correct_answer", 255);
            String explanation = text(question, "explanation", 4000);
            List<String> choices = List.of();
            if ("MULTIPLE_CHOICE".equals(type)) {
                choices = readChoices(question.path("choices"), correct);
                multipleChoiceCount++;
            } else if (!"SHORT_ANSWER".equals(type)) {
                throw invalid("The course contains an unknown question type.");
            }
            result.add(new GeneratedQuestion(prompt, type, correct, explanation, choices));
        }
        if (multipleChoiceCount != QUESTIONS_PER_TYPE) {
            throw invalid("Expected 20 questions of each type for each chapter.");
        }
        return result;
    }

    private List<String> readChoices(JsonNode choices, String correct) {
        if (!choices.isArray() || choices.size() != 4) {
            throw invalid("A multiple-choice question requires four choices.");
        }
        List<String> result = new ArrayList<>();
        for (JsonNode choice : choices) {
            String value = choice.asText().strip();
            if (!choice.isTextual() || value.isEmpty() || value.length() > 255 || result.contains(value)) {
                throw invalid("A multiple-choice question contains an invalid choice.");
            }
            result.add(value);
        }
        if (!result.contains(correct)) {
            throw invalid("The answer should match one of the choices.");
        }
        return result;
    }

    private String text(JsonNode node, String field, int maxLength) {
        JsonNode value = node.path(field);
        String text = value.asText().strip();
        if (!value.isTextual() || text.isEmpty() || text.length() > maxLength) {
            throw invalid("A course response contains an invalid " + field + ".");
        }
        return text;
    }

    private CourseGenerationException invalid(String reason) {
        return new CourseGenerationException(reason + " Try generating the course again.");
    }
}
