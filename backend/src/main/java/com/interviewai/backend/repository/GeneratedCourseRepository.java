package com.interviewai.backend.repository;

import com.interviewai.backend.model.GeneratedChapter;
import com.interviewai.backend.model.GeneratedCourse;
import com.interviewai.backend.model.GeneratedQuestion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;

@Repository
public class GeneratedCourseRepository {

    private final JdbcTemplate jdbc;

    public GeneratedCourseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insert(int userId, GeneratedCourse course) {
        int courseId = insertWithId("""
                INSERT INTO generated_courses (user_id, course_title, status) VALUES (?, ?, 'ACTIVE')
                """, userId, course.getTitle());
        for (GeneratedChapter chapter : course.getChapters()) {
            int chapterId = insertWithId("""
                    INSERT INTO chapters (course_id, chapter_number, name, description)
                    VALUES (?, ?, ?, ?)
                    """, courseId, chapter.getNumber(), chapter.getName(), chapter.getDescription());
            for (GeneratedQuestion question : chapter.getQuestions()) {
                int questionId = insertWithId("""
                        INSERT INTO questions (chapter_id, question, question_type, correct_answer, explanation)
                        VALUES (?, ?, ?, ?, ?)
                        """, chapterId, question.getText(), question.getType(), question.getAnswer(),
                        question.getExplanation());
                for (String choice : question.getChoices()) {
                    jdbc.update("INSERT INTO choices (question_id, choice_text) VALUES (?, ?)", questionId, choice);
                }
            }
        }
        return courseId;
    }

    public void linkPair(int userId, int technical, int softSkills) {
        jdbc.update("""
                INSERT INTO interview_prep (user_id, technical_course_id, softskills_course_id)
                VALUES (?, ?, ?)
                """, userId, technical, softSkills);
    }

    private int insertWithId(String sql, Object... values) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            for (int index = 0; index < values.length; index++) {
                statement.setObject(index + 1, values[index]);
            }
            return statement;
        }, key);
        Number id = key.getKey();
        if (id == null) {
            throw new IllegalStateException("The database did not return an ID.");
        }
        return id.intValue();
    }
}
