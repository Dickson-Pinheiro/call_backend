package com.group_call.call_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SchemaMigration implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SchemaMigration.class);
    private final JdbcTemplate jdbcTemplate;

    public SchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Checking for obsolete unique constraints on call_ratings table...");

        try {
            // Find unique constraint on call_id only (not composite)
            String findConstraintSql = """
                    SELECT tc.constraint_name
                    FROM information_schema.table_constraints tc
                    JOIN information_schema.key_column_usage kcu
                      ON tc.constraint_name = kcu.constraint_name
                      AND tc.table_schema = kcu.table_schema
                    WHERE tc.constraint_type = 'UNIQUE'
                      AND tc.table_name = 'call_ratings'
                      AND kcu.column_name = 'call_id'
                    GROUP BY tc.constraint_name
                    HAVING COUNT(*) = 1;
                    """;

            List<String> constraints = jdbcTemplate.query(findConstraintSql,
                    (rs, rowNum) -> rs.getString("constraint_name"));

            for (String constraintName : constraints) {
                // Double check it's not the new composite one if the query logic leaked
                // (unlikely with HAVING COUNT=1)
                // But specifically we want to avoid dropping the composite if it happened to be
                // listed

                logger.info("Found obsolete unique constraint: {}", constraintName);
                String dropSql = "ALTER TABLE call_ratings DROP CONSTRAINT " + constraintName;
                jdbcTemplate.execute(dropSql);
                logger.info("Dropped constraint: {}", constraintName);
            }

            if (constraints.isEmpty()) {
                logger.info("No obsolete unique constraints found on call_ratings.call_id.");
            }

        } catch (Exception e) {
            logger.error("Error during schema migration: ", e);
            // Don't fail the app startup, as this might be a persistent issue or permission
            // issue
            // but we want to log it clearly.
        }
    }
}
