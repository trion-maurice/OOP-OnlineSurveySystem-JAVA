
import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main extends JFrame {

    private String currentUsername;
    private String currentRole;
    private SurveyCreator creator;
    private Respondent respondent;

    public Main() {
        setTitle("Online Survey System");
        setSize(750, 550);
        setMinimumSize(new Dimension(650, 450));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                DatabaseHelper.initializeDefaultUsers();
                new Main().showLogin();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Could not initialize the database.\n"
                                + e.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    private void showLogin() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(
                BorderFactory.createEmptyBorder(25, 40, 25, 40)
        );

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel(
                "ONLINE SURVEY SYSTEM",
                SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 23));

        JTextField username = new JTextField(20);
        JPasswordField password = new JPasswordField(20);

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        panel.add(heading, c);

        c.gridwidth = 1;
        c.gridy++;
        c.gridx = 0;
        panel.add(new JLabel("Username:"), c);

        c.gridx = 1;
        panel.add(username, c);

        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel("Password:"), c);

        c.gridx = 1;
        panel.add(password, c);

        JButton login = new JButton("Login");

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        panel.add(login, c);

        JLabel hint = new JLabel(
                "<html><center>Default accounts, if newly created:"
                        + "<br>Creator: creator / 1234"
                        + "<br>Respondent: student / 1234</center></html>"
        );

        c.gridy++;
        panel.add(hint, c);

        login.addActionListener(e -> {
            String user = username.getText().trim();
            String pass = new String(password.getPassword());

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this, "Enter your username and password."
                );
                return;
            }

            String role = DatabaseHelper.authenticate(user, pass);

            if (role == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Login failed. Check your credentials and database."
                );
                return;
            }

            currentUsername = user;
            currentRole = role;

            if ("CREATOR".equalsIgnoreCase(role)) {
                creator = new SurveyCreator(user, pass);
                showCreatorDashboard();
            } else if ("RESPONDENT".equalsIgnoreCase(role)) {
                respondent = new Respondent(user, pass);
                showRespondentDashboard();
            } else {
                JOptionPane.showMessageDialog(
                        this, "This account has an unsupported role."
                );
            }
        });

        setContentPane(panel);
        revalidate();
        repaint();
        setVisible(true);
    }

    private void showCreatorDashboard() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(
                BorderFactory.createEmptyBorder(25, 30, 25, 30)
        );

        JLabel heading = new JLabel(
                "Welcome, " + currentUsername + " (Survey Creator)",
                SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(heading, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new GridLayout(4, 1, 10, 10));

        JButton create = new JButton("Create Survey");
        JButton view = new JButton("View Surveys");
        JButton results = new JButton("View Survey Results");
        JButton logout = new JButton("Logout");

        buttons.add(create);
        buttons.add(view);
        buttons.add(results);
        buttons.add(logout);

        panel.add(buttons, BorderLayout.CENTER);

        create.addActionListener(e -> createSurvey());
        view.addActionListener(e -> showSurveyList(false));
        results.addActionListener(e -> showSurveyList(true));
        logout.addActionListener(e -> logout());

        setContentPane(panel);
        revalidate();
        repaint();
    }

    private void showRespondentDashboard() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(
                BorderFactory.createEmptyBorder(25, 30, 25, 30)
        );

        JLabel heading = new JLabel(
                "Welcome, " + currentUsername + " (Respondent)",
                SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(heading, BorderLayout.NORTH);

        JButton available = new JButton("View and Take Surveys");
        JButton logout = new JButton("Logout");

        JPanel buttons = new JPanel(new GridLayout(2, 1, 10, 10));
        buttons.add(available);
        buttons.add(logout);

        panel.add(buttons, BorderLayout.CENTER);

        available.addActionListener(e -> showSurveyList(false));
        logout.addActionListener(e -> logout());

        setContentPane(panel);
        revalidate();
        repaint();
    }

    private void createSurvey() {
        JTextField titleField = new JTextField();

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.gridx = 0;
        c.gridy = 0;
        form.add(new JLabel("Survey title:"), c);

        c.gridx = 1;
        form.add(titleField, c);

        JTextField[] questions = new JTextField[3];
        JTextField[] options = new JTextField[3];

        @SuppressWarnings("unchecked")
        JComboBox<QuestionType>[] types = new JComboBox[3];

        for (int i = 0; i < 3; i++) {
            questions[i] = new JTextField(25);
            options[i] = new JTextField(25);
            types[i] = new JComboBox<>(QuestionType.values());

            int row = 1 + i * 3;

            c.gridx = 0;
            c.gridy = row;
            form.add(new JLabel("Question " + (i + 1) + ":"), c);

            c.gridx = 1;
            form.add(questions[i], c);

            c.gridx = 0;
            c.gridy = row + 1;
            form.add(new JLabel("Question type:"), c);

            c.gridx = 1;
            form.add(types[i], c);

            c.gridx = 0;
            c.gridy = row + 2;
            form.add(new JLabel("Options (comma-separated):"), c);

            c.gridx = 1;
            form.add(options[i], c);
        }

        JScrollPane scroll = new JScrollPane(form);
        scroll.setPreferredSize(new Dimension(620, 420));

        int choice = JOptionPane.showConfirmDialog(
                this,
                scroll,
                "Create Survey",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String title = titleField.getText().trim();

        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this, "Enter a survey title."
            );
            return;
        }

        Survey survey = new Survey(title);

        for (int i = 0; i < 3; i++) {
            String text = questions[i].getText().trim();

            QuestionType type =
                    (QuestionType) types[i].getSelectedItem();

            if (text.isEmpty() || type == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Enter Question " + (i + 1)
                                + " and select its type."
                );
                return;
            }

            Question question = new Question(text, type);

            if (type != QuestionType.TEXT) {
                String rawOptions = options[i].getText().trim();

                if (rawOptions.isEmpty()) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Enter options for Question " + (i + 1) + "."
                    );
                    return;
                }

                for (String option : rawOptions.split(",")) {
                    String cleaned = option.trim();

                    if (!cleaned.isEmpty()
                            && !question.getOptions().contains(cleaned)) {
                        question.addOption(cleaned);
                    }
                }

                if (question.getOptions().size() < 2) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Each choice question needs at least two "
                                    + "different options."
                    );
                    return;
                }
            }

            survey.addQuestion(question);
        }

        boolean saved = DatabaseHelper.saveSurvey(
                survey, currentUsername
        );

        if (saved) {
            JOptionPane.showMessageDialog(
                    this, "Survey saved successfully!"
            );
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "The survey could not be saved. Check the terminal "
                            + "for the database error.",
                    "Save Failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void showSurveyList(boolean showResults) {
        List<Survey> surveys = DatabaseHelper.loadSurveys();

        if (surveys.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this, "No surveys found, or the database could not "
                            + "load them."
            );
            return;
        }

        DefaultListModel<Survey> model = new DefaultListModel<>();

        for (Survey survey : surveys) {
            model.addElement(survey);
        }

        JList<Survey> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        list.setCellRenderer(
                (jlist, survey, index, selected, focus) -> {
                    JLabel label = new JLabel(
                            survey.getTitle()
                                    + " (ID: " + survey.getId() + ")"
                    );

                    label.setOpaque(true);
                    label.setBorder(
                            BorderFactory.createEmptyBorder(8, 8, 8, 8)
                    );

                    label.setBackground(
                            selected
                                    ? jlist.getSelectionBackground()
                                    : jlist.getBackground()
                    );

                    label.setForeground(
                            selected
                                    ? jlist.getSelectionForeground()
                                    : jlist.getForeground()
                    );

                    return label;
                }
        );

        String prompt = showResults
                ? "Select a survey to view results:"
                : ("RESPONDENT".equalsIgnoreCase(currentRole)
                    ? "Select a survey to answer:"
                    : "Select a survey to view:");

        int choice = JOptionPane.showConfirmDialog(
                this,
                new JScrollPane(list),
                prompt,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION
                || list.getSelectedValue() == null) {
            return;
        }

        Survey selected = list.getSelectedValue();

        if (showResults) {
            String report = DatabaseHelper.getResults(selected.getId());

            JTextArea area = new JTextArea(report, 20, 55);
            area.setEditable(false);
            area.setCaretPosition(0);

            JOptionPane.showMessageDialog(
                    this,
                    new JScrollPane(area),
                    "Survey Results",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } else if ("RESPONDENT".equalsIgnoreCase(currentRole)) {
            takeSurvey(selected);

        } else {
            StringBuilder details = new StringBuilder();
            details.append(selected.getTitle()).append("\n\n");

            for (Question question : selected.getQuestions()) {
                details.append(question.getQuestionText())
                        .append("\nType: ")
                        .append(question.getType())
                        .append("\n");

                for (String option : question.getOptions()) {
                    details.append("  • ")
                            .append(option)
                            .append("\n");
                }

                details.append("\n");
            }

            JTextArea area = new JTextArea(
                    details.toString(), 18, 50
            );
            area.setEditable(false);

            JOptionPane.showMessageDialog(
                    this,
                    new JScrollPane(area),
                    "Survey Details",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private void takeSurvey(Survey survey) {
        Map<Integer, String> answers = new LinkedHashMap<>();

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        Map<Question, JComponent> controls = new LinkedHashMap<>();
        int row = 0;

        for (Question question : survey.getQuestions()) {
            c.gridx = 0;
            c.gridy = row++;
            c.gridwidth = 2;

            form.add(
                    new JLabel(question.getQuestionText()), c
            );

            c.gridy = row++;
            JComponent control;

            if (question.getType() == QuestionType.TEXT) {
                control = new JTextField(30);

            } else if (question.getType()
                    == QuestionType.MULTIPLE_CHOICE) {

                JComboBox<String> combo = new JComboBox<>(
                        question.getOptions().toArray(new String[0])
                );
                combo.setSelectedIndex(-1);
                control = combo;

            } else {
                JPanel checks = new JPanel(
                        new GridLayout(0, 1)
                );

                for (String option : question.getOptions()) {
                    checks.add(new JCheckBox(option));
                }

                control = checks;
            }

            controls.put(question, control);
            form.add(control, c);
        }

        JScrollPane scroll = new JScrollPane(form);
        scroll.setPreferredSize(new Dimension(560, 400));

        int choice = JOptionPane.showConfirmDialog(
                this,
                scroll,
                survey.getTitle(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        for (Map.Entry<Question, JComponent> entry
                : controls.entrySet()) {

            Question question = entry.getKey();
            JComponent control = entry.getValue();
            String value;

            if (control instanceof JTextField) {
                value = ((JTextField) control).getText().trim();

            } else if (control instanceof JComboBox<?>) {
                Object selected =
                        ((JComboBox<?>) control).getSelectedItem();

                value = selected == null
                        ? ""
                        : selected.toString();

            } else {
                JPanel checks = (JPanel) control;
                List<String> selectedOptions = new ArrayList<>();

                for (Component component : checks.getComponents()) {
                    if (component instanceof JCheckBox
                            && ((JCheckBox) component).isSelected()) {
                        selectedOptions.add(
                                ((JCheckBox) component).getText()
                        );
                    }
                }

                value = String.join(", ", selectedOptions);
            }

            if (value.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this, "Please answer every question."
                );
                return;
            }

            answers.put(question.getId(), value);
        }

        boolean submitted = DatabaseHelper.submitAnswers(
                survey.getId(), currentUsername, answers
        );

        if (submitted) {
            JOptionPane.showMessageDialog(
                    this,
                    "Your responses have been submitted successfully."
            );
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Your responses could not be submitted. "
                            + "Check the terminal for the database error.",
                    "Submission Failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void logout() {
        currentUsername = null;
        currentRole = null;
        creator = null;
        respondent = null;
        showLogin();
    }
}
