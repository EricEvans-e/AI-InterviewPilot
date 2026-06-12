package com.interviewpilot.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaBootstrapSqlTest {

    @Test
    void bootstrapSql_ShouldProvisionStudentAndQuestionBankTables() throws Exception {
        String sql = Files.readString(
                Path.of("src/main/resources/sql/bootstrap_schema_extension.sql"),
                StandardCharsets.UTF_8
        );

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `student_profile`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `student_target_college`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `student_target_major`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `question`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `interview_session_question`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `teacher_review`"));
        assertTrue(sql.contains("information_schema.COLUMNS"));
        assertTrue(sql.contains("COLUMN_NAME = 'content_score'"));
        assertTrue(sql.contains("COLUMN_NAME = 'session_mode'"));
    }

    @Test
    void dockerComposeBootstrap_ShouldMountSchemaExtensionSql() throws Exception {
        String devCompose = Files.readString(Path.of("../docker-compose.yml"), StandardCharsets.UTF_8);
        String prodCompose = Files.readString(Path.of("../docker-compose.prod.yml"), StandardCharsets.UTF_8);

        assertTrue(devCompose.contains("bootstrap_schema_extension.sql:/docker-entrypoint-initdb.d/07-bootstrap_schema_extension.sql:ro"));
        assertTrue(prodCompose.contains("bootstrap_schema_extension.sql:/docker-entrypoint-initdb.d/07-bootstrap_schema_extension.sql:ro"));
    }
}
