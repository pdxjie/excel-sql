package com.excel.sql.engine.repository;

import com.excel.sql.engine.model.entity.QueryCacheEntity;

/**
 * 查询缓存仓库接口
 */
public interface QueryCacheRepository {
    
    /**
     * 根据缓存键查找缓存实体
     *
     * @param cacheKey 缓存键
     * @return 缓存实体
     */
    QueryCacheEntity findByCacheKey(String cacheKey);
    
    /**
     * 保存缓存实体
     *
     * @param entity 缓存实体
     * @return 保存后的缓存实体
     */
    QueryCacheEntity save(QueryCacheEntity entity);
    
    /**
     * 更新缓存实体
     *
     * @param entity 缓存实体
     * @return 更新后的缓存实体
     */
    QueryCacheEntity update(QueryCacheEntity entity);
    
    /**
     * 更新命中次数
     *
     * @param id 缓存实体ID
     * @param hitCount 命中次数
     */
    void updateHitCount(Long id, Integer hitCount);
    
    /**
     * 删除指定工作簿的所有缓存
     *
     * @param workbook 工作簿名称
     */
    void deleteByWorkbook(String workbook);
    
    /**
     * 删除指定工作簿和工作表的所有缓存
     *
     * @param workbook 工作簿名称
     * @param sheet 工作表名称
     */
    void deleteByWorkbookAndSheet(String workbook, String sheet);
    
    /**
     * 删除所有缓存
     */
    void deleteAll();
} 