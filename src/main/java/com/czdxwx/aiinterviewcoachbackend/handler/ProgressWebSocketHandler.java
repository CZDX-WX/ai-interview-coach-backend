package com.czdxwx.aiinterviewcoachbackend.handler;

import com.czdxwx.aiinterviewcoachbackend.service.dto.ProgressUpdateDTO;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProgressWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProgressWebSocketHandler.class);
    // 使用一个线程安全的Map来存储 taskId 和对应的 WebSocketSession
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从连接的URI中获取 taskId，例如 ws://.../ws/progress?taskId=xyz
        String taskId = getTaskId(session);
        if (taskId != null) {
            sessions.put(taskId, session);
            logger.info("WebSocket connection established for taskId: {}. SessionId: {}", taskId, session.getId());
        } else {
            logger.warn("WebSocket connection established without a taskId. SessionId: {}", session.getId());
        }
    }

    /**
     * 这是最核心的公共方法，由我们的后台任务调用，用于向前端推送进度
     */
    public void sendProgressUpdate(ProgressUpdateDTO update) {
        WebSocketSession session = sessions.get(update.getTaskId());
        if (session != null && session.isOpen()) {
            try {
                logger.info("Sending progress update for taskId {}: {}%", update.getTaskId(), update.getProgress());
                session.sendMessage(new TextMessage(gson.toJson(update)));
            } catch (IOException e) {
                logger.error("Failed to send progress update for taskId: {}", update.getTaskId(), e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        String taskId = getTaskId(session);
        if (taskId != null) {
            sessions.remove(taskId);
            logger.info("WebSocket connection closed for taskId: {}. SessionId: {}", taskId, session.getId());
        }
    }

    private String getTaskId(WebSocketSession session) {
        // 从 WebSocket 连接的查询参数中解析出 taskId
        String query = session.getUri().getQuery();
        if (query != null && query.startsWith("taskId=")) {
            return query.substring("taskId=".length());
        }
        return null;
    }
}
