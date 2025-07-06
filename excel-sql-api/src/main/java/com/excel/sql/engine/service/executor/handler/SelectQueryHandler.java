package com.excel.sql.engine.service.executor.handler;

import com.excel.sql.engine.model.dto.SqlQueryResult;
import com.excel.sql.engine.service.parser.ParsedSql;

/**
 * SELECT查询处理器接口
 */
public interface SelectQueryHandler extends QueryHandler {
    
    /**
     * 处理SELECT查询
     *
     * @param parsedSql 已解析的SQL
     * @param workbook 工作簿名称
     * @param maxRows 最大返回行数
     * @return SQL查询结果
     */
    SqlQueryResult handle(ParsedSql parsedSql, String workbook, int maxRows);
} 