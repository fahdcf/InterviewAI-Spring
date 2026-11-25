package com.interviewai.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "quest_claims", uniqueConstraints = @UniqueConstraint(
        name = "uq_quest_claim_request", columnNames = {"user_id", "request_id"}))
public class QuestClaimRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "request_id", length = 36, nullable = false)
    private String requestId;

    @Column(name = "quest_type", length = 10, nullable = false)
    private String type;

    @Column(name = "xp_awarded", nullable = false)
    private int xpAwarded;

    @Column(name = "claimed_at", columnDefinition = "timestamp", insertable = false, updatable = false, nullable = false)
    private LocalDateTime claimedAt;

    protected QuestClaimRecord() {
    }

    public QuestClaimRecord(int userId, String requestId, String type, int xpAwarded) {
        this.userId = userId;
        this.requestId = requestId;
        this.type = type;
        this.xpAwarded = xpAwarded;
    }

    public String getType() { return type; }
    public int getXpAwarded() { return xpAwarded; }
}
