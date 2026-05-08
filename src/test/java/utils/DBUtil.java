package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Properties;

public class DBUtil {
    public static Connection getConnection() throws SQLException {
        // Check if we're running in GitHub Actions
        String dbName = System.getenv("POSTGRES_DB");
        String dbUser = System.getenv("POSTGRES_USER");
        String dbPassword = System.getenv("POSTGRES_PASSWORD");

        try {
            if (dbName != null && dbUser != null && dbPassword != null) {
                // We're running in GitHub Actions
                System.out.println("[DEBUG_LOG] Connecting to database using GitHub Actions environment variables");
                return DriverManager
                        .getConnection("jdbc:postgresql://localhost:5432/" + dbName, dbUser, dbPassword);
            } else {
                // We're running locally
                System.out.println("[DEBUG_LOG] Connecting to database using local configuration");
                // Try to load properties from file first
                try {
                    ClassLoader classLoader = DBUtil.class.getClassLoader();
                    InputStream inputStream = classLoader.getResourceAsStream("db.properties");
                    if (inputStream != null) {
                        Properties props = new java.util.Properties();
                        props.load(inputStream);
                        String url = props.getProperty("jdbcUrl");
                        String user = props.getProperty("dataSource.user");
                        String password = props.getProperty("dataSource.password");
                        if (url != null && user != null && password != null) {
                            System.out.println("[DEBUG_LOG] Using connection properties from db.properties");
                            return DriverManager.getConnection(url, user, password);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[DEBUG_LOG] Error loading db.properties: " + e.getMessage());
                }

                // Fallback to hardcoded values
                System.out.println("[DEBUG_LOG] Using hardcoded connection properties");
                return DriverManager
                        .getConnection("jdbc:postgresql://localhost:5432/postgres", "$POSTGRES_USER", "$POSTGRES_PASSWORD");
            }
        } catch (SQLException e) {
            System.out.println("[DEBUG_LOG] Database connection failed: " + e.getMessage());
            throw e;
        }
    }

    public static int totalCount(String tableName) throws SQLException {
        String query = "SELECT count(*) FROM " + tableName;
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(query)) {
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        }
    }

    public void executeFile(String path) throws IOException, SQLException {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(path);

        assert inputStream != null;
        // Use try-with-resources to ensure both the connection and statement are closed
        try (Connection connection = getConnection();
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
             Statement statement = connection.createStatement()) {

            StringBuilder builder = new StringBuilder();

            String line;
            int lineNumber = 0;
            int count = 0;

            while ((line = bufferedReader.readLine()) != null) {
                lineNumber += 1;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("--"))
                    continue;

                builder.append(line);
                if (line.endsWith(";"))
                    try {
                        statement.execute(builder.toString());
                        System.err.println(
                                ++count
                                        + " Command successfully executed : "
                                        + builder.substring(
                                        0,
                                        Math.min(builder.length(), 15))
                                        + "...");
                        builder.setLength(0);
                    } catch (SQLException e) {
                        System.err.println(
                                "At line " + lineNumber + " : "
                                        + e.getMessage() + "\n");
                        return;
                    }
            }
        }
    }
}
