package com.interviewpilot.common.config.user;

import com.interviewpilot.common.biz.user.UserFlowRiskControlFilter;
import com.interviewpilot.common.ratelimit.RequestRateLimitKeyResolver;
import com.interviewpilot.common.ratelimit.RequestRateLimitService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * User-related web filter configuration.
 */
@Configuration
public class UserConfiguration {

    @Bean
    @ConditionalOnProperty(name = "interview-pilot.flow-limit.enable", havingValue = "true")
    public FilterRegistrationBean<UserFlowRiskControlFilter> globalUserFlowRiskControlFilter(
            UserFlowRiskControlConfiguration userFlowRiskControlConfiguration,
            RequestRateLimitService requestRateLimitService,
            RequestRateLimitKeyResolver requestRateLimitKeyResolver) {
        FilterRegistrationBean<UserFlowRiskControlFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new UserFlowRiskControlFilter(
                userFlowRiskControlConfiguration,
                requestRateLimitService,
                requestRateLimitKeyResolver
        ));
        registration.addUrlPatterns("/*");
        registration.setOrder(10);
        return registration;
    }
}
