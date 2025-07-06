package com.excel.sql.engine.service.executor;

import com.excel.sql.engine.model.dto.SqlQueryRequest;
import com.excel.sql.engine.model.dto.SqlQueryResult;
import com.excel.sql.engine.service.parser.ParsedSql;

/**
 * SQL执行器接口
 */
public interface SqlExecutor {
    
    /**
     * 执行SQL查询
     *
     * @param request SQL查询请求
     * @return SQL查询结果
     */
    SqlQueryResult execute(SqlQueryRequest request);
    
    /**
     * 执行已解析的SQL查询
     *
     * @param parsedSql 已解析的SQL
     * @param workbook 工作簿名称
     * @param useCache 是否使用缓存
     * @param maxRows 最大返回行数
     * @return SQL查询结果
     */
    SqlQueryResult execute(ParsedSql parsedSql, String workbook, boolean useCache, int maxRows);
} 