package com.interviewpilot.common.config.dotenv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalDotenvEnvironmentPostProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadDotenvFromCurrentWorkingDirectory() throws Exception {
        Path projectDir = Files.createDirectories(tempDir.resolve("project"));
        Files.writeString(projectDir.resolve(".env"), "MIMO_API_KEY=tp-from-dotenv\n");

        ConfigurableEnvironment environment = new StandardEnvironment();

        invokePostProcessor(projectDir, environment);

        assertEquals("tp-from-dotenv", environment.getProperty("MIMO_API_KEY"));
    }

    @Test
    void shouldFallbackToParentDirectoryDotenvAndKeepExistingEnvOverride() throws Exception {
        Path projectDir = Files.createDirectories(tempDir.resolve("project"));
        Path adminDir = Files.createDirectories(projectDir.resolve("admin"));
        Files.writeString(projectDir.resolve(".env"), "MIMO_API_KEY=tp-from-parent-dotenv\n");

        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(
                new MapPropertySource("testOverride", Map.of("MIMO_API_KEY", "tp-from-existing-env"))
        );

        invokePostProcessor(adminDir, environment);

        assertEquals("tp-from-existing-env", environment.getProperty("MIMO_API_KEY"));
    }

    private void invokePostProcessor(Path workingDirectory, ConfigurableEnvironment environment) throws Exception {
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", workingDirectory.toString());
            EnvironmentPostProcessor processor = (EnvironmentPostProcessor) Class
                    .forName("com.interviewpilot.common.config.dotenv.LocalDotenvEnvironmentPostProcessor")
                    .getDeclaredConstructor()
                    .newInstance();
            processor.postProcessEnvironment(environment, new SpringApplication());
        } finally {
            restoreUserDir(originalUserDir);
        }
    }

    private void restoreUserDir(String originalUserDir) throws IOException {
        if (originalUserDir == null) {
            System.clearProperty("user.dir");
            return;
        }
        System.setProperty("user.dir", originalUserDir);
    }
}
