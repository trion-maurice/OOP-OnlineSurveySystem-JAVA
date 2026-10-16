
import java.util.ArrayList;
import java.util.List;

public class Respondent extends User {
    private final List<Answer> answers = new ArrayList<>();

    public Respondent(String username, String password) {
        super(username, password);
    }

    public void addAnswer(Answer answer) {
        answers.add(answer);
    }

    public List<Answer> getAnswers() {
        return new ArrayList<>(answers);
    }
}
