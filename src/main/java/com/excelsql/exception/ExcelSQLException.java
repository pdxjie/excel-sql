package com.excelsql.exception;

/**
 * @Description: 这是系统中所有业务异常的基类，用于封装Excel-SQL操作过程中的各种错误情况
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:44
 */
public class ExcelSQLException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误代码
     */
    private String errorCode;

    /**
     * 错误详细信息
     */
    private Object errorDetails;

    /**
     * SQL语句（如果适用）
     */
    private String sql;

    /**
     * 错误发生的组件
     */
    private String component;

    /**
     * 构造函数 - 仅包含错误消息
     *
     * @param message 错误消息
     */
    public ExcelSQLException(String message) {
        super(message);
        this.errorCode = "EXCEL_SQL_ERROR";
    }

    /**
     * 构造函数 - 包含错误消息和原因
     *
     * @param message 错误消息
     * @param cause 异常原因
     */
    public ExcelSQLException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "EXCEL_SQL_ERROR";
    }

    /**
     * 构造函数 - 包含错误代码和消息
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     */
    public ExcelSQLException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造函数 - 完整的错误信息
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param cause 异常原因
     * @param sql 相关的SQL语句
     * @param component 发生错误的组件
     */
    public ExcelSQLException(String errorCode, String message, Throwable cause,
                             String sql, String component) {
        super(message, cause);
        this.errorCode = errorCode;
        this.sql = sql;
        this.component = component;
    }

    // ========================= 静态工厂方法 =========================

    /**
     * 创建工作簿相关异常
     */
    public static ExcelSQLException workbookError(String message) {
        return new ExcelSQLException("WORKBOOK_ERROR", message);
    }

    /**
     * 创建工作簿相关异常（带原因）
     */
    public static ExcelSQLException workbookError(String message, Throwable cause) {
        ExcelSQLException ex = new ExcelSQLException("WORKBOOK_ERROR", message);
        ex.initCause(cause);
        return ex;
    }

    /**
     * 创建Sheet相关异常
     */
    public static ExcelSQLException sheetError(String message) {
        return new ExcelSQLException("SHEET_ERROR", message);
    }

    /**
     * 创建Sheet相关异常（带原因）
     */
    public static ExcelSQLException sheetError(String message, Throwable cause) {
        ExcelSQLException ex = new ExcelSQLException("SHEET_ERROR", message);
        ex.initCause(cause);
        return ex;
    }

    /**
     * 创建数据操作相关异常
     */
    public static ExcelSQLException dataError(String message) {
        return new ExcelSQLException("DATA_ERROR", message);
    }

    /**
     * 创建数据操作相关异常（带原因）
     */
    public static ExcelSQLException dataError(String message, Throwable cause) {
        ExcelSQLException ex = new ExcelSQLException("DATA_ERROR", message);
        ex.initCause(cause);
        return ex;
    }

    /**
     * 创建缓存相关异常
     */
    public static ExcelSQLException cacheError(String message) {
        return new ExcelSQLException("CACHE_ERROR", message);
    }

    /**
     * 创建缓存相关异常（带原因）
     */
    public static ExcelSQLException cacheError(String message, Throwable cause) {
        ExcelSQLException ex = new ExcelSQLException("CACHE_ERROR", message);
        ex.initCause(cause);
        return ex;
    }

    /**
     * 创建索引相关异常
     */
    public static ExcelSQLException indexError(String message) {
        return new ExcelSQLException("INDEX_ERROR", message);
    }

    /**
     * 创建索引相关异常（带原因）
     */
    public static ExcelSQLException indexError(String message, Throwable cause) {
        ExcelSQLException ex = new ExcelSQLException("INDEX_ERROR", message);
        ex.initCause(cause);
        return ex;
    }

    /**
     * 创建函数执行相关异常
     */
    public static ExcelSQLException functionError(String functionName, String message) {
        return new ExcelSQLException("FUNCTION_ERROR",
                String.format("Function '%s' error: %s", functionName, message));
    }

    /**
     * 创建函数执行相关异常（带原因）
     */
    public static ExcelSQLException functionError(String functionName, String message, Throwable cause) {
        ExcelSQLException ex = new ExcelSQLException("FUNCTION_ERROR",
                String.format("Function '%s' error: %s", functionName, message));
        ex.initCause(cause);
        return ex;
    }

    /**
     * 创建配置相关异常
     */
    public static ExcelSQLException configError(String message) {
        return new ExcelSQLException("CONFIG_ERROR", message);
    }

    /**
     * 创建权限相关异常
     */
    public static ExcelSQLException permissionError(String message) {
        return new ExcelSQLException("PERMISSION_ERROR", message);
    }

    /**
     * 创建资源不足异常
     */
    public static ExcelSQLException resourceError(String message) {
        return new ExcelSQLException("RESOURCE_ERROR", message);
    }

    // ========================= Getter/Setter =========================

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Object getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(Object errorDetails) {
        this.errorDetails = errorDetails;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    /**
     * 创建构建器模式的异常
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 异常构建器
     */
    public static class Builder {
        private String errorCode = "EXCEL_SQL_ERROR";
        private String message;
        private Throwable cause;
        private String sql;
        private String component;
        private Object errorDetails;

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public Builder sql(String sql) {
            this.sql = sql;
            return this;
        }

        public Builder component(String component) {
            this.component = component;
            return this;
        }

        public Builder details(Object errorDetails) {
            this.errorDetails = errorDetails;
            return this;
        }

        public ExcelSQLException build() {
            ExcelSQLException ex = new ExcelSQLException(errorCode, message, cause, sql, component);
            ex.setErrorDetails(errorDetails);
            return ex;
        }
    }
}
