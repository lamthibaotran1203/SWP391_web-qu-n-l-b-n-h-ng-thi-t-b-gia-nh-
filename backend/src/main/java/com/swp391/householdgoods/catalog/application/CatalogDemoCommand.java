package com.swp391.householdgoods.catalog.application;

import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;

/** Explicit seed entry point; no credentials/accounts are created. */
public final class CatalogDemoCommand {
    private CatalogDemoCommand() {}
    public static int execute() {
        var context=org.slf4j.LoggerFactory.getILoggerFactory();
        if(context instanceof ch.qos.logback.classic.LoggerContext logs)
            logs.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).setLevel(ch.qos.logback.classic.Level.OFF);
        try(var input=CatalogDemoCommand.class.getResourceAsStream("/demo/catalog.sql")) {
            if(input==null) throw new IllegalStateException("Demo resource missing");
            String sql=new String(input.readAllBytes(),StandardCharsets.UTF_8);
            try(var connection=DriverManager.getConnection(System.getenv("DB_URL"),
                    System.getenv("DB_USERNAME"),System.getenv("DB_PASSWORD"))) {
                connection.setAutoCommit(false);
                try(var statement=connection.createStatement()) {
                    statement.execute(sql);
                    connection.commit();
                    System.out.println("Catalog demo seed complete (idempotent; no existing data overwritten).");
                } catch(Exception error) { connection.rollback(); throw error; }
            }
            return 0;
        } catch(Exception error) {
            System.err.println("Catalog demo seed failed: "+error.getClass().getSimpleName());
            return 1;
        }
    }
}
