package com.excelsql.engine.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 列信息类
 * 存储Excel列的详细信息和元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 列名（表头名称）
     */
    private String columnName;
    
    /**
     * 列索引（从0开始）
     */
    private int columnIndex;
    
    /**
     * Excel列标识（A, B, C...）
     */
    private String columnLabel;
    
    /**
     * 数据类型
     */
    private ExcelDataType dataType;
    
    /**
     * 是否允许为空
     */
    private boolean nullable;
    
    /**
     * 是否是主键
     */
    private boolean isPrimaryKey;
    
    /**
     * 列宽
     */
    private double columnWidth;
    
    /**
     * 数据格式
     */
    private String dataFormat;
    
    /**
     * 示例值（用于类型推断）
     */
    private List<Object> sampleValues;
    
    /**
     * 数据验证规则
     */
    private DataValidation validation;
    
    /**
     * 注释信息
     */
    private String comment;
    
    /**
     * 统计信息
     */
    private ColumnStatistics statistics;
    
    @Builder
    public ColumnInfo(String columnName, int columnIndex, ExcelDataType dataType) {
        this.columnName = columnName;
        this.columnIndex = columnIndex;
        this.columnLabel = convertToExcelColumn(columnIndex);
        this.dataType = dataType != null ? dataType : ExcelDataType.STRING;
        this.nullable = true;
        this.isPrimaryKey = false;
        this.columnWidth = 10.0;
        this.sampleValues = new ArrayList<>();
    }
    
    /**
     * 将列索引转换为Excel列标识
     */
    public static String convertToExcelColumn(int columnIndex) {
        StringBuilder result = new StringBuilder();
        while (columnIndex >= 0) {
            result.insert(0, (char) ('A' + columnIndex % 26));
            columnIndex = columnIndex / 26 - 1;
        }
        return result.toString();
    }
    
    /**
     * Excel数据类型枚举
     */
    public enum ExcelDataType {
        STRING("字符串"),
        INTEGER("整数"),
        DECIMAL("小数"),
        DATE("日期"),
        DATETIME("日期时间"),
        BOOLEAN("布尔值"),
        FORMULA("公式"),
        ERROR("错误值"),
        BLANK("空值");
        
        private final String description;
        
        ExcelDataType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 数据验证类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataValidation {
        private String validationType;
        private String validationFormula;
        private String errorMessage;
        private boolean allowBlank;
    }
    
    /**
     * 列统计信息类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnStatistics {
        private int totalCount;
        private int nullCount;
        private int uniqueCount;
        private Object minValue;
        private Object maxValue;
        private Double avgValue;
    }
}
