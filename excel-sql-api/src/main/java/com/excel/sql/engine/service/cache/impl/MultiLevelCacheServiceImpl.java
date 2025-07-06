package com.excel.sql.engine.service.cache.impl;

import com.excel.sql.engine.model.dto.SqlQueryResult;
import com.excel.sql.engine.model.entity.QueryCacheEntity;
import com.excel.sql.engine.repository.QueryCacheRepository;
import com.excel.sql.engine.service.cache.QueryCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存服务实现类
 * L1: Caffeine (内存缓存)
 * L2: Redis (分布式缓存)
 * L3: MySQL (持久化存储)
 */
@Slf4j
@Service
public class MultiLevelCacheServiceImpl implements QueryCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final QueryCacheRepository queryCacheRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${excel-sql.cache.l1-size:200}")
    private int l1CacheSize;
    
    @Value("${excel-sql.cache.l2-ttl:3600}")
    private int l2CacheTtl;
    
    /**
     * L1缓存 (Caffeine)
     */
    private final Cache<String, SqlQueryResult> l1Cache = Caffeine.newBuilder()
            .maximumSize(200)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats()
            .build();
    
    public MultiLevelCacheServiceImpl(
            @Qualifier("customRedisTemplate") RedisTemplate<String, Object> redisTemplate,
            QueryCacheRepository queryCacheRepository,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.queryCacheRepository = queryCacheRepository;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public SqlQueryResult getFromCache(String cacheKey) {
        // 1. 尝试从L1缓存获取
        SqlQueryResult result = l1Cache.getIfPresent(cacheKey);
        if (result != null) {
            log.debug("L1缓存命中: {}", cacheKey);
            return result;
        }
        
        try {
            // 2. 尝试从L2缓存获取
            Object redisValue = redisTemplate.opsForValue().get("excel-sql:cache:" + cacheKey);
            if (redisValue != null) {
                result = objectMapper.readValue(redisValue.toString(), SqlQueryResult.class);
                // 回填L1缓存
                l1Cache.put(cacheKey, result);
                log.debug("L2缓存命中: {}", cacheKey);
                return result;
            }
            
            // 3. 尝试从L3缓存获取
            QueryCacheEntity cacheEntity = queryCacheRepository.findByCacheKey(cacheKey);
            if (cacheEntity != null && cacheEntity.getExpireTime().isAfter(LocalDateTime.now())) {
                result = objectMapper.readValue(cacheEntity.getResultJson(), SqlQueryResult.class);
                // 回填L1和L2缓存
                l1Cache.put(cacheKey, result);
                redisTemplate.opsForValue().set("excel-sql:cache:" + cacheKey, cacheEntity.getResultJson(), l2CacheTtl, TimeUnit.SECONDS);
                
                // 更新命中次数
                cacheEntity.setHitCount(cacheEntity.getHitCount() + 1);
                queryCacheRepository.updateHitCount(cacheEntity.getId(), cacheEntity.getHitCount());
                
                log.debug("L3缓存命中: {}", cacheKey);
                return result;
            }
        } catch (Exception e) {
            log.error("从缓存获取数据异常: {}", e.getMessage(), e);
        }
        
        return null;
    }
    
    @Override
    public void putToCache(String cacheKey, SqlQueryResult result) {
        try {
            // 1. 放入L1缓存
            l1Cache.put(cacheKey, result);
            
            // 2. 放入L2缓存
            String jsonResult = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set("excel-sql:cache:" + cacheKey, jsonResult, l2CacheTtl, TimeUnit.SECONDS);
            
            // 3. 放入L3缓存
            QueryCacheEntity cacheEntity = queryCacheRepository.findByCacheKey(cacheKey);
            if (cacheEntity == null) {
                cacheEntity = new QueryCacheEntity();
                cacheEntity.setCacheKey(cacheKey);
                cacheEntity.setSqlQuery(result.getSqlType().name());
                cacheEntity.setResultJson(jsonResult);
                cacheEntity.setExecutionTime(result.getExecutionTime());
                cacheEntity.setHitCount(0);
                cacheEntity.setExpireTime(LocalDateTime.now().plusSeconds(l2CacheTtl * 2));
                cacheEntity.setCreateTime(LocalDateTime.now());
                cacheEntity.setUpdateTime(LocalDateTime.now());
                cacheEntity.setDeleted(0);
                queryCacheRepository.save(cacheEntity);
            } else {
                cacheEntity.setResultJson(jsonResult);
                cacheEntity.setExecutionTime(result.getExecutionTime());
                cacheEntity.setExpireTime(LocalDateTime.now().plusSeconds(l2CacheTtl * 2));
                cacheEntity.setUpdateTime(LocalDateTime.now());
                queryCacheRepository.update(cacheEntity);
            }
            
            log.debug("缓存查询结果: {}", cacheKey);
        } catch (Exception e) {
            log.error("缓存查询结果异常: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void clearCache(String workbook) {
        try {
            // 1. 清除L1缓存
            l1Cache.invalidateAll();
            
            // 2. 清除L2缓存
            redisTemplate.delete(redisTemplate.keys("excel-sql:cache:" + workbook + ":*"));
            
            // 3. 清除L3缓存
            queryCacheRepository.deleteByWorkbook(workbook);
            
            log.debug("清除工作簿缓存: {}", workbook);
        } catch (Exception e) {
            log.error("清除缓存异常: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void clearCache(String workbook, String sheet) {
        try {
            // 1. 清除L1缓存
            l1Cache.invalidateAll();
            
            // 2. 清除L2缓存
            redisTemplate.delete(redisTemplate.keys("excel-sql:cache:" + workbook + ":" + sheet + ":*"));
            
            // 3. 清除L3缓存
            queryCacheRepository.deleteByWorkbookAndSheet(workbook, sheet);
            
            log.debug("清除工作表缓存: {}:{}", workbook, sheet);
        } catch (Exception e) {
            log.error("清除缓存异常: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void clearAllCache() {
        try {
            // 1. 清除L1缓存
            l1Cache.invalidateAll();
            
            // 2. 清除L2缓存
            redisTemplate.delete(redisTemplate.keys("excel-sql:cache:*"));
            
            // 3. 清除L3缓存
            queryCacheRepository.deleteAll();
            
            log.debug("清除所有缓存");
        } catch (Exception e) {
            log.error("清除所有缓存异常: {}", e.getMessage(), e);
        }
    }
} 