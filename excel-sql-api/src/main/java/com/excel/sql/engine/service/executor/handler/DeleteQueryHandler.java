package com.excel.sql.engine.service.executor.handler;

import com.excel.sql.engine.model.dto.SqlQueryResult;
import com.excel.sql.engine.service.parser.ParsedSql;

/**
 * DELETE查询处理器接口
 */
public interface DeleteQueryHandler extends QueryHandler {
    
    /**
     * 处理DELETE查询
     *
     * @param parsedSql 已解析的SQL
     * @param workbook 工作簿名称
     * @return SQL查询结果
     */
    @Override
    SqlQueryResult handle(ParsedSql parsedSql, String workbook);
} 