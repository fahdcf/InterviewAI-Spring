package com.interviewai.desktop.api;

import com.interviewai.desktop.api.dto.CourseJobStatus;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class CourseJobApi {

    private static final String PATH = "/api/v1/course-jobs";
    private final ApiClient api;

    public CourseJobApi(ApiClient api) {
        this.api = api;
    }

    public CourseJobStatus start(UUID requestId) throws IOException, InterruptedException {
        return api.writeJson("POST", PATH, Map.of("requestId", requestId), CourseJobStatus.class);
    }

    public CourseJobStatus retry(long jobId, UUID requestId) throws IOException, InterruptedException {
        return api.writeJson("POST", PATH + "/" + jobId + "/retry",
                Map.of("requestId", requestId), CourseJobStatus.class);
    }

    public CourseJobStatus get(long jobId) throws IOException, InterruptedException {
        return api.readJson(PATH + "/" + jobId, CourseJobStatus.class);
    }

    public CourseJobStatus latest() throws IOException, InterruptedException {
        return api.readJson(PATH + "/latest", CourseJobStatus.class);
    }
}
