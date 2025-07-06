package com.excel.sql.engine.model.excel;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Excel列模型
 * 对应数据库中的Column概念
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelColumn {
    
    /**
     * 列ID
     */
    private Long id;

    private Integer index;
    
    /**
     * 所属工作表ID
     */
    private Long sheetId;
    
    /**
     * 列名
     */
    private String name;
    
    /**
     * 列索引（从0开始）
     */
    private Integer columnIndex;
    
    /**
     * 数据类型
     */
    private ExcelDataType dataType;
    
    /**
     * 是否可为空
     */
    private Boolean nullable;
    
    /**
     * 是否为索引列
     */
    private Boolean indexed;
    
    /**
     * 格式模式（如日期格式）
     */
    private String formatPattern;
    
    /**
     * 获取规范化列名（用于SQL查询）
     * 去除特殊字符，空格替换为下划线
     *
     * @return 规范化列名
     */
    public String getNormalizedName() {
        if (name == null) {
            return "";
        }
        
        return name.trim()
                .replaceAll("[^a-zA-Z0-9_\\s]", "")
                .replaceAll("\\s+", "_")
                .toLowerCase();
    }
    
    /**
     * 根据字符串值解析为对应的数据类型
     *
     * @param value 字符串值
     * @return 转换后的值
     */
    public Object parseValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            switch (dataType) {
                case STRING:
                    return value;
                case NUMBER:
                    // 尝试解析为整数或浮点数
                    if (value.contains(".")) {
                        return Double.parseDouble(value);
                    } else {
                        try {
                            return Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            return Long.parseLong(value);
                        }
                    }
                case DATE:
                    // 使用格式模式解析日期
                    if (formatPattern != null && !formatPattern.isEmpty()) {
                        return java.time.LocalDate.parse(value, java.time.format.DateTimeFormatter.ofPattern(formatPattern));
                    } else {
                        return java.time.LocalDate.parse(value);
                    }
                case DATETIME:
                    // 使用格式模式解析日期时间
                    if (formatPattern != null && !formatPattern.isEmpty()) {
                        return java.time.LocalDateTime.parse(value, java.time.format.DateTimeFormatter.ofPattern(formatPattern));
                    } else {
                        return java.time.LocalDateTime.parse(value);
                    }
                case BOOLEAN:
                    return Boolean.parseBoolean(value) || "1".equals(value) || "Y".equalsIgnoreCase(value) || "YES".equalsIgnoreCase(value);
                default:
                    return value;
            }
        } catch (Exception e) {
            // 解析失败时返回原始字符串
            return value;
        }
    }
} 