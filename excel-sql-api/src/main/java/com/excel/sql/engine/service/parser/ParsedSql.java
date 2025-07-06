package com.excel.sql.engine.service.parser;

import com.excel.sql.engine.model.dto.SqlQueryResult;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 解析后的SQL对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedSql {
    
    /**
     * 原始SQL语句
     */
    private String originalSql;
    
    /**
     * SQL语句类型
     */
    private SqlQueryResult.SqlType sqlType;
    
    /**
     * 目标表名（FROM子句中的表名）
     */
    private List<String> targetTables;
    
    /**
     * 选择的列（SELECT子句中的列名）
     */
    private List<String> selectedColumns;
    
    /**
     * 列别名映射（列名 -> 别名）
     */
    private Map<String, String> columnAliases;
    
    /**
     * 表别名映射（表名 -> 别名）
     */
    private Map<String, String> tableAliases;
    
    /**
     * WHERE条件
     */
    private String whereCondition;
    
    /**
     * GROUP BY子句
     */
    private List<String> groupByColumns;
    
    /**
     * HAVING条件
     */
    private String havingCondition;
    
    /**
     * ORDER BY子句
     */
    private List<OrderByClause> orderByClauses;
    
    /**
     * LIMIT子句
     */
    private Integer limit;
    
    /**
     * OFFSET子句
     */
    private Integer offset;
    
    /**
     * JOIN子句
     */
    private List<JoinClause> joinClauses;
    
    /**
     * 聚合函数
     */
    private Map<String, String> aggregateFunctions;
    
    /**
     * 插入的值（INSERT语句）
     */
    private List<Map<String, Object>> insertValues;
    
    /**
     * 更新的值（UPDATE语句）
     */
    private Map<String, Object> updateValues;
    
    /**
     * 是否包含子查询
     */
    private boolean hasSubquery;
    
    /**
     * 子查询映射（子查询别名 -> 子查询ParsedSql）
     */
    private Map<String, ParsedSql> subqueries;
    
    /**
     * 解析是否成功
     */
    private boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * ORDER BY子句
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderByClause {
        /**
         * 列名
         */
        private String column;
        
        /**
         * 是否降序
         */
        private boolean descending;
    }
    
    /**
     * JOIN子句
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinClause {
        /**
         * JOIN类型（INNER, LEFT, RIGHT, FULL）
         */
        private String joinType;
        
        /**
         * 右表名
         */
        private String rightTable;
        
        /**
         * 右表别名
         */
        private String rightTableAlias;
        
        /**
         * JOIN条件
         */
        private String onCondition;
    }
} 