package com.interviewai.backend.repository;

import com.interviewai.backend.model.entity.InterviewPrep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewPrepRepository extends JpaRepository<InterviewPrep, Integer> {

    List<InterviewPrep> findByUserIdOrderByCreatedAtDescIdDesc(int userId);
}
