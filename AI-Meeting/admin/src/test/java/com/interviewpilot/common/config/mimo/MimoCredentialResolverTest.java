package com.interviewpilot.common.config.mimo;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MimoCredentialResolverTest {

    @Test
    void resolveSecret_ShouldResolveRawEnvironmentVariableName() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("MIMO_API_KEY", "tp-env-key");
        MimoCredentialResolver resolver = new MimoCredentialResolver(environment);

        assertEquals("tp-env-key", resolver.resolveSecret("MIMO_API_KEY"));
    }

    @Test
    void resolveSecret_ShouldResolveBracedEnvironmentPlaceholder() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("SPRING_AI_OPENAI_API_KEY", "tp-spring-key");
        MimoCredentialResolver resolver = new MimoCredentialResolver(environment);

        assertEquals(
                "tp-spring-key",
                resolver.resolveSecret("${SPRING_AI_OPENAI_API_KEY:}")
        );
    }

    @Test
    void resolveSecret_ShouldKeepLiteralSecretValues() {
        MimoCredentialResolver resolver = new MimoCredentialResolver(new MockEnvironment());

        assertEquals("tp-literal-key", resolver.resolveSecret(" tp-literal-key "));
    }
}
