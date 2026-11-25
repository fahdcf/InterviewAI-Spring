package com.interviewai.backend.repository;

import com.interviewai.backend.model.entity.QuestClaimRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestRepository extends JpaRepository<QuestClaimRecord, Long> {

    Optional<QuestClaim> findByUserIdAndRequestId(int userId, String requestId);

    record QuestClaim(String type, int xpAwarded) {
    }
}
