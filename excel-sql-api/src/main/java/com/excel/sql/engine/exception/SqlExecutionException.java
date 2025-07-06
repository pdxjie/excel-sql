package com.excel.sql.engine.exception;

import lombok.Getter;

/**
 * SQL执行异常
 */
@Getter
public class SqlExecutionException extends RuntimeException {
    
    /**
     * 错误码
     */
    private final String errorCode;
    
    /**
     * 构造函数
     *
     * @param message 错误信息
     */
    public SqlExecutionException(String message) {
        super(message);
        this.errorCode = "SQL_EXECUTION_ERROR";
    }
    
    /**
     * 构造函数
     *
     * @param message 错误信息
     * @param errorCode 错误码
     */
    public SqlExecutionException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 构造函数
     *
     * @param message 错误信息
     * @param cause 原因
     */
    public SqlExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "SQL_EXECUTION_ERROR";
    }
    
    /**
     * 构造函数
     *
     * @param message 错误信息
     * @param cause 原因
     * @param errorCode 错误码
     */
    public SqlExecutionException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * SQL语法错误
     *
     * @param message 错误信息
     * @return SQL语法错误异常
     */
    public static SqlExecutionException syntaxError(String message) {
        return new SqlExecutionException(message, "SQL_SYNTAX_ERROR");
    }
    
    /**
     * 表不存在错误
     *
     * @param tableName 表名
     * @return 表不存在错误异常
     */
    public static SqlExecutionException tableNotFound(String tableName) {
        return new SqlExecutionException("表不存在: " + tableName, "TABLE_NOT_FOUND");
    }
    
    /**
     * 列不存在错误
     *
     * @param columnName 列名
     * @return 列不存在错误异常
     */
    public static SqlExecutionException columnNotFound(String columnName) {
        return new SqlExecutionException("列不存在: " + columnName, "COLUMN_NOT_FOUND");
    }
    
    /**
     * 查询超时错误
     *
     * @param timeoutSeconds 超时秒数
     * @return 查询超时错误异常
     */
    public static SqlExecutionException queryTimeout(int timeoutSeconds) {
        return new SqlExecutionException("查询超时: " + timeoutSeconds + "秒", "QUERY_TIMEOUT");
    }
} 