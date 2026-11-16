package com.interviewai.backend.service;

import com.interviewai.backend.integration.ai.CourseGenerationException;
import com.interviewai.backend.integration.ai.CourseGenerator;
import com.interviewai.backend.model.CourseInput;
import com.interviewai.backend.model.CourseJob;
import com.interviewai.backend.model.GeneratedCourse;
import com.interviewai.backend.repository.CourseJobRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CourseGenerationWorker {

    private static final Logger LOG = LoggerFactory.getLogger(CourseGenerationWorker.class);
    private final CourseJobRepository jobs;
    private final CourseGenerator generator;
    private final SaveGeneratedCourses saver;

    public CourseGenerationWorker(CourseJobRepository jobs, CourseGenerator generator, SaveGeneratedCourses saver) {
        this.jobs = jobs;
        this.generator = generator;
        this.saver = saver;
    }

    @PostConstruct
    void recover() {
        jobs.recoverInterruptedJobs();
    }

    @Scheduled(fixedDelay = 1000)
    public void processNextJob() {
        jobs.nextQueued().ifPresent(this::generate);
    }

    private void generate(CourseJob job) {
        long id = job.getId();
        if (!jobs.start(id)) {
            return;
        }
        try {
            CourseInput input = jobs.findInput(id);
            GeneratedCourse technical = generator.generate(input, true, step ->
                    jobs.saveProgress(id, step * 6, "Generating the technical course…"));
            GeneratedCourse softSkills = generator.generate(input, false, step ->
                    jobs.saveProgress(id, 45 + step * 6, "Generating the soft-skills course…"));
            jobs.saveProgress(id, 95, "Saving both courses…");
            saver.save(job, technical, softSkills);
        } catch (CourseGenerationException error) {
            jobs.stop(id, "FAILED", error.getMessage());
        } catch (InterruptedException error) {
            jobs.stop(id, "INTERRUPTED", "Generation was interrupted. You can retry.");
            Thread.currentThread().interrupt();
        } catch (RuntimeException error) {
            LOG.error("Saving or generating course job {} failed ({})", id, error.getClass().getSimpleName());
            jobs.stop(id, "FAILED", "Could not finish generation. You can retry.");
        }
    }
}
