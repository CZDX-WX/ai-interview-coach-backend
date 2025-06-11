package com.czdxwx.aiinterviewcoachbackend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Logger log = LoggerFactory.getLogger(WebConfig.class);

    @Value("${app.file-storage.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /api/files/** 的URL请求映射到文件系统上的 uploadDir 目录
        String resourceLocation = "file:" + uploadDir.replace("\\", "/") + "/";

        log.info("修正后的静态资源映射: /api/files/** -> {}", resourceLocation);

        registry.addResourceHandler("/api/files/**")
                .addResourceLocations(resourceLocation);
    }
}