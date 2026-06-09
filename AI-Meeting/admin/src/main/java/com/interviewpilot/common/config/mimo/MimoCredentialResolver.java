package com.interviewpilot.common.config.mimo;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class MimoCredentialResolver {

    private static final Pattern ENV_NAME_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");
    private static final Pattern BRACED_PLACEHOLDER_PATTERN = Pattern.compile("^\\$\\{([^}:]+)(?::([^}]*))?}$");

    private final Environment environment;

    public String resolveSecret(String configuredValue) {
        String value = StrUtil.trimToEmpty(configuredValue);
        if (StrUtil.isBlank(value)) {
            return "";
        }

        Matcher bracedPlaceholder = BRACED_PLACEHOLDER_PATTERN.matcher(value);
        if (bracedPlaceholder.matches()) {
            String resolved = environment.getProperty(bracedPlaceholder.group(1));
            if (StrUtil.isNotBlank(resolved)) {
                return resolved.trim();
            }
            return StrUtil.trimToEmpty(bracedPlaceholder.group(2));
        }

        String resolvedPlaceholders = environment.resolvePlaceholders(value);
        if (!value.equals(resolvedPlaceholders)) {
            return StrUtil.trimToEmpty(resolvedPlaceholders);
        }

        if (ENV_NAME_PATTERN.matcher(value).matches()) {
            return StrUtil.trimToEmpty(environment.getProperty(value));
        }

        return value;
    }
}
