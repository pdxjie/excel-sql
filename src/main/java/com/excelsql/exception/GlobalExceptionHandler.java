package com.excelsql.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: 统一处理系统中的各种异常，提供标准化的错误响应格式
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:44
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ========================= Excel-SQL 相关异常 =========================

    /**
     * 处理自定义Excel-SQL异常
     */
    @ExceptionHandler(ExcelSQLException.class)
    public ResponseEntity<ErrorResponse> handleExcelSQLException(ExcelSQLException e,
                                                                 HttpServletRequest request) {
        logger.error("Excel-SQL Error: {}", e.getMessage(), e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode(e.getErrorCode())
                .message(e.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .sql(e.getSql())
                .component(e.getComponent())
                .details(e.getErrorDetails())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 处理SQL解析异常
     */
    @ExceptionHandler(ParseException.class)
    public ResponseEntity<ErrorResponse> handleParseException(ParseException e,
                                                              HttpServletRequest request) {
        logger.error("SQL Parse Error: {}", e.getFormattedMessage(), e);

        Map<String, Object> parseDetails = new HashMap<>();
        parseDetails.put("position", e.getPosition());
        parseDetails.put("lineNumber", e.getLineNumber());
        parseDetails.put("columnNumber", e.getColumnNumber());
        parseDetails.put("expected", e.getExpected());
        parseDetails.put("actual", e.getActual());
        parseDetails.put("formattedMessage", e.getFormattedMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode(e.getErrorCode())
                .message(e.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .sql(e.getSql())
                .component(e.getComponent())
                .details(parseDetails)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // ========================= Spring 框架异常 =========================

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e,
                                                                   HttpServletRequest request) {
        logger.warn("Validation error: {}", e.getMessage());

        List<String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("VALIDATION_ERROR")
                .message("请求参数验证失败")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .details(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e,
                                                             HttpServletRequest request) {
        logger.warn("Bind error: {}", e.getMessage());

        List<String> errors = e.getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("BIND_ERROR")
                .message("数据绑定失败")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .details(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(MissingServletRequestParameterException e,
                                                                         HttpServletRequest request) {
        logger.warn("Missing parameter: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("MISSING_PARAMETER")
                .message(String.format("缺少必需的请求参数: %s", e.getParameterName()))
                .path(request.getRequestURI())
                .method(request.getMethod())
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .details(Map.of("parameterName", e.getParameterName(), "parameterType", e.getParameterType()))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 处理方法参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException e,
                                                                     HttpServletRequest request) {
        logger.warn("Type mismatch: {}", e.getMessage());

        String message = String.format("参数 '%s' 的值 '%s' 不能转换为 %s 类型",
                e.getName(), e.getValue(), e.getRequiredType().getSimpleName());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("TYPE_MISMATCH")
                .message(message)
                .path(request.getRequestURI())
                .method(request.getMethod())
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .details(Map.of(
                        "parameterName", e.getName(),
                        "providedValue", Objects.toString(e.getValue(), "null"),
                        "requiredType", e.getRequiredType().getSimpleName()
                ))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 处理HTTP消息不可读异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e,
                                                                               HttpServletRequest request) {
        logger.warn("HTTP message not readable: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("MESSAGE_NOT_READABLE")
                .message("请求体格式错误或不可读")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .details(Map.of("originalMessage", e.getMostSpecificCause().getMessage()))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 处理不支持的HTTP方法异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e,
                                                                           HttpServletRequest request) {
        logger.warn("Method not supported: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("METHOD_NOT_SUPPORTED")
                .message(String.format("不支持的HTTP方法: %s", e.getMethod()))
                .path(request.getRequestURI())
                .method(request.getMethod())
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .details(Map.of(
                        "supportedMethods", Arrays.toString(e.getSupportedMethods()),
                        "requestedMethod", e.getMethod()
                ))
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException e,
                                                                       HttpServletRequest request) {
        logger.warn("No handler found: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("NOT_FOUND")
                .message(String.format("未找到请求的资源: %s %s", e.getHttpMethod(), e.getRequestURL()))
                .path(request.getRequestURI())
                .method(request.getMethod())
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e,
                                                                              HttpServletRequest request) {
        logger.warn("Upload size exceeded: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("UPLOAD_SIZE_EXCEEDED")
                .message("上传文件大小超过限制")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .details(Map.of("maxUploadSize", e.getMaxUploadSize()))
                .build();

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    // ========================= 系统异常 =========================

    /**
     * 处理IO异常
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException e,
                                                           HttpServletRequest request) {
        logger.error("IO error: {}", e.getMessage(), e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("IO_ERROR")
                .message("文件操作失败")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .details(Map.of("originalMessage", e.getMessage()))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 处理内存不足异常
     */
    @ExceptionHandler(OutOfMemoryError.class)
    public ResponseEntity<ErrorResponse> handleOutOfMemoryError(OutOfMemoryError e,
                                                                HttpServletRequest request) {
        logger.error("Out of memory: {}", e.getMessage(), e);

        // 尝试强制垃圾回收
        System.gc();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("OUT_OF_MEMORY")
                .message("系统内存不足，请减少数据量或联系管理员")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .build();

        return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE).body(errorResponse);
    }

    /**
     * 处理其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e,
                                                                HttpServletRequest request) {
        logger.error("Unexpected error: {}", e.getMessage(), e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode("INTERNAL_ERROR")
                .message("系统内部错误，请稍后重试")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .details(Map.of(
                        "exceptionType", e.getClass().getSimpleName(),
                        "originalMessage", e.getMessage()
                ))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // ========================= 错误响应类 =========================

    /**
     * 标准化错误响应格式
     */
    public static class ErrorResponse {
        private boolean success;
        private String errorCode;
        private String message;
        private String path;
        private String method;
        private String timestamp;
        private String sql;
        private String component;
        private Object details;
        private String traceId;

        // 构造函数
        public ErrorResponse() {}

        public ErrorResponse(boolean success, String errorCode, String message) {
            this.success = success;
            this.errorCode = errorCode;
            this.message = message;
        }

        // Builder模式
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ErrorResponse response = new ErrorResponse();

            public Builder success(boolean success) {
                response.success = success;
                return this;
            }

            public Builder errorCode(String errorCode) {
                response.errorCode = errorCode;
                return this;
            }

            public Builder message(String message) {
                response.message = message;
                return this;
            }

            public Builder path(String path) {
                response.path = path;
                return this;
            }

            public Builder method(String method) {
                response.method = method;
                return this;
            }

            public Builder timestamp(String timestamp) {
                response.timestamp = timestamp;
                return this;
            }

            public Builder sql(String sql) {
                response.sql = sql;
                return this;
            }

            public Builder component(String component) {
                response.component = component;
                return this;
            }

            public Builder details(Object details) {
                response.details = details;
                return this;
            }

            public Builder traceId(String traceId) {
                response.traceId = traceId;
                return this;
            }

            public ErrorResponse build() {
                return response;
            }
        }

        // Getter/Setter methods
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public String getSql() { return sql; }
        public void setSql(String sql) { this.sql = sql; }

        public String getComponent() { return component; }
        public void setComponent(String component) { this.component = component; }

        public Object getDetails() { return details; }
        public void setDetails(Object details) { this.details = details; }

        public String getTraceId() { return traceId; }
        public void setTraceId(String traceId) { this.traceId = traceId; }
    }
}
