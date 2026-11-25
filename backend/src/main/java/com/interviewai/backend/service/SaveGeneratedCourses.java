package com.interviewai.backend.service;

import com.interviewai.backend.model.CourseJob;
import com.interviewai.backend.model.GeneratedChapter;
import com.interviewai.backend.model.GeneratedCourse;
import com.interviewai.backend.model.GeneratedQuestion;
import com.interviewai.backend.model.entity.Chapter;
import com.interviewai.backend.model.entity.Choice;
import com.interviewai.backend.model.entity.Course;
import com.interviewai.backend.model.entity.InterviewPrep;
import com.interviewai.backend.model.entity.Question;
import com.interviewai.backend.repository.AccountRepository;
import com.interviewai.backend.repository.ChapterRepository;
import com.interviewai.backend.repository.ChoiceRepository;
import com.interviewai.backend.repository.CourseJobRepository;
import com.interviewai.backend.repository.CourseRepository;
import com.interviewai.backend.repository.InterviewPrepRepository;
import com.interviewai.backend.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaveGeneratedCourses {

    private final CourseRepository courses;
    private final ChapterRepository chapters;
    private final QuestionRepository questions;
    private final ChoiceRepository choices;
    private final InterviewPrepRepository pairs;
    private final CourseJobRepository jobs;
    private final AccountRepository users;
    private final AccountService accounts;

    public SaveGeneratedCourses(CourseRepository courses, ChapterRepository chapters, QuestionRepository questions,
            ChoiceRepository choices, InterviewPrepRepository pairs, CourseJobRepository jobs, AccountRepository users,
            AccountService accounts) {
        this.courses = courses;
        this.chapters = chapters;
        this.questions = questions;
        this.choices = choices;
        this.pairs = pairs;
        this.jobs = jobs;
        this.users = users;
        this.accounts = accounts;
    }

    @Transactional
    public void save(CourseJob job, GeneratedCourse technical, GeneratedCourse softSkills) {
        int userId = job.getUserId();
        accounts.lockActiveAccount(userId);
        int technicalId = insert(userId, technical);
        int softSkillsId = insert(userId, softSkills);
        pairs.save(new InterviewPrep(userId, technicalId, softSkillsId));
        users.saveLocation(userId, technicalId, null);
        if (jobs.succeed(job.getId(), technicalId, softSkillsId) != 1) {
            throw new IllegalStateException("The generation job is no longer running.");
        }
    }

    private int insert(int userId, GeneratedCourse generated) {
        Course course = courses.save(new Course(userId, generated.getTitle(), "ACTIVE"));
        for (GeneratedChapter part : generated.getChapters()) {
            Chapter chapter = chapters.save(new Chapter(course, part.getNumber(), part.getName(), part.getDescription()));
            for (GeneratedQuestion item : part.getQuestions()) {
                Question question = questions.save(new Question(chapter, item.getText(), item.getType(),
                        item.getAnswer(), item.getExplanation()));
                for (String choice : item.getChoices()) {
                    choices.save(new Choice(question, choice));
                }
            }
        }
        return course.getId();
    }
}
