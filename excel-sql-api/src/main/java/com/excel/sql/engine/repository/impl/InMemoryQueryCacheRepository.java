package com.excel.sql.engine.repository.impl;

import com.excel.sql.engine.model.entity.QueryCacheEntity;
import com.excel.sql.engine.repository.QueryCacheRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存版查询缓存仓库实现
 * 用于开发和测试环境，生产环境应使用数据库实现
 */
@Repository
public class InMemoryQueryCacheRepository implements QueryCacheRepository {
    
    private final Map<String, QueryCacheEntity> cacheMap = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    public QueryCacheEntity findByCacheKey(String cacheKey) {
        return cacheMap.get(cacheKey);
    }
    
    @Override
    public QueryCacheEntity save(QueryCacheEntity entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        }
        cacheMap.put(entity.getCacheKey(), entity);
        return entity;
    }
    
    @Override
    public QueryCacheEntity update(QueryCacheEntity entity) {
        if (entity.getId() == null) {
            return save(entity);
        }
        cacheMap.put(entity.getCacheKey(), entity);
        return entity;
    }
    
    @Override
    public void updateHitCount(Long id, Integer hitCount) {
        cacheMap.values().stream()
                .filter(entity -> entity.getId().equals(id))
                .findFirst()
                .ifPresent(entity -> entity.setHitCount(hitCount));
    }
    
    @Override
    public void deleteByWorkbook(String workbook) {
        cacheMap.entrySet().removeIf(entry -> 
                entry.getKey().startsWith(workbook + ":"));
    }
    
    @Override
    public void deleteByWorkbookAndSheet(String workbook, String sheet) {
        cacheMap.entrySet().removeIf(entry -> 
                entry.getKey().startsWith(workbook + ":" + sheet + ":"));
    }
    
    @Override
    public void deleteAll() {
        cacheMap.clear();
    }
} 