package com.interviewai.backend.service;

import com.interviewai.backend.model.CourseJob;
import com.interviewai.backend.model.GeneratedCourse;
import com.interviewai.backend.repository.CourseJobRepository;
import com.interviewai.backend.repository.DashboardRepository;
import com.interviewai.backend.repository.GeneratedCourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaveGeneratedCourses {

    private final GeneratedCourseRepository courses;
    private final CourseJobRepository jobs;
    private final DashboardRepository dashboard;

    public SaveGeneratedCourses(GeneratedCourseRepository courses, CourseJobRepository jobs,
            DashboardRepository dashboard) {
        this.courses = courses;
        this.jobs = jobs;
        this.dashboard = dashboard;
    }

    @Transactional
    public void save(CourseJob job, GeneratedCourse technical, GeneratedCourse softSkills) {
        jobs.lockUser(job.getUserId());
        int technicalId = courses.insert(job.getUserId(), technical);
        int softSkillsId = courses.insert(job.getUserId(), softSkills);
        courses.linkPair(job.getUserId(), technicalId, softSkillsId);
        dashboard.saveLocation(job.getUserId(), technicalId, null);
        jobs.succeed(job.getId(), technicalId, softSkillsId);
    }
}
