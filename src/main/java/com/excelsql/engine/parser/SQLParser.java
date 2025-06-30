package com.excelsql.engine.parser;

import com.excelsql.engine.parser.model.ParsedQuery;

/**
 * @Description: SQL 解析接口
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:40
 */
public interface SQLParser {
    ParsedQuery parseSQL(String sql);
}
