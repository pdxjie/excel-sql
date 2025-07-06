package com.excel.sql.engine.service.executor.handler;

import com.excel.sql.engine.model.dto.SqlQueryResult;
import com.excel.sql.engine.service.parser.ParsedSql;

/**
 * UPDATE查询处理器接口
 */
public interface UpdateQueryHandler extends QueryHandler {
    
    /**
     * 处理UPDATE查询
     *
     * @param parsedSql 已解析的SQL
     * @param workbook 工作簿名称
     * @return SQL查询结果
     */
    @Override
    SqlQueryResult handle(ParsedSql parsedSql, String workbook);
} 