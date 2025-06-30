package com.excelsql.service.impl;

import com.excelsql.engine.executor.DDLExecutor;
import com.excelsql.engine.executor.DMLExecutor;
import com.excelsql.engine.executor.DQLExecutor;
import com.excelsql.engine.executor.QueryExecutor;
import com.excelsql.engine.parser.SQLParser;
import com.excelsql.engine.parser.model.ParsedQuery;
import com.excelsql.engine.parser.model.QueryType;
import com.excelsql.engine.cache.ExcelCacheManager;
import com.excelsql.service.ExcelSQLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Description: TODO::Need To Do
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:43
 */
@Service
public class ExcelSQLServiceImpl implements ExcelSQLService {

    @Autowired
    private SQLParser sqlParser;

    @Autowired
    private DDLExecutor ddlExecutor;

    @Autowired
    private DMLExecutor dmlExecutor;

    @Autowired
    private DQLExecutor dqlExecutor;

    @Autowired
    private ExcelCacheManager cacheManager;

    private final AtomicLong queryCount = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);

    @Override
    public Object executeSQL(String sql) {
        long startTime = System.currentTimeMillis();

        try {
            // Parse SQL
            ParsedQuery query = sqlParser.parseSQL(sql);

            // Route to appropriate executor
            QueryExecutor executor = getExecutor(query.getQueryType());

            // Execute query
            Object result = executor.executeQuery(query);

            // Update statistics
            long executionTime = System.currentTimeMillis() - startTime;
            queryCount.incrementAndGet();
            totalExecutionTime.addAndGet(executionTime);

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute SQL: " + sql, e);
        }
    }

    @Override
    public Map<String, Object> getQueryStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQueries", queryCount.get());
        stats.put("totalExecutionTimeMs", totalExecutionTime.get());
        stats.put("averageExecutionTimeMs",
                queryCount.get() > 0 ? totalExecutionTime.get() / queryCount.get() : 0);
        return stats;
    }

    @Override
    public void clearCache() {
        cacheManager.clearAll();
    }

    private QueryExecutor getExecutor(QueryType queryType) {
        switch (queryType) {
            case CREATE_WORKBOOK:
            case CREATE_SHEET:
            case DROP_WORKBOOK:
            case DROP_SHEET:
            case USE_WORKBOOK:
            case SHOW_WORKBOOKS:
            case SHOW_SHEETS:
                return ddlExecutor;

            case INSERT:
            case UPDATE:
            case DELETE:
                return dmlExecutor;

            case SELECT:
                return dqlExecutor;

            default:
                throw new UnsupportedOperationException("Unsupported query type: " + queryType);
        }
    }
}