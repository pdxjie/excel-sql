package com.excelsql.engine.cache;

import com.excelsql.config.ExcelSQLConfig;
import com.excelsql.engine.cache.model.CacheStats;
import com.excelsql.engine.cache.model.QueryResultCache;
import com.excelsql.engine.cache.model.SheetData;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @Description: 缓存管理
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:41
 */
@Component
@Slf4j
public class ExcelCacheManager {

    @Resource
    private ExcelSQLConfig config;

    // 工作簿元数据缓存 - 使用ConcurrentHashMap保证线程安全
    private final Map<String, WorkbookCache> workbookCache = new ConcurrentHashMap<>();

    // 表格数据缓存 - 使用LRU策略
    private LRUCache<String, SheetData> sheetDataCache;

    // 查询结果缓存 - 使用Caffeine缓存
    private Cache<String, QueryResultCache> queryResultCache;

    // 缓存统计
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);

    @PostConstruct
    public void init() {
        // 初始化LRU缓存
        int maxSheetCacheSize = config.getCache().getMaxSize();
        this.sheetDataCache = new LRUCache<>(maxSheetCacheSize);

        // 初始化Caffeine查询结果缓存
        this.queryResultCache = Caffeine.newBuilder()
                .maximumSize(config.getCache().getMaxQueryResultSize())
                .expireAfterWrite(Duration.ofMinutes(config.getCache().getExpireAfterWriteMinutes()))
                .recordStats()
                .removalListener((key, value, cause) -> {
                    evictionCount.incrementAndGet();
                    log.debug("Query cache evicted: key={}, cause={}", key, cause);
                })
                .build();

        log.info("ExcelCacheManager initialized with maxSheetCacheSize={}, maxQueryResultSize={}",
                maxSheetCacheSize, config.getCache().getMaxQueryResultSize());
    }

    // ========================= 工作簿缓存 =========================

    /**
     * 获取工作簿缓存
     */
    public WorkbookCache getWorkbook(String workbookName) {
        WorkbookCache workbook = workbookCache.get(workbookName);
        if (workbook != null) {
            hitCount.incrementAndGet();
            workbook.updateLastAccess();
            log.debug("Workbook cache hit: {}", workbookName);
        } else {
            missCount.incrementAndGet();
            log.debug("Workbook cache miss: {}", workbookName);
        }
        return workbook;
    }

    /**
     * 缓存工作簿
     */
    public void putWorkbook(String workbookName, WorkbookCache workbook) {
        if (workbook == null) {
            return;
        }

        workbookCache.put(workbookName, workbook);
        log.debug("Workbook cached: {}", workbookName);

        // 检查缓存大小，如果超过限制则清理旧的条目
        cleanupWorkbookCacheIfNeeded();
    }

    /**
     * 清除工作簿缓存
     */
    public void evictWorkbook(String workbookName) {
        WorkbookCache removed = workbookCache.remove(workbookName);
        if (removed != null) {
            evictionCount.incrementAndGet();
            log.debug("Workbook cache evicted: {}", workbookName);

            // 同时清除该工作簿下的所有表格数据缓存
            evictWorkbookRelatedSheetData(workbookName);
        }
    }

    // ========================= 表格数据缓存 =========================

    /**
     * 获取表格数据缓存
     */
    public SheetData getSheetData(String workbookName, String sheetName) {
        String cacheKey = buildSheetCacheKey(workbookName, sheetName);
        SheetData sheetData = sheetDataCache.get(cacheKey);

        if (sheetData != null) {
            hitCount.incrementAndGet();
            log.debug("Sheet data cache hit: {}#{}", workbookName, sheetName);
        } else {
            missCount.incrementAndGet();
            log.debug("Sheet data cache miss: {}#{}", workbookName, sheetName);
        }

        return sheetData;
    }

    /**
     * 缓存表格数据
     */
    public void putSheetData(String workbookName, String sheetName, List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        String cacheKey = buildSheetCacheKey(workbookName, sheetName);
        SheetData sheetData = new SheetData(workbookName, sheetName, data);

        sheetDataCache.put(cacheKey, sheetData);
        log.debug("Sheet data cached: {}#{}, rows={}", workbookName, sheetName, data.size());
    }

    /**
     * 清除特定表格数据缓存
     */
    public void evictSheetData(String workbookName, String sheetName) {
        String cacheKey = buildSheetCacheKey(workbookName, sheetName);
        SheetData removed = sheetDataCache.remove(cacheKey);

        if (removed != null) {
            evictionCount.incrementAndGet();
            log.debug("Sheet data cache evicted: {}#{}", workbookName, sheetName);
        }

        // 同时清除相关的查询结果缓存
        evictRelatedQueryResults(workbookName, sheetName);
    }

    // ========================= 查询结果缓存 =========================

    /**
     * 获取查询结果缓存
     */
    public QueryResultCache getQueryResult(String queryKey) {
        QueryResultCache result = queryResultCache.getIfPresent(queryKey);

        if (result != null) {
            hitCount.incrementAndGet();
            log.debug("Query result cache hit: {}", queryKey);
        } else {
            missCount.incrementAndGet();
            log.debug("Query result cache miss: {}", queryKey);
        }

        return result;
    }

    /**
     * 缓存查询结果
     */
    public void putQueryResult(String queryKey, QueryResultCache result) {
        if (result == null) {
            return;
        }

        queryResultCache.put(queryKey, result);
        log.debug("Query result cached: {}", queryKey);
    }

    /**
     * 清除查询结果缓存
     */
    public void evictQueryResult(String queryKey) {
        QueryResultCache removed = queryResultCache.asMap().remove(queryKey);
        if (removed != null) {
            evictionCount.incrementAndGet();
            log.debug("Query result cache evicted: {}", queryKey);
        }
    }

    // ========================= 批量操作 =========================

    /**
     * 清除工作簿相关的所有表格数据缓存
     */
    private void evictWorkbookRelatedSheetData(String workbookName) {
        String prefix = workbookName + "#";
        Set<String> keysToRemove = sheetDataCache.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .collect(Collectors.toSet());

        for (String key : keysToRemove) {
            sheetDataCache.remove(key);
            evictionCount.incrementAndGet();
        }

        log.debug("Evicted {} sheet data caches for workbook: {}", keysToRemove.size(), workbookName);
    }

    /**
     * 清除相关的查询结果缓存
     */
    private void evictRelatedQueryResults(String workbookName, String sheetName) {
        String targetKey = workbookName + "#" + sheetName;
        Set<String> keysToRemove = queryResultCache.asMap().keySet().stream()
                .filter(key -> key.contains(targetKey))
                .collect(Collectors.toSet());

        for (String key : keysToRemove) {
            queryResultCache.invalidate(key);
        }

        log.debug("Evicted {} query result caches for sheet: {}#{}", keysToRemove.size(), workbookName, sheetName);
    }

    /**
     * 清理工作簿缓存（当超过限制时）
     */
    private void cleanupWorkbookCacheIfNeeded() {
        int maxWorkbookCacheSize = config.getCache().getMaxWorkbookCacheSize();
        if (workbookCache.size() > maxWorkbookCacheSize) {
            // 按最后访问时间排序，清理最老的条目
            List<Map.Entry<String, WorkbookCache>> sortedEntries = workbookCache.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue((a, b) ->
                            a.getLastAccessTime().compareTo(b.getLastAccessTime())))
                    .collect(Collectors.toList());

            int toRemove = workbookCache.size() - maxWorkbookCacheSize + 1;
            for (int i = 0; i < toRemove && i < sortedEntries.size(); i++) {
                String key = sortedEntries.get(i).getKey();
                evictWorkbook(key);
            }
        }
    }

    // ========================= 缓存管理 =========================

    /**
     * 清除所有缓存
     */
    public void clearAll() {
        workbookCache.clear();
        sheetDataCache.clear();
        queryResultCache.invalidateAll();

        // 重置统计
        hitCount.set(0);
        missCount.set(0);
        evictionCount.set(0);

        log.info("All caches cleared");
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = queryResultCache.stats();

        return CacheStats.builder()
                .workbookCacheSize(workbookCache.size())
                .sheetDataCacheSize(sheetDataCache.size())
                .queryResultCacheSize(queryResultCache.estimatedSize())
                .totalHitCount(hitCount.get() + caffeineStats.hitCount())
                .totalMissCount(missCount.get() + caffeineStats.missCount())
                .totalEvictionCount(evictionCount.get() + caffeineStats.evictionCount())
                .hitRate(calculateHitRate())
                .memoryUsage(estimateMemoryUsage())
                .build();
    }

    /**
     * 预热缓存
     */
    public void warmup(List<String> frequentlyAccessedWorkbooks) {
        for (String workbookName : frequentlyAccessedWorkbooks) {
            try {
                // 这里可以异步预加载工作簿元数据
                log.debug("Warming up cache for workbook: {}", workbookName);
                // 实际实现需要调用存储层来预加载数据
            } catch (Exception e) {
                log.warn("Failed to warm up cache for workbook: {}", workbookName, e);
            }
        }
    }

    // ========================= 工具方法 =========================

    private String buildSheetCacheKey(String workbookName, String sheetName) {
        return workbookName + "#" + sheetName;
    }

    private double calculateHitRate() {
        long totalHits = hitCount.get() + queryResultCache.stats().hitCount();
        long totalRequests = totalHits + missCount.get() + queryResultCache.stats().missCount();
        return totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
    }

    private long estimateMemoryUsage() {
        // 简单估算内存使用量
        long workbookMemory = workbookCache.size() * 1024; // 假设每个工作簿缓存1KB
        long sheetDataMemory = sheetDataCache.values().stream()
                .mapToLong(sheetData -> sheetData.getData().size() * 256) // 假设每行256字节
                .sum();
        return workbookMemory + sheetDataMemory;
    }

    // ========================= 内部类 =========================

    /**
     * LRU缓存实现
     */
    private static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        public LRUCache(int maxSize) {
            super(16, 0.75f, true); // accessOrder = true
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }

        @Override
        public V get(Object key) {
            return super.get(key);
        }

        @Override
        public V put(K key, V value) {
            return super.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return super.remove(key);
        }

        public void clear() {
            super.clear();
        }
    }
}