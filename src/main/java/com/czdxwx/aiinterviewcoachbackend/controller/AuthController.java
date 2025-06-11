package com.czdxwx.aiinterviewcoachbackend.controller;

import com.czdxwx.aiinterviewcoachbackend.entity.User;
import com.czdxwx.aiinterviewcoachbackend.service.AuthService;
import com.czdxwx.aiinterviewcoachbackend.service.dto.AuthResponseDto;
import com.czdxwx.aiinterviewcoachbackend.service.dto.LoginRequestDto;
import com.czdxwx.aiinterviewcoachbackend.service.dto.RegisterRequestDto;
import com.czdxwx.aiinterviewcoachbackend.service.impl.AuthServiceImpl;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequestDto registerRequestDto) {
        log.info("接收到注册请求: username={}, email={}", registerRequestDto.getUsername(), registerRequestDto.getEmail());
        try {
            User registeredUser = authService.register(registerRequestDto);
            // 可以选择不返回完整的 User 对象，只返回成功消息
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "用户注册成功！请登录。"));
        } catch (IllegalArgumentException e) {
            log.warn("注册失败 (IllegalArgumentException): {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) { // 更广泛的运行时错误
            log.error("注册时发生运行时错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "注册服务内部错误，请稍后再试。"));
        }
    }

    // login 方法保持不变
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        log.info("接收到登录请求: {}", loginRequestDto.getUsernameOrEmail());
        try {
            AuthResponseDto authResponse = authService.login(loginRequestDto);
            log.info("用户 {} 登录成功", authResponse.getUsername());
            return ResponseEntity.ok(authResponse);
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.warn("登录认证失败 for {}: {}", loginRequestDto.getUsernameOrEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "登录失败：用户名或密码错误。"));
        } catch (RuntimeException e) {
            log.error("登录业务逻辑处理时发生运行时错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("登录时发生未知错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "登录过程中发生未知错误。"));
        }
    }

}
