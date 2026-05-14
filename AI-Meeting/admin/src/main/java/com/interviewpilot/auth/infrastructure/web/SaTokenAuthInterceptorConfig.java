package com.interviewpilot.auth.infrastructure.web;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenAuthInterceptorConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> SaRouter
                .match("/api/ip/v1/**")
                .notMatch("/api/ip/v1/users/login")
                .notMatch("/api/ip/v1/users/register")
                .notMatch("/api/ip/v1/users/has-username")
                .notMatch(("/api/ip/v1/users/check-login"))
                .notMatch("/api/ip/v1/users/send-sms-code")
                .notMatch("/api/ip/v1/users/phone-login")
                .notMatch("/api/ip/v1/ai/doubao/**")
                .notMatch("/api/ip/v1/ai/roleplay/**")
                .check(StpUtil::checkLogin)
        )).addPathPatterns("/**");
    }
}
