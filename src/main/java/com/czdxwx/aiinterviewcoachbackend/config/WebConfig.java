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

    // 从 application.yml 读取文件上传的根目录
    @Value("${app.file-storage.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // **只需保留这一条规则即可**
        // 它会将所有 /api/files/ 开头的请求，映射到您服务器上的 uploads 目录
        // 例如，/api/files/tts_audio/123.mp3 会被映射到 /uploads/tts_audio/123.mp3 文件
        Path uploadPath = Paths.get(uploadDir);
        String resourceLocation = uploadPath.toUri().toString();

        log.info("配置统一的静态资源映射: /api/files/** -> {}", resourceLocation);

        registry.addResourceHandler("/api/files/**")
                .addResourceLocations(resourceLocation);
    }
}