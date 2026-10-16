
public class Answer {
    private final Survey survey;
    private final Question question;
    private final String answerText;

    public Answer(Survey survey, Question question, String answerText) {
        this.survey = survey;
        this.question = question;
        this.answerText = answerText;
    }

    public Survey getSurvey() {
        return survey;
    }

    public Question getQuestion() {
        return question;
    }

    public String getAnswerText() {
        return answerText;
    }
}
