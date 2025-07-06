package com.excel.sql.engine.model.dto;

import com.excel.sql.engine.model.excel.ExcelDataType;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * SQL查询结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlQueryResult {
    
    /**
     * 列定义列表
     */
    private List<ColumnDefinition> columns;
    
    /**
     * 数据行列表
     */
    private List<Map<String, Object>> rows;
    
    /**
     * 影响的行数（用于INSERT/UPDATE/DELETE）
     */
    private Integer affectedRows;
    
    /**
     * 执行时间（毫秒）
     */
    private Long executionTime;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * SQL语句类型
     */
    private SqlType sqlType;
    
    /**
     * 列定义内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnDefinition {
        /**
         * 列名
         */
        private String name;
        
        /**
         * 列标签（别名）
         */
        private String label;
        
        /**
         * 数据类型
         */
        private ExcelDataType dataType;
        
        /**
         * 是否为聚合列
         */
        private boolean aggregated;
    }
    
    /**
     * SQL语句类型枚举
     */
    public enum SqlType {
        SELECT,
        INSERT,
        UPDATE,
        DELETE,
        CREATE,
        DROP,
        ALTER,
        USE,
        CREATE_WORKBOOK,
        CREATE_SHEET,
        DROP_WORKBOOK,
        DROP_SHEET,
        USE_WORKBOOK,
        UNKNOWN
    }
    
    /**
     * 创建成功的查询结果
     *
     * @param columns 列定义
     * @param rows 数据行
     * @param executionTime 执行时间
     * @return 查询结果
     */
    public static SqlQueryResult success(List<ColumnDefinition> columns, List<Map<String, Object>> rows, Long executionTime) {
        return SqlQueryResult.builder()
                .columns(columns)
                .rows(rows)
                .executionTime(executionTime)
                .success(true)
                .sqlType(SqlType.SELECT)
                .build();
    }
    
    /**
     * 创建成功的DML结果
     *
     * @param affectedRows 影响的行数
     * @param sqlType SQL类型
     * @param executionTime 执行时间
     * @return 查询结果
     */
    public static SqlQueryResult successDml(Integer affectedRows, SqlType sqlType, Long executionTime) {
        return SqlQueryResult.builder()
                .affectedRows(affectedRows)
                .executionTime(executionTime)
                .success(true)
                .sqlType(sqlType)
                .build();
    }
    
    /**
     * 创建失败的查询结果
     *
     * @param errorMessage 错误信息
     * @return 查询结果
     */
    public static SqlQueryResult error(String errorMessage) {
        return SqlQueryResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
} 