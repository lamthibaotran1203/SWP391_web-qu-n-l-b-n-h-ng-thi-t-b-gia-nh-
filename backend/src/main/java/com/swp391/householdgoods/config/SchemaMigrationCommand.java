package com.swp391.householdgoods.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import org.flywaydb.core.Flyway;

/** Explicit database command without web startup, bootstrap accounts or mail. */
public final class SchemaMigrationCommand {

    private SchemaMigrationCommand() {}

    public static int execute(boolean migrate) {
        // Connection errors can contain the JDBC URL. Only print safe summaries.
        System.setProperty("logging.level.root", "OFF");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");
        var loggerContext = org.slf4j.LoggerFactory.getILoggerFactory();
        if (loggerContext instanceof ch.qos.logback.classic.LoggerContext context) {
            context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
                    .setLevel(ch.qos.logback.classic.Level.OFF);
        }
        try {
            String url = requiredEnvironment("DB_URL");
            String username = requiredEnvironment("DB_USERNAME");
            String password = requiredEnvironment("DB_PASSWORD");
            Flyway flyway = Flyway.configure()
                    .dataSource(url, username, password)
                    .defaultSchema("app")
                    .schemas("app")
                    .createSchemas(true)
                    .locations("classpath:db/migration")
                    .cleanDisabled(true)
                    .baselineOnMigrate(false)
                    .validateOnMigrate(true)
                    .load();
            if (migrate) {
                var result = flyway.migrate();
                System.out.println("Flyway migrations executed: " + result.migrationsExecuted);
                flyway.validate();
            }
            var info = flyway.info();
            System.out.println("Flyway current version: "
                    + (info.current() == null ? "none" : info.current().getVersion()));
            System.out.println("Flyway pending migrations: " + info.pending().length);
            try (Connection connection = DriverManager.getConnection(url, username, password);
                    var statement = connection.createStatement()) {
                try (ResultSet result = statement.executeQuery(
                        "select current_setting('server_version'), "
                                + "(select count(*) from information_schema.tables "
                                + "where table_schema = 'app' and table_type = 'BASE TABLE')")) {
                    result.next();
                    System.out.println("PostgreSQL version: " + result.getString(1));
                    System.out.println("app tables including Flyway history: " + result.getInt(2));
                }
            }
            return 0;
        } catch (Exception exception) {
            Throwable cause = exception;
            String sqlState = null;
            while (cause != null) {
                if (cause instanceof SQLException sqlException) {
                    sqlState = sqlException.getSQLState();
                }
                cause = cause.getCause();
            }
            System.err.println("Schema command failed: " + exception.getClass().getSimpleName()
                    + (sqlState == null ? "" : " (SQLSTATE " + sqlState + ")"));
            return 1;
        }
    }

    public static boolean handles(String[] args) {
        return Arrays.asList(args).contains("--migrate-schema")
                || Arrays.asList(args).contains("--schema-info");
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required environment variable: " + name);
        }
        return value;
    }
}
