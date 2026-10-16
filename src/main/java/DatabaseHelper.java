
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class DatabaseHelper {

    private static final int HASH_ITERATIONS = 65536;
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 256;

    private DatabaseHelper() {
        // Utility class
    }

     
    // PASSWORD HASHING
     

    private static String hashPassword(String password) throws Exception {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                HASH_ITERATIONS,
                HASH_LENGTH
        );

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            byte[] hash = factory.generateSecret(spec).getEncoded();

            return HASH_ITERATIONS + ":"
                    + Base64.getEncoder().encodeToString(salt) + ":"
                    + Base64.getEncoder().encodeToString(hash);
        } finally {
            spec.clearPassword();
        }
    }

    
private static boolean verifyPassword(
        String password,
        String storedHash) {

    if (password == null || storedHash == null) {
        return false;
    }

    try {
        String[] parts = storedHash.split(":");

        int iterations;
        byte[] salt;
        byte[] expectedHash;

        if (parts.length == 2) {
            // Existing database format: salt:hash
            iterations = HASH_ITERATIONS;
            salt = Base64.getDecoder().decode(parts[0]);
            expectedHash = Base64.getDecoder().decode(parts[1]);

        } else if (parts.length == 3) {
            // Format: iterations:salt:hash
            iterations = Integer.parseInt(parts[0]);
            salt = Base64.getDecoder().decode(parts[1]);
            expectedHash = Base64.getDecoder().decode(parts[2]);

        } else {
            return false;
        }

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                iterations,
                expectedHash.length * 8
        );

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256"
                    );

            byte[] actualHash =
                    factory.generateSecret(spec).getEncoded();

            if (actualHash.length != expectedHash.length) {
                return false;
            }

            int difference = 0;

            for (int i = 0; i < actualHash.length; i++) {
                difference |= actualHash[i] ^ expectedHash[i];
            }

            return difference == 0;

        } finally {
            spec.clearPassword();
        }

    } catch (Exception e) {
        System.err.println(
                "Password verification error: " + e.getMessage()
        );
        return false;
    }
}


     
    // DEFAULT USERS
     

    public static void initializeDefaultUsers() {
        String sql = """
                INSERT IGNORE INTO users
                    (username, password_hash, role)
                VALUES (?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "creator");
            ps.setString(2, hashPassword("1234"));
            ps.setString(3, "CREATOR");
            ps.executeUpdate();

            ps.setString(1, "student");
            ps.setString(2, hashPassword("1234"));
            ps.setString(3, "RESPONDENT");
            ps.executeUpdate();

        } catch (Exception e) {
            System.err.println("Could not initialize default users.");
            e.printStackTrace();
        }
    }

     
    // LOGIN
    // Returns CREATOR, RESPONDENT, or null.
     

    public static String authenticate(
            String username,
            String password) {

        String sql = """
                SELECT password_hash, role
                FROM users
                WHERE username = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");

                    if (verifyPassword(password, storedHash)) {
                        return rs.getString("role");
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Login failed.");
            e.printStackTrace();
        }

        return null;
    }

     
    // SAVE A SURVEY AND ITS QUESTIONS
     

    public static boolean saveSurvey(
            Survey survey,
            String creatorUsername) {

        String surveySql = """
                INSERT INTO surveys (title, description, creator_id)
                VALUES (?, NULL,
                    (SELECT id FROM users WHERE username = ?))
                """;

        String questionSql = """
                INSERT INTO questions
                    (survey_id, question_text, question_type)
                VALUES (?, ?, ?)
                """;

        String optionSql = """
                INSERT INTO question_options (question_id, option_text)
                VALUES (?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int surveyId;

                try (PreparedStatement surveyPs = conn.prepareStatement(
                        surveySql, Statement.RETURN_GENERATED_KEYS)) {

                    surveyPs.setString(1, survey.getTitle());
                    surveyPs.setString(2, creatorUsername);

                    int affected = surveyPs.executeUpdate();

                    if (affected == 0) {
                        conn.rollback();
                        return false;
                    }

                    try (ResultSet keys = surveyPs.getGeneratedKeys()) {
                        if (!keys.next()) {
                            conn.rollback();
                            return false;
                        }

                        surveyId = keys.getInt(1);
                    }
                }

                try (PreparedStatement questionPs =
                             conn.prepareStatement(
                                     questionSql,
                                     Statement.RETURN_GENERATED_KEYS);
                     PreparedStatement optionPs =
                             conn.prepareStatement(optionSql)) {

                    for (Question question : survey.getQuestions()) {
                        questionPs.setInt(1, surveyId);
                        questionPs.setString(
                                2, question.getQuestionText());
                        questionPs.setString(
                                3, question.getType().name());

                        questionPs.executeUpdate();

                        int questionId;

                        try (ResultSet keys =
                                     questionPs.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new SQLException(
                                        "Could not retrieve question ID.");
                            }

                            questionId = keys.getInt(1);
                        }

                        for (String option : question.getOptions()) {
                            optionPs.setInt(1, questionId);
                            optionPs.setString(2, option.trim());
                            optionPs.addBatch();
                        }

                        optionPs.executeBatch();
                    }
                }

                conn.commit();
                survey.setId(surveyId);
                return true;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception e) {
            System.err.println("Could not save survey.");
            e.printStackTrace();
            return false;
        }
    }

     
    // LOAD ALL SURVEYS
     

    public static List<Survey> loadSurveys() {
        List<Survey> surveys = new ArrayList<>();

        String sql = """
                SELECT id, title
                FROM surveys
                ORDER BY id DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Survey survey = new Survey(rs.getString("title"));
                survey.setId(rs.getInt("id"));

                loadQuestions(conn, survey);
                surveys.add(survey);
            }

        } catch (SQLException e) {
            System.err.println("Could not load surveys.");
            e.printStackTrace();
        }

        return surveys;
    }

    private static void loadQuestions(
            Connection conn,
            Survey survey) throws SQLException {

        String sql = """
                SELECT id, question_text, question_type
                FROM questions
                WHERE survey_id = ?
                ORDER BY id
                """;

        List<Question> questions = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, survey.getId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    QuestionType type = QuestionType.valueOf(
                            rs.getString("question_type")
                                    .toUpperCase()
                    );

                    Question question = new Question(
                            rs.getString("question_text"),
                            type
                    );

                    question.setId(rs.getInt("id"));
                    questions.add(question);
                }
            }
        }

        // Load options after closing the question ResultSet.
        String optionSql = """
                SELECT option_text
                FROM question_options
                WHERE question_id = ?
                ORDER BY id
                """;

        try (PreparedStatement optionPs =
                     conn.prepareStatement(optionSql)) {

            for (Question question : questions) {
                optionPs.setInt(1, question.getId());

                try (ResultSet optionRs = optionPs.executeQuery()) {
                    while (optionRs.next()) {
                        question.addOption(
                                optionRs.getString("option_text")
                        );
                    }
                }

                survey.addQuestion(question);
            }
        }
    }

     
    // SUBMIT RESPONSES
    //
    // answers maps question IDs to selected answer text.
    // For multiple-answer questions, selected answers are
    // expected to be separated by commas.
     

    public static boolean submitAnswers(
            int surveyId,
            String username,
            Map<Integer, String> answers) {

        String respondentSql = """
                SELECT id
                FROM users
                WHERE username = ?
                """;

        String responseSql = """
                INSERT INTO survey_responses
                    (survey_id, respondent_id)
                VALUES (?, ?)
                """;

        String answerSql = """
                INSERT INTO answers
                    (response_id, question_id, answer_text)
                VALUES (?, ?, ?)
                """;

        String optionLookupSql = """
                SELECT id
                FROM question_options
                WHERE question_id = ?
                  AND option_text = ?
                """;

        String answerOptionSql = """
                INSERT INTO answer_options
                    (answer_id, option_id)
                VALUES (?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int respondentId;

                try (PreparedStatement userPs =
                             conn.prepareStatement(respondentSql)) {

                    userPs.setString(1, username);

                    try (ResultSet rs = userPs.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }

                        respondentId = rs.getInt("id");
                    }
                }

                int responseId;

                try (PreparedStatement responsePs =
                             conn.prepareStatement(
                                     responseSql,
                                     Statement.RETURN_GENERATED_KEYS)) {

                    responsePs.setInt(1, surveyId);
                    responsePs.setInt(2, respondentId);
                    responsePs.executeUpdate();

                    try (ResultSet keys = responsePs.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "Could not retrieve response ID.");
                        }

                        responseId = keys.getInt(1);
                    }
                }

                try (PreparedStatement answerPs =
                             conn.prepareStatement(
                                     answerSql,
                                     Statement.RETURN_GENERATED_KEYS);
                     PreparedStatement optionLookupPs =
                             conn.prepareStatement(optionLookupSql);
                     PreparedStatement answerOptionPs =
                             conn.prepareStatement(answerOptionSql)) {

                    for (Map.Entry<Integer, String> entry
                            : answers.entrySet()) {

                        int questionId = entry.getKey();
                        String answerText = entry.getValue();

                        answerPs.setInt(1, responseId);
                        answerPs.setInt(2, questionId);
                        answerPs.setString(3, answerText);
                        answerPs.executeUpdate();

                        int answerId;

                        try (ResultSet keys =
                                     answerPs.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new SQLException(
                                        "Could not retrieve answer ID.");
                            }

                            answerId = keys.getInt(1);
                        }

                        if (answerText == null || answerText.isBlank()) {
                            continue;
                        }

                        String[] selectedOptions =
                                answerText.split(",");

                        for (String selected : selectedOptions) {
                            optionLookupPs.setInt(1, questionId);
                            optionLookupPs.setString(
                                    2, selected.trim());

                            try (ResultSet optionRs =
                                         optionLookupPs.executeQuery()) {

                                if (optionRs.next()) {
                                    answerOptionPs.setInt(1, answerId);
                                    answerOptionPs.setInt(
                                            2, optionRs.getInt("id"));
                                    answerOptionPs.executeUpdate();
                                }
                            }
                        }
                    }
                }

                conn.commit();
                return true;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception e) {
            System.err.println("Could not submit answers.");
            e.printStackTrace();
            return false;
        }
    }

     
    // VIEW SURVEY RESULTS
     

    public static String getResults(int surveyId) {
        StringBuilder result = new StringBuilder();

        String surveySql = """
                SELECT title
                FROM surveys
                WHERE id = ?
                """;

        String questionSql = """
                SELECT id, question_text, question_type
                FROM questions
                WHERE survey_id = ?
                ORDER BY id
                """;

        String responseCountSql = """
                SELECT COUNT(*) AS total
                FROM survey_responses
                WHERE survey_id = ?
                """;

        String answerSql = """
                SELECT u.username, a.answer_text
                FROM answers a
                JOIN survey_responses sr ON a.response_id = sr.id
                JOIN users u ON sr.respondent_id = u.id
                WHERE sr.survey_id = ?
                  AND a.question_id = ?
                ORDER BY u.username
                """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String title = "Survey Results";

            try (PreparedStatement titlePs =
                         conn.prepareStatement(surveySql)) {

                titlePs.setInt(1, surveyId);

                try (ResultSet titleRs = titlePs.executeQuery()) {
                    if (titleRs.next()) {
                        title = titleRs.getString("title");
                    } else {
                        return "Survey not found.";
                    }
                }
            }

            result.append("RESULTS: ")
                    .append(title)
                    .append("\n")
                    .append("================================\n\n");

            int totalResponses = 0;

            try (PreparedStatement countPs =
                         conn.prepareStatement(responseCountSql)) {

                countPs.setInt(1, surveyId);

                try (ResultSet countRs = countPs.executeQuery()) {
                    if (countRs.next()) {
                        totalResponses = countRs.getInt("total");
                    }
                }
            }

            result.append("Total responses: ")
                    .append(totalResponses)
                    .append("\n\n");

            try (PreparedStatement questionPs =
                         conn.prepareStatement(questionSql)) {

                questionPs.setInt(1, surveyId);

                try (ResultSet questionRs = questionPs.executeQuery()) {
                    while (questionRs.next()) {
                        int questionId = questionRs.getInt("id");

                        result.append("Question: ")
                                .append(questionRs.getString("question_text"))
                                .append("\n");

                        result.append("Type: ")
                                .append(questionRs.getString("question_type"))
                                .append("\n");

                        // Use a distinct variable name for this query.
                        try (PreparedStatement answerPs =
                                     conn.prepareStatement(answerSql)) {

                            answerPs.setInt(1, surveyId);
                            answerPs.setInt(2, questionId);

                            try (ResultSet answerRs =
                                         answerPs.executeQuery()) {

                                boolean foundAnswer = false;

                                while (answerRs.next()) {
                                    foundAnswer = true;

                                    result.append("  ")
                                            .append(answerRs.getString("username"))
                                            .append(": ")
                                            .append(answerRs.getString("answer_text"))
                                            .append("\n");
                                }

                                if (!foundAnswer) {
                                    result.append("  No answers yet.\n");
                                }
                            }
                        }

                        result.append("\n");
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Could not retrieve survey results.");
            e.printStackTrace();

            return "Unable to load results.\n"
                    + "Check the database connection and table columns.\n"
                    + e.getMessage();
        }

        return result.toString();
    }
}
