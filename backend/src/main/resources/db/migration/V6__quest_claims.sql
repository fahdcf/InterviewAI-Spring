CREATE TABLE quest_claims (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    request_id VARCHAR(36) NOT NULL,
    quest_type VARCHAR(10) NOT NULL,
    xp_awarded INT NOT NULL,
    claimed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_quest_claim_request UNIQUE (user_id, request_id),
    CONSTRAINT fk_quest_claim_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
