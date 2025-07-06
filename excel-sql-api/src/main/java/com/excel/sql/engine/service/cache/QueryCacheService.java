package com.excel.sql.engine.service.cache;

import com.excel.sql.engine.model.dto.SqlQueryResult;

/**
 * 查询缓存服务接口
 */
public interface QueryCacheService {
    
    /**
     * 从缓存中获取查询结果
     *
     * @param cacheKey 缓存键
     * @return 查询结果，如果缓存中不存在则返回null
     */
    SqlQueryResult getFromCache(String cacheKey);
    
    /**
     * 将查询结果放入缓存
     *
     * @param cacheKey 缓存键
     * @param result 查询结果
     */
    void putToCache(String cacheKey, SqlQueryResult result);
    
    /**
     * 清除指定工作簿的缓存
     *
     * @param workbook 工作簿名称
     */
    void clearCache(String workbook);
    
    /**
     * 清除指定工作簿和工作表的缓存
     *
     * @param workbook 工作簿名称
     * @param sheet 工作表名称
     */
    void clearCache(String workbook, String sheet);
    
    /**
     * 清除所有缓存
     */
    void clearAllCache();
} 