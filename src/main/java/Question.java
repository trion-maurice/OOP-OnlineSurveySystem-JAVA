
import java.util.ArrayList;
import java.util.List;

public class Question {
    private int id;
    private final String questionText;
    private final QuestionType type;
    private final List<String> options = new ArrayList<>();

    public Question(String questionText, QuestionType type) {
        this.questionText = questionText;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public QuestionType getType() {
        return type;
    }

    public void addOption(String option) {
        if (option != null && !option.trim().isEmpty()) {
            options.add(option.trim());
        }
    }

    public List<String> getOptions() {
        return new ArrayList<>(options);
    }
}
