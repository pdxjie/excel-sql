package com.excelsql.engine.parser;

/**
 * @Description: SQL 解析接口
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:40
 */
import com.excelsql.engine.parser.model.ParsedQuery;
import com.excelsql.exception.ParseException;

public interface SQLParser {
    ParsedQuery parse(String sql) throws ParseException;
}
