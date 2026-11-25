package com.interviewai.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_goals")
public class UserGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "goal_name", length = 255, nullable = false)
    private String name;

    @Column(name = "goal_type", length = 50, nullable = false)
    private String type;

    @Column(name = "target_value", nullable = false)
    private int target;

    @Column(name = "start_value")
    private Integer startValue;

    @Column(name = "current_value")
    private Integer currentValue;

    @Column(name = "is_active")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Boolean active;

    @Column(name = "created_at", columnDefinition = "timestamp", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "timestamp", insertable = false, updatable = false, nullable = false)
    private LocalDateTime updatedAt;

    protected UserGoal() {
    }

    public UserGoal(int userId, String name, String type, int target, Integer startValue) {
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.target = target;
        this.startValue = startValue;
        this.active = true;
        this.currentValue = 0;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public int getTarget() { return target; }

    public int getStartValue() {
        return startValue == null ? 0 : startValue;
    }
}
