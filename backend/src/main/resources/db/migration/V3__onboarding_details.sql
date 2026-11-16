ALTER TABLE onboarding_data ADD COLUMN description TEXT;

CREATE TABLE onboarding_cv (
    user_id INT NOT NULL PRIMARY KEY,
    extracted_text TEXT NOT NULL,
    CONSTRAINT fk_onboarding_cv_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
