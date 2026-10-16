
CREATE DATABASE IF NOT EXISTS online_survey_system;
USE online_survey_system;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS surveys (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    creator_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS questions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    survey_id INT NOT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(30) NOT NULL,
    question_order INT NOT NULL,
    FOREIGN KEY (survey_id)
        REFERENCES surveys(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS question_options (
    id INT AUTO_INCREMENT PRIMARY KEY,
    question_id INT NOT NULL,
    option_text VARCHAR(255) NOT NULL,
    FOREIGN KEY (question_id)
        REFERENCES questions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS survey_responses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    survey_id INT NOT NULL,
    respondent_id INT NOT NULL,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (survey_id, respondent_id),
    FOREIGN KEY (survey_id)
        REFERENCES surveys(id) ON DELETE CASCADE,
    FOREIGN KEY (respondent_id)
        REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS answers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    response_id INT NOT NULL,
    question_id INT NOT NULL,
    answer_text TEXT,
    FOREIGN KEY (response_id)
        REFERENCES survey_responses(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id)
        REFERENCES questions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS answer_options (
    answer_id INT NOT NULL,
    option_id INT NOT NULL,
    PRIMARY KEY (answer_id, option_id),
    FOREIGN KEY (answer_id)
        REFERENCES answers(id) ON DELETE CASCADE,
    FOREIGN KEY (option_id)
        REFERENCES question_options(id)
);
