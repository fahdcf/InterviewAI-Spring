CREATE TABLE practice_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    question_id INT NOT NULL,
    request_id VARCHAR(36) NOT NULL,
    mode VARCHAR(10) NOT NULL,
    answer TEXT,
    status VARCHAR(20) NOT NULL,
    score INT,
    strengths TEXT,
    improvements TEXT,
    explanation TEXT,
    xp_awarded INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_practice_request UNIQUE (user_id, request_id),
    CONSTRAINT fk_attempt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_question FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE
);
CREATE INDEX idx_attempt_queue ON practice_attempts (status, id);
