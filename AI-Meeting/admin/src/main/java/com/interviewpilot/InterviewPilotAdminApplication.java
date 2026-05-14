
package com.interviewpilot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.interviewpilot.**.dao.mapper")
@EnableScheduling
public class InterviewPilotAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(InterviewPilotAdminApplication.class, args);
    }
}
