package com.excelsql.engine.cache.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CacheStats {
    private int workbookCacheSize;
    private int sheetDataCacheSize;
    private long queryResultCacheSize;
    private long totalHitCount;
    private long totalMissCount;
    private long totalEvictionCount;
    private double hitRate;
    private long memoryUsage; // 估算的内存使用量（字节）
}