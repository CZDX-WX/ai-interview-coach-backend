package com.czdxwx.aiinterviewcoachbackend.config;

import com.czdxwx.aiinterviewcoachbackend.handler.ProgressWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket // 开启 WebSocket 功能
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ProgressWebSocketHandler progressWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册我们的处理器，路径为 /ws/progress
        // setAllowedOrigins("*") 允许跨域连接
        registry.addHandler(progressWebSocketHandler, "/ws/progress").setAllowedOrigins("*");
    }
}