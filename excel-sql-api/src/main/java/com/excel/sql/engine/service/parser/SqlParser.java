package com.excel.sql.engine.service.parser;

import com.excel.sql.engine.model.dto.SqlQueryResult;

/**
 * SQL解析器接口
 */
public interface SqlParser {
    
    /**
     * 解析SQL语句
     *
     * @param sql SQL语句
     * @return 解析结果
     */
    ParsedSql parse(String sql);
    
    /**
     * 验证SQL语句
     *
     * @param sql SQL语句
     * @return 验证结果，如果有错误则返回错误信息，否则返回null
     */
    String validate(String sql);
    
    /**
     * 获取SQL语句类型
     *
     * @param sql SQL语句
     * @return SQL语句类型
     */
    SqlQueryResult.SqlType getSqlType(String sql);
} 