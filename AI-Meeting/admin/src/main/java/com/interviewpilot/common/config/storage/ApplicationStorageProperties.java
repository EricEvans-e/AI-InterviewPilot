package com.interviewpilot.common.config.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Runtime storage configuration for uploads, temporary audio files and logs.
 */
@Data
@Component
@ConfigurationProperties(prefix = "interview-pilot.storage")
public class ApplicationStorageProperties {

    private String baseDir;

    private String uploadTempDir;

    private String agentFileDir;

    private String audioTempDir;

    private String logDir;

    private String recordingDir;

    public Path getBasePath() {
        return Path.of(baseDir).toAbsolutePath().normalize();
    }

    public Path getUploadTempPath() {
        return Path.of(uploadTempDir).toAbsolutePath().normalize();
    }

    public Path getAgentFilePath() {
        String path = agentFileDir == null || agentFileDir.isBlank()
                ? getBasePath().resolve("agent-files").toString()
                : agentFileDir;
        return Path.of(path).toAbsolutePath().normalize();
    }

    public Path getAudioTempPath() {
        return Path.of(audioTempDir).toAbsolutePath().normalize();
    }

    public Path getLogPath() {
        return Path.of(logDir).toAbsolutePath().normalize();
    }

    public Path getRecordingPath() {
        return Path.of(recordingDir).toAbsolutePath().normalize();
    }
}
