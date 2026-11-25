package com.interviewai.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", length = 100, nullable = false, unique = true)
    private String username;

    @Column(name = "email", length = 255, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "role", length = 20)
    private String legacyRole;

    @Column(name = "created_at", columnDefinition = "timestamp", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_course_id")
    private Integer lastCourseId;

    @Column(name = "last_chapter_id")
    private Integer lastChapterId;

    @Column(name = "last_daily_quest_claim")
    private LocalDate lastDailyQuestClaim;

    @Column(name = "last_monthly_quest_claim")
    private LocalDate lastMonthlyQuestClaim;

    @Column(name = "is_active")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Boolean active;

    @Column(name = "two_factor_enabled")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Boolean twoFactorEnabled;

    @Column(name = "daily_quest_last_reset", columnDefinition = "datetime(6)")
    private LocalDateTime dailyQuestLastReset;

    @Column(name = "daily_quest_claims_count")
    private Integer dailyQuestClaimsCount;

    @Column(name = "session_version", nullable = false)
    private int sessionVersion;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    protected UserAccount() {
    }

    public UserAccount(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.legacyRole = "CANDIDATE";
        this.active = true;
        this.twoFactorEnabled = false;
        this.dailyQuestClaimsCount = 0;
    }

    public Integer getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Integer getLastCourseId() { return lastCourseId; }
    public Integer getLastChapterId() { return lastChapterId; }
    public int getSessionVersion() { return sessionVersion; }

    public void assignRole(Role role) {
        roles.clear();
        roles.add(role);
        legacyRole = role.getName();
        sessionVersion++;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void changeActive(boolean active) {
        this.active = active;
        sessionVersion++;
    }

    public void addRole(Role role) {
        roles.add(role);
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
