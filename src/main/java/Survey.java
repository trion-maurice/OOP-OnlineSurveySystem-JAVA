
import java.util.ArrayList;
import java.util.List;

public class Survey {
    private int id;
    private final String title;
    private final List<Question> questions = new ArrayList<>();

    public Survey(String title) {
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void addQuestion(Question question) {
        questions.add(question);
    }

    public List<Question> getQuestions() {
        return new ArrayList<>(questions);
    }
}
