package com.interviewai.backend.repository;

import com.interviewai.backend.model.entity.Choice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChoiceRepository extends JpaRepository<Choice, Integer> {

    @Query("""
            select c.question.id as questionId, c.text as text from Choice c
            where c.question.chapter.course.id = :courseId and c.text is not null
            order by c.question.id, c.id
            """)
    List<QuestionChoice> findCourseChoices(int courseId);

    @Query("select c.text from Choice c where c.question.id = :questionId and c.text is not null order by c.id")
    List<String> findChoices(int questionId);

    interface QuestionChoice {
        int getQuestionId();
        String getText();
    }
}
