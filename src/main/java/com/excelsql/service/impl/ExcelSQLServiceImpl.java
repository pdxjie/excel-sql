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
    private ExcelCacheManager excelCacheManager;

    private final AtomicLong queryCount = new AtomicLong(0);

    private final AtomicLong totalExecutionTime = new AtomicLong(0);

    /**
     * 执行给定的SQL语句
     * 该方法可以处理任何类型的SQL语句，但具体的行为和性能特性可能依赖于底层数据库和驱动程序
     *
     * @param sql 要执行的SQL语句，不应包含任何未经过滤的用户输入以防止SQL注入
     * @return 执行SQL语句的结果，具体类型取决于SQL语句的类型和预期的返回值
     */
    @Override
    public Object executeSQL(String sql) {
        // 记录查询开始时间
        long startTime = System.currentTimeMillis();

        try {
            // 解析 SQL 查询
            ParsedQuery query = sqlParser.parseSQL(sql);

            // 根据查询类型获取相应的执行器
            QueryExecutor executor = getExecutor(query.getQueryType());

            // 执行查询并返回结果
            Object result = executor.executeQuery(query);

            // 计算查询执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            // 增加查询计数
            queryCount.incrementAndGet();
            // 增加总执行时间
            totalExecutionTime.addAndGet(executionTime);

            return result;

        } catch (Exception e) {
            // 如果查询执行失败，抛出运行时异常
            throw new RuntimeException("Failed to execute SQL: " + sql + '('+ e.getMessage() +')');
        }
    }

    /**
     * 获取查询统计信息
     * 提供有关先前执行的查询的性能和统计信息，这些信息可以帮助开发者进行性能分析和优化
     *
     * @return 包含查询统计信息的Map，键是统计信息的名称，值是对应的值
     */
    @Override
    public Map<String, Object> getQueryStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQueries", queryCount.get());
        stats.put("totalExecutionTimeMs", totalExecutionTime.get());
        stats.put("averageExecutionTimeMs",
                queryCount.get() > 0 ? totalExecutionTime.get() / queryCount.get() : 0);
        return stats;
    }

    /**
     * 清空缓存
     * 该方法清空了内部使用的任何缓存，通常用于测试环境以确保不会因为缓存而影响测试结果的准确性
     */
    @Override
    public void clearCache() {
        excelCacheManager.clearAll();
    }

    /**
     * 根据查询类型获取相应的查询执行器
     *
     * @param queryType 查询类型，用于决定使用哪种类型的查询执行器
     * @return 返回对应查询类型的QueryExecutor实例
     * @throws UnsupportedOperationException 如果查询类型不受支持，则抛出此异常
     */
    private QueryExecutor getExecutor(QueryType queryType) {
        // 根据不同的查询类型，分配不同的查询执行器
        switch (queryType) {
            // 数据定义语言（DDL）操作，如创建、删除工作簿或表单等
            case CREATE_WORKBOOK:
            case CREATE_SHEET:
            case DROP_WORKBOOK:
            case DROP_SHEET:
            case USE_WORKBOOK:
            case SHOW_WORKBOOKS:
            case SHOW_SHEETS:
                return ddlExecutor;
            // 数据操作语言（DML）操作，涉及数据的增删改
            case INSERT:
            case UPDATE:
            case DELETE:
                return dmlExecutor;
            // 数据查询语言（DQL）操作，主要用于数据的查询
            case SELECT:
                return dqlExecutor;

            // 如果是未知的查询类型，抛出异常
            default:
                throw new UnsupportedOperationException("Unsupported query type: " + queryType);
        }
    }
}