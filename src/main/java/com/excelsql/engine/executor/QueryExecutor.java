package com.excelsql.engine.executor;

import com.excelsql.engine.parser.model.ParsedQuery;

import java.util.Map;

/**
 * @Description: 查询执行器
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:40
 */
public interface QueryExecutor {

    Map<String, Object> execute(ParsedQuery query);

    boolean canExecute(ParsedQuery query);
}
