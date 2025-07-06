package com.excel.sql.engine.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理SQL执行异常
     */
    @ExceptionHandler(SqlExecutionException.class)
    public ResponseEntity<Map<String, Object>> handleSqlExecutionException(SqlExecutionException e) {
        log.error("SQL执行异常: {}", e.getMessage(), e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        response.put("errorCode", e.getErrorCode());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理文件处理异常
     */
    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleFileProcessingException(FileProcessingException e) {
        log.error("文件处理异常: {}", e.getMessage(), e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        response.put("errorCode", e.getErrorCode());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("文件上传大小超限: {}", e.getMessage(), e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "文件大小超出限制");
        response.put("errorCode", "FILE_TOO_LARGE");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException e) {
        BindingResult result = e.getBindingResult();
        String errorMessage = result.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.error("参数校验异常: {}", errorMessage);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", errorMessage);
        response.put("errorCode", "VALIDATION_ERROR");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理其他所有异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "系统异常: " + e.getMessage());
        response.put("errorCode", "SYSTEM_ERROR");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
} 