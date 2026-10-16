
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL =
            "jdbc:mysql://localhost:3306/online_survey_system";
    private static final String USER = "survey_app";

    // Replace this value with the MySQL password you already use.
    private static final String PASSWORD = "TrionOOP123";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "MySQL driver not found. Check the Connector/J JAR.",
                    e
            );
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
