package com.interviewai.backend.integration.ai;

import java.util.List;

class CoursePrompts {

    private String mapInterviewType(String type) {
        switch (type) {
            case "JOB": return "Job";
            case "VISA": return "Visa";
            case "INTERNSHIP": return "Internship";
            case "UNIVERSITY": return "University";
            default: return type;
        }
    }

    private String mapLanguage(String lang) {
        switch (lang) {
            case "ENGLISH": return "English";
            case "FRENCH": return "French";
            case "ARABIC": return "Arabic";
            case "SPANISH": return "Spanish";
            default: return lang;
        }
    }

    private String mapTimeline(String timeline) {
        switch (timeline) {
            case "TOMORROW": return "Intensive - Interview tomorrow";
            case "THIS_WEEK": return "Moderate - Interview this week";
            case "LATER": return "Relaxed - Interview later";
            default: return timeline;
        }
    }

    /**
     * STAGE 1: Build a prompt to generate 12 chapter outlines (without questions).
     */
    public String buildChapterOutlinesPrompt(
        String interviewType,
        String language,
        String timeline,
        String context,
        String cvContent,
        String courseContent,
        String courseGoal
    ) {
        return String.format(
            "You are an expert AI course designer.\n\n" +
            "Your task: Generate a comprehensive interview preparation course outline for a **%s** position or program.\n" +
            "Focus area: %s\n" +
            "User preparation timeline: %s\n" +
            "Preferred language: %s\n\n" +
            "User context: %s\n" +
            "Course goal: %s\n\n" +
            "Below is the candidate’s CV text (use it to tailor topics and skill focus):\n" +
            "\"\"\"%s\"\"\"\n\n" +
            "Instructions:\n" +
            "- Create exactly 12 chapters (each representing a topic area).\n" +
            "- Each chapter must have:\n" +
            "  * A clear and descriptive name\n" +
            "  * A detailed description of what will be covered\n\n" +
            "Return your response **as pure JSON**, with no explanations or extra text, in this exact format:\n" +
            "{\n" +
            "  \"course_title\": \"[Generated title]\",\n" +
            "  \"chapters\": [\n" +
            "    {\n" +
            "      \"chapter_number\": 1,\n" +
            "      \"name\": \"[Chapter name]\",\n" +
            "      \"description\": \"[Detailed description]\"\n" +
            "    }\n" +
            "  ]\n" +
            "}",
            mapInterviewType(interviewType),
            courseContent,
            mapTimeline(timeline),
            mapLanguage(language),
            context,
            courseGoal,
            cvContent
        );
    }
    /**
     * STAGE 2: Build a prompt to generate 2 specific chapters with questions.
     * Each chapter will have 20 multiple choice + 20 short answer questions = 40 total.
     */
    public String buildChapterQuestionsPrompt(String courseTitle, List<ChapterOutline> outlines,
                                             int startChapterNum, int endChapterNum,
                                             String language) {
        StringBuilder chapterInfo = new StringBuilder();
        for (ChapterOutline outline : outlines) {
            if (outline.getChapterNumber() >= startChapterNum &&
                outline.getChapterNumber() <= endChapterNum) {
                chapterInfo.append(String.format(
                    "Chapter %d: %s - %s\n",
                    outline.getChapterNumber(),
                    outline.getName(),
                    outline.getDescription()
                ));
            }
        }

        return String.format(
            "Generate detailed questions for the following chapters of the course '%s':\n\n" +
            "%s\n" +
            "Language: %s\n\n" +
            "For EACH chapter, generate:\n" +
            "1. 20 multiple-choice questions with:\n" +
            "   - 4 short, concise answer choices (A, B, C, D)\n" +
            "   - Each choice should be a SHORT PHRASE (maximum 10-15 words)\n" +
            "   - Clear correct answer\n" +
            "   - Brief explanation\n\n" +
            "2. 20 short-answer questions with:\n" +
            "   - Questions that require SHORT ANSWERS (1-3 words or a brief phrase)\n" +
            "   - Clear correct answer (must be short and concise)\n" +
            "   - Brief explanation\n\n" +
            "Return the response in valid JSON format with this exact structure:\n" +
            "{\n" +
            "  \"chapters\": [\n" +
            "    {\n" +
            "      \"chapter_number\": %d,\n" +
            "      \"name\": \"[Chapter name]\",\n" +
            "      \"description\": \"[Brief description]\",\n" +
            "      \"questions\": [\n" +
            "        {\n" +
            "          \"id\": 1,\n" +
            "          \"question\": \"[Question text]\",\n" +
            "          \"question_type\": \"MULTIPLE_CHOICE\",\n" +
            "          \"choices\": [\"Short Choice A\", \"Short Choice B\", \"Short Choice C\", \"Short Choice D\"],\n" +
            "          \"correct_answer\": \"Short Choice A\",\n" +
            "          \"explanation\": \"[Why this is correct]\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"id\": 21,\n" +
            "          \"question\": \"[Short answer question]\",\n" +
            "          \"question_type\": \"SHORT_ANSWER\",\n" +
            "          \"choices\": null,\n" +
            "          \"correct_answer\": \"[Brief answer]\",\n" +
            "          \"explanation\": \"[Why this is correct]\"\n" +
            "        },\n" +
            "        \n" +
            "      ]\n" +
            "    },\n" +
            "    \n" +
            "  ]\n" +
            "}\n\n" +
            "IMPORTANT: Make sure chapters complement each other and build upon previous knowledge.",
            courseTitle,
            chapterInfo.toString(),
            mapLanguage(language),
            startChapterNum
        );
    }
}
