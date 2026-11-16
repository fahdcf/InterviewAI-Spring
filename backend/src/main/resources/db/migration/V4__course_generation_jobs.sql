CREATE TABLE course_generation_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    request_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    progress INT NOT NULL DEFAULT 0,
    message VARCHAR(255) NOT NULL DEFAULT 'Waiting to start.',
    interview_type VARCHAR(50) NOT NULL,
    language VARCHAR(50) NOT NULL,
    timeline VARCHAR(50) NOT NULL,
    context TEXT NOT NULL,
    description TEXT,
    cv_text TEXT,
    technical_course_id INT,
    softskills_course_id INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_course_job_request UNIQUE (user_id, request_id),
    CONSTRAINT fk_course_job_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_course_job_technical FOREIGN KEY (technical_course_id) REFERENCES generated_courses (id) ON DELETE SET NULL,
    CONSTRAINT fk_course_job_softskills FOREIGN KEY (softskills_course_id) REFERENCES generated_courses (id) ON DELETE SET NULL
);

CREATE INDEX idx_course_job_queue ON course_generation_jobs (status, id);
