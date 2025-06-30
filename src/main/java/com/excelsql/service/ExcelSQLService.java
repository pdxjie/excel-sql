package com.excelsql.service;

import java.util.Map;

/**
 * @Description: 核心服务接口
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:43
 */
public interface ExcelSQLService {
    Object executeSQL(String sql);
    Map<String, Object> getQueryStatistics();
    void clearCache();
}
