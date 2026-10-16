# Online Survey System

A Java Swing coursework project demonstrating object-oriented programming and event-driven GUI design. The application lets a survey creator build a survey with text, single-choice, and multiple-answer questions. A student can then complete the survey and the creator can review the submitted answers.

## Run in VS Code

Open this project folder in VS Code with the Extension Pack for Java installed.
The workspace settings include the Connector/J JAR in the Java classpath; open
`src/main/java/Main.java` and use **Run** above `main`.

Before running, copy `config.properties.example` to `config.properties` and set
the username and password for a MySQL account that can use
`online_survey_system`. Alternatively, set `SURVEY_DB_URL`, `SURVEY_DB_USER`,
and `SURVEY_DB_PASSWORD` in the environment. Do not commit `config.properties`.
Run `database.sql` once to create the database and its tables.

The project can also be compiled manually from its root folder with JDK 11 or
later:

```powershell
javac -cp "lib\mysql-connector-j-26.7.0.jar" -d out src\main\java\*.java
java -cp "out;lib\mysql-connector-j-26.7.0.jar" Main
```

## Demo accounts

| Role | Username | Password |
|---|---|---|
| Survey creator | `creator` | `1234` |
| Student respondent | `student` | `1234` |

## Coursework features

- Swing GUI using `JFrame`, panels, forms, lists, buttons, dialogs, and scroll panes.
- Encapsulated domain classes: `Survey`, `Question`, `Answer`, and `Respondent`.
- Inheritance via `User`, `SurveyCreator`, and `Respondent`.
- An enum (`QuestionType`) for supported response types.
- Input validation and MySQL-backed response and results persistence.

"java -cp ".\out;.\lib\mysql-connector-j-26.7.0.jar" Main"
