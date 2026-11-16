package com.interviewai.desktop.api;

import com.interviewai.desktop.api.dto.AttemptResult;
import com.interviewai.desktop.api.dto.PracticeQuestion;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PracticeApi {

    private final ApiClient api;

    public PracticeApi(ApiClient api) {
        this.api = api;
    }

    public List<PracticeQuestion> questions(int courseId, Integer chapterId, boolean review)
            throws IOException, InterruptedException {
        String path;
        if (chapterId == null) {
            path = "/api/v1/courses/" + courseId + "/review";
        } else {
            path = "/api/v1/chapters/" + chapterId + (review ? "/review" : "/questions");
        }
        return List.of(api.readJson(path, PracticeQuestion[].class));
    }

    public AttemptResult submit(int questionId, String mode, String answer, boolean skip, UUID requestId)
            throws IOException, InterruptedException {
        return api.writeJson("POST", "/api/v1/questions/" + questionId + "/attempts",
                Map.of("mode", mode, "answer", answer, "skip", skip, "requestId", requestId), AttemptResult.class);
    }

    public AttemptResult attempt(long id) throws IOException, InterruptedException {
        return api.readJson("/api/v1/attempts/" + id, AttemptResult.class);
    }
}
