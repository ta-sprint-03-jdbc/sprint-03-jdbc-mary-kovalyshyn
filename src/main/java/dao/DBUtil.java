package dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public class DBUtil {
    private static HikariDataSource dataSource;

    // Default pool configuration values
    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final int DEFAULT_MIN_IDLE = 2;
    private static final int DEFAULT_IDLE_TIMEOUT = 30000;
    private static final int DEFAULT_CONNECTION_TIMEOUT = 10000;

    // Initialize the connection pool
    static {
        try {
            initializeDataSource();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutting down connection pool");
                closeDataSource();
            }));
        } catch (Exception e) {
            log.error("Failed to initialize connection pool: {}", e.getMessage());
        }
    }

    private static void initializeDataSource() {
        try {
            // Check if we're running in GitHub Actions
            String dbName = System.getenv("POSTGRES_DB");
            String dbUser = System.getenv("POSTGRES_USER");
            String dbPassword = System.getenv("POSTGRES_PASSWORD");

            if (dbName != null && dbUser != null && dbPassword != null) {
                // We're running in GitHub Actions
                log.info("Initializing connection pool for GitHub Actions");
                HikariConfig config = createDefaultConfig();
                config.setJdbcUrl("jdbc:postgresql://localhost:5432/" + dbName);
                config.setUsername(dbUser);
                config.setPassword(dbPassword);
                dataSource = new HikariDataSource(config);
                return;
            }

            // Try to load from properties file
            ClassLoader classLoader = DBUtil.class.getClassLoader();
            URL resource = classLoader.getResource("db.properties");

            if (resource != null) {
                try {
                    log.info("Using HikariCP connection pool with db.properties");
                    String configFile = resource.getFile();
                    HikariConfig config = new HikariConfig(configFile);
                    applyDefaultPoolSettings(config);
                    dataSource = new HikariDataSource(config);
                    return;
                } catch (Exception e) {
                    log.warn("HikariCP with properties failed: {}, using fallback connection pool", e.getMessage());
                }
            } else {
                log.info("db.properties file not found, using fallback connection pool");
            }

            // Fallback to default configuration
            HikariConfig config = createDefaultConfig();
            config.setJdbcUrl("jdbc:postgresql://localhost:5432/example");
            config.setUsername("$POSTGRES_USER");
            config.setPassword("$POSTGRES_PASSWORD");
            dataSource = new HikariDataSource(config);

        } catch (Exception e) {
            log.error("Failed to initialize connection pool: {}", e.getMessage());
            throw e;
        }
    }

    private static HikariConfig createDefaultConfig() {
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(DEFAULT_MAX_POOL_SIZE);
        config.setMinimumIdle(DEFAULT_MIN_IDLE);
        config.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
        config.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
        return config;
    }

    private static void applyDefaultPoolSettings(HikariConfig config) {
        if (config.getMaximumPoolSize() <= 0) {
            config.setMaximumPoolSize(DEFAULT_MAX_POOL_SIZE);
        }
        if (config.getMinimumIdle() <= 0) {
            config.setMinimumIdle(DEFAULT_MIN_IDLE);
        }
        if (config.getIdleTimeout() <= 0) {
            config.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
        }
        if (config.getConnectionTimeout() <= 0) {
            config.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
        }
    }

    // Get a connection from the pool
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initializeDataSource();
        }
        return dataSource.getConnection();
    }

    // Close the connection pool
    public static void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

}
