package com.czdxwx.aiinterviewcoachbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@SpringBootApplication
@EnableTransactionManagement
public class AiInterviewCoachBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiInterviewCoachBackendApplication.class, args);
    }
}
