package com.excelsql.engine.cache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 缓存统计信息类
 * 提供缓存性能监控数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStats {

    /**
     * 工作簿缓存大小
     */
    private long workbookCacheSize;

    /**
     * 工作表数据缓存大小
     */
    private long sheetDataCacheSize;

    /**
     * 查询结果缓存大小
     */
    private long queryResultCacheSize;

    /**
     * 总命中次数
     */
    private long totalHitCount;

    /**
     * 总未命中次数
     */
    private long totalMissCount;

    /**
     * 总逐出次数
     */
    private long totalEvictionCount;

    /**
     * 命中率
     */
    private double hitRate;

    /**
     * 内存使用量（字节）
     */
    private long memoryUsage;

    /**
     * 统计时间
     */
    private LocalDateTime statisticsTime;

    /**
     * 构造函数设置统计时间
     */
    @Builder
    public CacheStats(long workbookCacheSize, long sheetDataCacheSize, long queryResultCacheSize,
                      long totalHitCount, long totalMissCount, long totalEvictionCount,
                      double hitRate, long memoryUsage) {
        this.workbookCacheSize = workbookCacheSize;
        this.sheetDataCacheSize = sheetDataCacheSize;
        this.queryResultCacheSize = queryResultCacheSize;
        this.totalHitCount = totalHitCount;
        this.totalMissCount = totalMissCount;
        this.totalEvictionCount = totalEvictionCount;
        this.hitRate = hitRate;
        this.memoryUsage = memoryUsage;
        this.statisticsTime = LocalDateTime.now();
    }

    /**
     * 获取总请求次数
     */
    public long getTotalRequestCount() {
        return totalHitCount + totalMissCount;
    }

    /**
     * 获取格式化的命中率
     */
    public String getFormattedHitRate() {
        return String.format("%.2f%%", hitRate * 100);
    }

    /**
     * 获取格式化的内存使用量
     */
    public String getFormattedMemoryUsage() {
        if (memoryUsage < 1024) {
            return memoryUsage + " B";
        } else if (memoryUsage < 1024 * 1024) {
            return String.format("%.2f KB", memoryUsage / 1024.0);
        } else if (memoryUsage < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", memoryUsage / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", memoryUsage / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 转换为JSON字符串
     */
    @Override
    public String toString() {
        return String.format(
                "CacheStats{workbookCache=%d, sheetDataCache=%d, queryResultCache=%d, " +
                        "hitRate=%.2f%%, memoryUsage=%s, totalRequests=%d}",
                workbookCacheSize, sheetDataCacheSize, queryResultCacheSize,
                hitRate * 100, getFormattedMemoryUsage(), getTotalRequestCount()
        );
    }
}