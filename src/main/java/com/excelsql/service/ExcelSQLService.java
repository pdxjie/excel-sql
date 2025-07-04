package com.excelsql.service;

import java.util.Map;

/**
 * @Description: 核心服务接口
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:43
 */
public interface ExcelSQLService {

    /**
     * 执行给定的SQL语句
     * 该方法可以处理任何类型的SQL语句，但具体的行为和性能特性可能依赖于底层数据库和驱动程序
     *
     * @param sql 要执行的SQL语句，不应包含任何未经过滤的用户输入以防止SQL注入
     * @return 执行SQL语句的结果，具体类型取决于SQL语句的类型和预期的返回值
     */
    Object executeSQL(String sql);

    /**
     * 获取查询统计信息
     * 提供有关先前执行的查询的性能和统计信息，这些信息可以帮助开发者进行性能分析和优化
     *
     * @return 包含查询统计信息的Map，键是统计信息的名称，值是对应的值
     */
    Map<String, Object> getQueryStatistics();

    /**
     * 清空缓存
     * 该方法清空了内部使用的任何缓存，通常用于测试环境以确保不会因为缓存而影响测试结果的准确性
     */
    void clearCache();
}
