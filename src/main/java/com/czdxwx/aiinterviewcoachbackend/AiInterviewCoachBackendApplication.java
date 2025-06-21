package com.czdxwx.aiinterviewcoachbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.sql.ResultSet;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@SpringBootApplication
@EnableTransactionManagement
@EnableCaching
@EnableAsync // 【新增】开启异步方法执行功能
@MapperScan("com.czdxwx.aiinterviewcoachbackend.mapper") // 扫描所有 Mapper
public class AiInterviewCoachBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiInterviewCoachBackendApplication.class, args);
    }



}
