package com.excel.sql.engine.exception;

import lombok.Getter;

/**
 * 文件处理异常
 */
@Getter
public class FileProcessingException extends RuntimeException {
    
    /**
     * 错误码
     */
    private final String errorCode;
    
    /**
     * 构造函数
     *
     * @param message 错误信息
     */
    public FileProcessingException(String message) {
        super(message);
        this.errorCode = "FILE_PROCESSING_ERROR";
    }
    
    /**
     * 构造函数
     *
     * @param message 错误信息
     * @param errorCode 错误码
     */
    public FileProcessingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 构造函数
     *
     * @param message 错误信息
     * @param cause 原因
     */
    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "FILE_PROCESSING_ERROR";
    }
    
    /**
     * 构造函数
     *
     * @param message 错误信息
     * @param cause 原因
     * @param errorCode 错误码
     */
    public FileProcessingException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * 文件不存在错误
     *
     * @param fileName 文件名
     * @return 文件不存在错误异常
     */
    public static FileProcessingException fileNotFound(String fileName) {
        return new FileProcessingException("文件不存在: " + fileName, "FILE_NOT_FOUND");
    }
    
    /**
     * 文件格式错误
     *
     * @param fileName 文件名
     * @return 文件格式错误异常
     */
    public static FileProcessingException invalidFileFormat(String fileName) {
        return new FileProcessingException("文件格式错误: " + fileName, "INVALID_FILE_FORMAT");
    }
    
    /**
     * 文件已存在错误
     *
     * @param fileName 文件名
     * @return 文件已存在错误异常
     */
    public static FileProcessingException fileAlreadyExists(String fileName) {
        return new FileProcessingException("文件已存在: " + fileName, "FILE_ALREADY_EXISTS");
    }
    
    /**
     * 文件读取错误
     *
     * @param fileName 文件名
     * @param cause 原因
     * @return 文件读取错误异常
     */
    public static FileProcessingException readError(String fileName, Throwable cause) {
        return new FileProcessingException("文件读取错误: " + fileName, cause, "FILE_READ_ERROR");
    }
    
    /**
     * 文件写入错误
     *
     * @param fileName 文件名
     * @param cause 原因
     * @return 文件写入错误异常
     */
    public static FileProcessingException writeError(String fileName, Throwable cause) {
        return new FileProcessingException("文件写入错误: " + fileName, cause, "FILE_WRITE_ERROR");
    }
} 