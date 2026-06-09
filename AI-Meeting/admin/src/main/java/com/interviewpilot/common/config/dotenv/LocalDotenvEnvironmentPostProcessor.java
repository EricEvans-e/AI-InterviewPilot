package com.interviewpilot.common.config.dotenv;

import cn.hutool.core.util.StrUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LocalDotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "localDotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> dotenvProperties = loadDotenvProperties();
        if (dotenvProperties.isEmpty()) {
            return;
        }
        environment.getPropertySources().remove(PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, dotenvProperties));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private Map<String, Object> loadDotenvProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Path candidate : resolveCandidates()) {
            if (Files.isRegularFile(candidate)) {
                readDotenvFile(candidate, properties);
            }
        }
        return properties;
    }

    private List<Path> resolveCandidates() {
        Path workingDirectory = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        Path parentDirectory = workingDirectory.getParent();
        if (parentDirectory == null) {
            return List.of(workingDirectory.resolve(".env"));
        }
        return List.of(parentDirectory.resolve(".env"), workingDirectory.resolve(".env"));
    }

    private void readDotenvFile(Path dotenvFile, Map<String, Object> properties) {
        try {
            for (String rawLine : Files.readAllLines(dotenvFile)) {
                parseLine(rawLine, properties);
            }
        } catch (IOException ignored) {
        }
    }

    private void parseLine(String rawLine, Map<String, Object> properties) {
        String line = StrUtil.trim(rawLine);
        if (StrUtil.isBlank(line) || line.startsWith("#")) {
            return;
        }
        if (line.startsWith("export ")) {
            line = StrUtil.trim(line.substring("export ".length()));
        }

        int separatorIndex = line.indexOf('=');
        if (separatorIndex <= 0) {
            return;
        }

        String key = StrUtil.trim(line.substring(0, separatorIndex));
        String value = StrUtil.trim(line.substring(separatorIndex + 1));
        if (StrUtil.isBlank(key)) {
            return;
        }
        properties.put(key, stripQuotes(value));
    }

    private String stripQuotes(String value) {
        if (value == null || value.length() < 2) {
            return StrUtil.nullToEmpty(value);
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
