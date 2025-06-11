package com.czdxwx.aiinterviewcoachbackend.exception;

import com.czdxwx.aiinterviewcoachbackend.service.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    /**
     * 处理 @Valid 注解校验失败的异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", HttpStatus.BAD_REQUEST.value());

        // 获取第一个校验失败的字段和它的错误信息
        FieldError firstError = ex.getBindingResult().getFieldError();
        if (firstError != null) {
            body.put("error", "输入数据验证失败");
            body.put("message", firstError.getDefaultMessage()); // 例如：“标题长度需在5到100个字符之间”
            body.put("field", firstError.getField()); // 哪个字段出错了
        } else {
            body.put("error", "请求参数无效");
        }

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理业务逻辑中的非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "无效的请求");
        body.put("message", ex.getMessage()); // 直接使用 Service 层抛出的中文错误信息
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }



    /**
     * 处理特定类型的异常，例如“用户不存在”
     * @param e 捕获到的 NoSuchElementException
     * @param request Spring 自动注入的请求对象
     * @return 返回 404 Not Found 状态
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElementException(NoSuchElementException e, HttpServletRequest request) {
        logger.warn("Resource not found for request {}: {}", request.getRequestURI(), e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                e.getClass().getSimpleName(),
                e.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * 处理所有其他未被捕获的通用异常
     * @param e 捕获到的通用 Exception
     * @param request Spring 自动注入的请求对象
     * @return 返回 500 Internal Server Error 状态
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception e, HttpServletRequest request) {
        logger.error("Unhandled exception for request {}: {}", request.getRequestURI(), e.getMessage(), e);

        Throwable rootCause = (e instanceof ExecutionException) ? e.getCause() : e;
        String errorMessage = (rootCause instanceof TimeoutException) ? "服务调用超时，请稍后重试。" : rootCause.getMessage();
        if (errorMessage == null) {
            errorMessage = "服务器内部发生未知错误。";
        }

        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                rootCause.getClass().getSimpleName(),
                errorMessage,
                request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}