package com.excel.sql.engine.model.excel;

/**
 * Excel数据类型枚举
 */
public enum ExcelDataType {
    /**
     * 字符串类型
     */
    STRING,
    
    /**
     * 数字类型（整数或浮点数）
     */
    NUMBER,
    
    /**
     * 整数类型
     */
    INTEGER,
    
    /**
     * 小数类型
     */
    DECIMAL,
    
    /**
     * 日期类型
     */
    DATE,
    
    /**
     * 日期时间类型
     */
    DATETIME,
    
    /**
     * 布尔类型
     */
    BOOLEAN,
    
    /**
     * 自动检测类型
     */
    AUTO,
    
    /**
     * 未知类型
     */
    UNKNOWN;
    
    /**
     * 根据单元格值推断数据类型
     *
     * @param value 单元格值
     * @return 推断的数据类型
     */
    public static ExcelDataType inferType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return STRING;
        }
        
        // 尝试解析为布尔值
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false") || 
            value.equals("1") || value.equals("0") || 
            value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("no") ||
            value.equalsIgnoreCase("y") || value.equalsIgnoreCase("n")) {
            return BOOLEAN;
        }
        
        // 尝试解析为数字
        try {
            Double.parseDouble(value);
            return NUMBER;
        } catch (NumberFormatException ignored) {
            // 不是数字，继续检查其他类型
        }
        
        // 尝试解析为日期时间
        try {
            // 常见的日期时间格式
            String[] dateTimePatterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy/MM/dd HH:mm:ss",
                "dd-MM-yyyy HH:mm:ss",
                "dd/MM/yyyy HH:mm:ss",
                "MM-dd-yyyy HH:mm:ss",
                "MM/dd/yyyy HH:mm:ss"
            };
            
            for (String pattern : dateTimePatterns) {
                try {
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(pattern);
                    java.time.LocalDateTime.parse(value, formatter);
                    return DATETIME;
                } catch (Exception ignored) {
                    // 尝试下一个模式
                }
            }
        } catch (Exception ignored) {
            // 不是日期时间，继续检查其他类型
        }
        
        // 尝试解析为日期
        try {
            // 常见的日期格式
            String[] datePatterns = {
                "yyyy-MM-dd",
                "yyyy/MM/dd",
                "dd-MM-yyyy",
                "dd/MM/yyyy",
                "MM-dd-yyyy",
                "MM/dd/yyyy"
            };
            
            for (String pattern : datePatterns) {
                try {
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(pattern);
                    java.time.LocalDate.parse(value, formatter);
                    return DATE;
                } catch (Exception ignored) {
                    // 尝试下一个模式
                }
            }
        } catch (Exception ignored) {
            // 不是日期，默认为字符串
        }
        
        // 默认为字符串类型
        return STRING;
    }
    
    /**
     * 获取对应的Java类型
     *
     * @return Java类型
     */
    public Class<?> getJavaType() {
        switch (this) {
            case STRING:
                return String.class;
            case NUMBER:
                return Double.class;
            case INTEGER:
                return Long.class;
            case DECIMAL:
                return Double.class;
            case DATE:
                return java.time.LocalDate.class;
            case DATETIME:
                return java.time.LocalDateTime.class;
            case BOOLEAN:
                return Boolean.class;
            case AUTO:
                return Object.class;
            default:
                return Object.class;
        }
    }
    
    /**
     * 获取对应的SQL类型
     *
     * @return SQL类型名称
     */
    public String getSqlTypeName() {
        switch (this) {
            case STRING:
                return "VARCHAR";
            case NUMBER:
                return "DOUBLE";
            case INTEGER:
                return "INTEGER";
            case DECIMAL:
                return "DECIMAL";
            case DATE:
                return "DATE";
            case DATETIME:
                return "DATETIME";
            case BOOLEAN:
                return "BOOLEAN";
            case AUTO:
                return "VARCHAR";
            default:
                return "VARCHAR";
        }
    }
} 