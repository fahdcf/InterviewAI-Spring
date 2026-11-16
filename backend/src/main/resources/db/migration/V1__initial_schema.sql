-- Tables and reference roles from the InterviewAI baseline.

CREATE TABLE `roles` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `description` text DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `roles_name` (`name`)
);

CREATE TABLE `permissions` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `description` text DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `permissions_name` (`name`)
);

CREATE TABLE `users` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `username` varchar(100) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `password_hash` varchar(255) DEFAULT NULL,
  `role` varchar(20) DEFAULT 'CANDIDATE',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_course_id` INT DEFAULT NULL,
  `last_chapter_id` INT DEFAULT NULL,
  `last_daily_quest_claim` date DEFAULT NULL,
  `last_monthly_quest_claim` date DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT 1,
  `two_factor_enabled` tinyint(1) DEFAULT 0,
  `daily_quest_last_reset` datetime DEFAULT (current_timestamp() - INTERVAL '1' DAY),
  `daily_quest_claims_count` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `users_username` (`username`),
  KEY `users_fk_users_last_course` (`last_course_id`),
  KEY `users_fk_users_last_chapter` (`last_chapter_id`)
);

CREATE TABLE `generated_courses` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `course_title` varchar(255) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`id`)
);

CREATE TABLE `chapters` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `course_id` INT NOT NULL,
  `chapter_number` INT DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'NOT_STARTED',
  `completed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `chapters_course_id` (`course_id`),
  CONSTRAINT `chapters_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `generated_courses` (`id`) ON DELETE CASCADE
);

CREATE TABLE `questions` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `chapter_id` INT NOT NULL,
  `question` text DEFAULT NULL,
  `question_type` varchar(50) DEFAULT 'MULTIPLE_CHOICE',
  `correct_answer` varchar(255) DEFAULT NULL,
  `explanation` text DEFAULT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'NOT_STARTED',
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `questions_chapter_id` (`chapter_id`),
  CONSTRAINT `questions_ibfk_1` FOREIGN KEY (`chapter_id`) REFERENCES `chapters` (`id`) ON DELETE CASCADE
);

CREATE TABLE `choices` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `question_id` INT NOT NULL,
  `choice_text` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `choices_question_id` (`question_id`),
  CONSTRAINT `choices_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`) ON DELETE CASCADE
);

CREATE TABLE `interview_prep` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `technical_course_id` INT DEFAULT NULL,
  `softskills_course_id` INT DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `interview_prep_idx_user_id` (`user_id`),
  KEY `interview_prep_idx_technical_course_id` (`technical_course_id`),
  KEY `interview_prep_idx_softskills_course_id` (`softskills_course_id`),
  CONSTRAINT `interview_prep_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `interview_prep_ibfk_2` FOREIGN KEY (`technical_course_id`) REFERENCES `generated_courses` (`id`) ON DELETE SET NULL,
  CONSTRAINT `interview_prep_ibfk_3` FOREIGN KEY (`softskills_course_id`) REFERENCES `generated_courses` (`id`) ON DELETE SET NULL
);

CREATE TABLE `onboarding_data` (
  `user_id` INT NOT NULL,
  `interview_type` varchar(50) NOT NULL COMMENT 'JOB, VISA, INTERNSHIP, UNIVERSITY',
  `language` varchar(50) NOT NULL COMMENT 'ENGLISH, FRENCH, ARABIC, SPANISH',
  `timeline` varchar(50) NOT NULL COMMENT 'TOMORROW, THIS_WEEK, LATER',
  `context` text DEFAULT NULL COMMENT 'Industry, program, or position details',
  `cv_path` varchar(500) DEFAULT NULL COMMENT 'File path to uploaded CV/resume',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`user_id`),
  KEY `onboarding_data_idx_interview_type` (`interview_type`),
  KEY `onboarding_data_idx_language` (`language`),
  CONSTRAINT `onboarding_data_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
);

CREATE TABLE `progress` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `course_id` INT NOT NULL,
  `xp` INT DEFAULT 0,
  `last_updated` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `progress_user_id` (`user_id`),
  KEY `progress_course_id` (`course_id`),
  CONSTRAINT `progress_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `progress_ibfk_2` FOREIGN KEY (`course_id`) REFERENCES `generated_courses` (`id`) ON DELETE CASCADE
);

CREATE TABLE `feedback` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT DEFAULT NULL,
  `course_id` INT DEFAULT NULL,
  `message` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`)
);

CREATE TABLE `role_permissions` (
  `role_id` INT NOT NULL,
  `permission_id` INT NOT NULL,
  PRIMARY KEY (`role_id`,`permission_id`),
  KEY `role_permissions_permission_id` (`permission_id`),
  CONSTRAINT `role_permissions_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `role_permissions_ibfk_2` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE
);

CREATE TABLE `user_roles` (
  `user_id` INT NOT NULL,
  `role_id` INT NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `user_roles_role_id` (`role_id`),
  CONSTRAINT `user_roles_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_roles_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
);

CREATE TABLE `user_goals` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `goal_name` varchar(255) NOT NULL,
  `goal_type` varchar(50) NOT NULL,
  `target_value` INT NOT NULL,
  `start_value` INT DEFAULT 0,
  `current_value` INT DEFAULT 0,
  `is_active` tinyint(1) DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `user_goals_user_id` (`user_id`),
  CONSTRAINT `user_goals_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
);

ALTER TABLE users ADD CONSTRAINT fk_users_last_chapter
    FOREIGN KEY (last_chapter_id) REFERENCES chapters (id) ON DELETE SET NULL;

ALTER TABLE users ADD CONSTRAINT fk_users_last_course
    FOREIGN KEY (last_course_id) REFERENCES generated_courses (id) ON DELETE SET NULL;

INSERT INTO `roles` VALUES (1,'SUPER_ADMIN','Full access to all features'),(6,'CANDIDATE','Standard user');

INSERT INTO `permissions` VALUES (1,'manage_users','Create, edit, delete users'),(2,'manage_courses','Create, edit, delete courses'),(3,'manage_ai_prompts','Manage AI prompts'),(4,'view_analytics','View system analytics'),(5,'moderate_feedback','Moderate user feedback');

INSERT INTO `role_permissions` VALUES (1,1),(1,2),(1,3),(1,4),(1,5);
