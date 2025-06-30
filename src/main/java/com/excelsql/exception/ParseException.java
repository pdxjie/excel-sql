package com.excelsql.exception;

/**
 * @Description: 用于封装SQL语句解析过程中遇到的各种错误，如语法错误、不支持的语法等
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:44
 */
public class ParseException extends ExcelSQLException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误发生的位置（字符索引）
     */
    private int position = -1;

    /**
     * 错误发生的行号
     */
    private int lineNumber = -1;

    /**
     * 错误发生的列号
     */
    private int columnNumber = -1;

    /**
     * 期望的标记或语法
     */
    private String expected;

    /**
     * 实际遇到的标记
     */
    private String actual;

    /**
     * 构造函数 - 仅包含错误消息
     *
     * @param message 错误消息
     */
    public ParseException(String message) {
        super("PARSE_ERROR", message);
    }

    /**
     * 构造函数 - 包含错误消息和原因
     *
     * @param message 错误消息
     * @param cause 异常原因
     */
    public ParseException(String message, Throwable cause) {
        super("PARSE_ERROR", message, cause, null, "SQL_PARSER");
    }

    /**
     * 构造函数 - 包含SQL语句和位置信息
     *
     * @param message 错误消息
     * @param sql SQL语句
     * @param position 错误位置
     */
    public ParseException(String message, String sql, int position) {
        super("PARSE_ERROR", message, null, sql, "SQL_PARSER");
        this.position = position;
    }

    /**
     * 构造函数 - 完整的解析错误信息
     *
     * @param message 错误消息
     * @param sql SQL语句
     * @param position 错误位置
     * @param lineNumber 行号
     * @param columnNumber 列号
     * @param expected 期望的内容
     * @param actual 实际的内容
     */
    public ParseException(String message, String sql, int position,
                          int lineNumber, int columnNumber,
                          String expected, String actual) {
        super("PARSE_ERROR", message, null, sql, "SQL_PARSER");
        this.position = position;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.expected = expected;
        this.actual = actual;
    }

    // ========================= 静态工厂方法 =========================

    /**
     * 创建语法错误异常
     */
    public static ParseException syntaxError(String sql, int position, String message) {
        return new ParseException(
                String.format("Syntax error at position %d: %s", position, message),
                sql, position
        );
    }

    /**
     * 创建不支持的语法异常
     */
    public static ParseException unsupportedSyntax(String syntax) {
        return new ParseException(String.format("Unsupported syntax: %s", syntax));
    }

    /**
     * 创建缺少标记异常
     */
    public static ParseException missingToken(String sql, int position, String expected) {
        return new ParseException(
                String.format("Missing %s at position %d", expected, position),
                sql, position, -1, -1, expected, null
        );
    }

    /**
     * 创建意外标记异常
     */
    public static ParseException unexpectedToken(String sql, int position,
                                                 String expected, String actual) {
        return new ParseException(
                String.format("Expected %s but found %s at position %d", expected, actual, position),
                sql, position, -1, -1, expected, actual
        );
    }

    /**
     * 创建无效的列名异常
     */
    public static ParseException invalidColumnName(String columnName) {
        return new ParseException(String.format("Invalid column name: %s", columnName));
    }

    /**
     * 创建无效的表名异常
     */
    public static ParseException invalidTableName(String tableName) {
        return new ParseException(String.format("Invalid table name: %s", tableName));
    }

    /**
     * 创建无效的数据类型异常
     */
    public static ParseException invalidDataType(String dataType) {
        return new ParseException(String.format("Invalid data type: %s", dataType));
    }

    /**
     * 创建无效的函数调用异常
     */
    public static ParseException invalidFunctionCall(String functionName, String reason) {
        return new ParseException(
                String.format("Invalid function call '%s': %s", functionName, reason)
        );
    }

    /**
     * 创建空SQL语句异常
     */
    public static ParseException emptySql() {
        return new ParseException("SQL statement cannot be empty or null");
    }

    /**
     * 创建SQL语句过长异常
     */
    public static ParseException sqlTooLong(int maxLength) {
        return new ParseException(
                String.format("SQL statement too long. Maximum allowed length is %d characters", maxLength)
        );
    }

    /**
     * 创建嵌套查询过深异常
     */
    public static ParseException nestedQueryTooDeep(int maxDepth) {
        return new ParseException(
                String.format("Nested query too deep. Maximum allowed depth is %d", maxDepth)
        );
    }

    // ========================= 工具方法 =========================

    /**
     * 获取格式化的错误消息，包含位置信息
     */
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessage());

        if (getSql() != null) {
            sb.append("\nSQL: ").append(getSql());
        }

        if (position >= 0) {
            sb.append("\nPosition: ").append(position);

            // 显示错误位置的上下文
            if (getSql() != null && position < getSql().length()) {
                String context = getErrorContext(getSql(), position, 20);
                sb.append("\nContext: ").append(context);
            }
        }

        if (lineNumber > 0) {
            sb.append("\nLine: ").append(lineNumber);
        }

        if (columnNumber > 0) {
            sb.append(", Column: ").append(columnNumber);
        }

        if (expected != null) {
            sb.append("\nExpected: ").append(expected);
        }

        if (actual != null) {
            sb.append("\nActual: ").append(actual);
        }

        return sb.toString();
    }

    /**
     * 获取错误位置的上下文
     */
    private String getErrorContext(String sql, int position, int contextLength) {
        int start = Math.max(0, position - contextLength);
        int end = Math.min(sql.length(), position + contextLength);

        StringBuilder context = new StringBuilder();
        context.append(sql, start, end);

        // 添加指示符指向错误位置
        int indicatorPos = position - start;
        context.append("\n");
        for (int i = 0; i < indicatorPos; i++) {
            context.append(" ");
        }
        context.append("^");

        return context.toString();
    }

    // ========================= Getter/Setter =========================

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public void setColumnNumber(int columnNumber) {
        this.columnNumber = columnNumber;
    }

    public String getExpected() {
        return expected;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }

    public String getActual() {
        return actual;
    }

    public void setActual(String actual) {
        this.actual = actual;
    }
}
