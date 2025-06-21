package com.czdxwx.aiinterviewcoachbackend.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 这是一个简单的日志记录过滤器，用于调试，它会在过滤器链的最前端运行。
 */
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 【核心】只要请求到达Tomcat，这行日志就会被打印
        logger.info("====== INCOMING REQUEST ====== Method: [{}], URI: [{}]", request.getMethod(), request.getRequestURI());

        // 继续执行后续的过滤器链
        filterChain.doFilter(request, response);
    }
}