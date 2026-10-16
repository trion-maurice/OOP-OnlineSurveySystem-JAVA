
public enum QuestionType {
    TEXT("Text response"),
    MULTIPLE_CHOICE("Multiple choice (one answer)"),
    MULTIPLE_ANSWER("Multiple answer (select all that apply)");

    private final String label;

    QuestionType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
