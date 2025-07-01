package com.excelsql.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Description: 主配置类
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:40
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "excel-sql")
@Data
@Validated
public class ExcelSQLConfig {

    private Cache cache = new Cache();
    private Performance performance = new Performance();
    private Storage storage = new Storage();
    private Security security = new Security();
    private Monitoring monitoring = new Monitoring();

    // ========================= 缓存配置 =========================
    @Data
    @Validated
    public static class Cache {

        /**
         * Sheet数据缓存最大条目数
         */
        @Min(value = 100, message = "Sheet cache max size must be at least 100")
        @Max(value = 10000, message = "Sheet cache max size cannot exceed 10000")
        private int maxSize = 1000;

        /**
         * 工作簿元数据缓存最大条目数
         */
        @Min(value = 50, message = "Workbook cache max size must be at least 50")
        @Max(value = 5000, message = "Workbook cache max size cannot exceed 5000")
        private int maxWorkbookCacheSize = 500;

        /**
         * 查询结果缓存最大条目数
         */
        @Min(value = 100, message = "Query result cache max size must be at least 100")
        @Max(value = 20000, message = "Query result cache max size cannot exceed 20000")
        private long maxQueryResultSize = 2000;

        /**
         * 查询结果缓存过期时间（分钟）
         */
        @Min(value = 1, message = "Cache expire time must be at least 1 minute")
        @Max(value = 1440, message = "Cache expire time cannot exceed 1440 minutes (24 hours)")
        private int expireAfterWriteMinutes = 30;

        /**
         * 缓存预热开关
         */
        private boolean enableWarmup = true;

        /**
         * 预热的工作簿列表
         */
        private List<String> warmupWorkbooks = new ArrayList<>();

        /**
         * 缓存统计开关
         */
        private boolean enableStats = true;

        /**
         * 缓存清理间隔（分钟）
         */
        @Min(value = 1, message = "Cache cleanup interval must be at least 1 minute")
        private int cleanupIntervalMinutes = 10;

        /**
         * 最大内存使用量（MB）
         */
        @Min(value = 64, message = "Max memory usage must be at least 64MB")
        private long maxMemoryUsageMB = 512;
    }

    // ========================= 性能配置 =========================
    @Data
    @Validated
    public static class Performance {

        /**
         * 流式处理阈值（MB）- 超过此大小的文件将使用流式处理
         */
        @Min(value = 1, message = "Streaming threshold must be at least 1MB")
        private long streamingThresholdMB = 10;

        /**
         * 异步处理阈值 - 超过此行数的操作将异步处理
         */
        @Min(value = 100, message = "Async threshold must be at least 100")
        private int asyncThreshold = 1000;

        /**
         * 最大内存使用量（MB）
         */
        @Min(value = 128, message = "Max memory usage must be at least 128MB")
        private long maxMemoryUsageMB = 512;

        /**
         * 线程池核心线程数
         */
        @Min(value = 2, message = "Core pool size must be at least 2")
        private int corePoolSize = 4;

        /**
         * 线程池最大线程数
         */
        @Min(value = 4, message = "Max pool size must be at least 4")
        private int maxPoolSize = 16;

        /**
         * 线程池队列容量
         */
        @Min(value = 50, message = "Queue capacity must be at least 50")
        private int queueCapacity = 200;

        /**
         * 线程空闲时间（秒）
         */
        @Min(value = 30, message = "Keep alive time must be at least 30 seconds")
        private int keepAliveSeconds = 60;

        /**
         * 批处理大小
         */
        @Min(value = 100, message = "Batch size must be at least 100")
        private int batchSize = 1000;

        /**
         * 查询超时时间（秒）
         */
        @Min(value = 5, message = "Query timeout must be at least 5 seconds")
        private int queryTimeoutSeconds = 300;

        /**
         * 是否启用压缩
         */
        private boolean enableCompression = true;

        /**
         * 是否启用索引
         */
        private boolean enableIndexing = true;
    }

    // ========================= 存储配置 =========================
    @Data
    @Validated
    public static class Storage {

        /**
         * Excel文件基础路径
         */
        @NotBlank(message = "Base path cannot be blank")
        private String basePath = "/data/excel-files";

        /**
         * 索引文件路径
         */
        @NotBlank(message = "Index path cannot be blank")
        private String indexPath = "/data/excel-indexes";

        /**
         * 临时文件路径
         */
        @NotBlank(message = "Temp path cannot be blank")
        private String tempPath = "/tmp/excel-temp";

        /**
         * 备份文件路径
         */
        private String backupPath = "/data/excel-backup";

        /**
         * 支持的文件格式
         */
        private List<String> supportedFormats = Arrays.asList("xlsx", "csv", "xls");

        /**
         * 单个文件最大大小（MB）
         */
        @Min(value = 1, message = "Max file size must be at least 1MB")
        private long maxFileSizeMB = 100;

        /**
         * 是否启用文件监控
         */
        private boolean enableFileWatcher = true;

        /**
         * 文件清理策略 - 天数
         */
        @Min(value = 1, message = "File retention days must be at least 1")
        private int fileRetentionDays = 30;

        /**
         * 是否启用自动备份
         */
        private boolean enableAutoBackup = false;

        /**
         * 备份间隔（小时）
         */
        @Min(value = 1, message = "Backup interval must be at least 1 hour")
        private int backupIntervalHours = 24;
    }

    // ========================= 安全配置 =========================
    @Data
    public static class Security {

        /**
         * 是否启用访问控制
         */
        private boolean enableAccessControl = false;

        /**
         * 允许的操作
         */
        private List<String> allowedOperations = Arrays.asList("SELECT", "INSERT", "UPDATE", "DELETE");

        /**
         * 受限制的工作簿
         */
        private List<String> restrictedWorkbooks = new ArrayList<>();

        /**
         * 最大查询行数限制
         */
        @Min(value = 1, message = "Max query rows must be at least 1")
        private int maxQueryRows = 10000;

        /**
         * 是否记录审计日志
         */
        private boolean enableAuditLog = false;

        /**
         * 审计日志路径
         */
        private String auditLogPath = "/data/logs/excel-sql-audit.log";
    }

    // ========================= 监控配置 =========================
    @Data
    public static class Monitoring {

        /**
         * 是否启用性能监控
         */
        private boolean enablePerformanceMonitoring = true;

        /**
         * 是否启用健康检查
         */
        private boolean enableHealthCheck = true;

        /**
         * 指标收集间隔（秒）
         */
        @Min(value = 10, message = "Metrics interval must be at least 10 seconds")
        private int metricsIntervalSeconds = 60;

        /**
         * 慢查询阈值（毫秒）
         */
        @Min(value = 100, message = "Slow query threshold must be at least 100ms")
        private long slowQueryThresholdMs = 5000;

        /**
         * 是否启用JMX监控
         */
        private boolean enableJmx = true;

        /**
         * 警报配置
         */
        private Alert alert = new Alert();

        @Data
        public static class Alert {
            /**
             * 是否启用警报
             */
            private boolean enabled = false;

            /**
             * 内存使用率警报阈值（百分比）
             */
            @Min(value = 50) @Max(value = 95)
            private int memoryUsageThreshold = 80;

            /**
             * 缓存命中率警报阈值（百分比）
             */
            @Min(value = 10) @Max(value = 90)
            private int cacheHitRateThreshold = 50;

            /**
             * 错误率警报阈值（百分比）
             */
            @Min(value = 1) @Max(value = 50)
            private int errorRateThreshold = 10;

            /**
             * 警报通知方式
             */
            private List<String> notificationChannels = Arrays.asList("log");
        }
    }

    // ========================= 配置验证和工具方法 =========================

    /**
     * 配置验证
     */
    @PostConstruct
    public void validateConfig() {
        // 验证路径存在性
        validatePaths();

        // 验证缓存配置合理性
        validateCacheConfig();

        // 验证性能配置合理性
        validatePerformanceConfig();

        log.info("ExcelSQLConfig validation completed successfully");
    }

    private void validatePaths() {
        List<String> paths = Arrays.asList(
                storage.getBasePath(),
                storage.getIndexPath(),
                storage.getTempPath()
        );

        for (String path : paths) {
            try {
                Path dir = Paths.get(path);
                if (!Files.exists(dir)) {
                    Files.createDirectories(dir);
                    log.info("Created directory: {}", path);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create directory: " + path, e);
            }
        }
    }

    private void validateCacheConfig() {
        if (cache.getMaxSize() > cache.getMaxQueryResultSize()) {
            log.warn("Sheet cache size ({}) is larger than query result cache size ({})",
                    cache.getMaxSize(), cache.getMaxQueryResultSize());
        }

        if (cache.getMaxMemoryUsageMB() < 64) {
            throw new IllegalArgumentException("Cache max memory usage must be at least 64MB");
        }
    }

    private void validatePerformanceConfig() {
        if (performance.getCorePoolSize() > performance.getMaxPoolSize()) {
            throw new IllegalArgumentException("Core pool size cannot be greater than max pool size");
        }

        if (performance.getMaxMemoryUsageMB() < cache.getMaxMemoryUsageMB()) {
            log.warn("Performance max memory ({} MB) is less than cache max memory ({} MB)",
                    performance.getMaxMemoryUsageMB(), cache.getMaxMemoryUsageMB());
        }
    }

    // ========================= 便捷方法 =========================

    /**
     * 获取流式处理阈值（字节）
     */
    public long getStreamingThresholdBytes() {
        return performance.getStreamingThresholdMB() * 1024 * 1024;
    }

    /**
     * 获取最大内存使用量（字节）
     */
    public long getMaxMemoryUsageBytes() {
        return performance.getMaxMemoryUsageMB() * 1024 * 1024;
    }

    /**
     * 获取缓存过期时间（Duration）
     */
    public Duration getCacheExpireDuration() {
        return Duration.ofMinutes(cache.getExpireAfterWriteMinutes());
    }

    /**
     * 是否启用流式处理
     */
    public boolean shouldUseStreaming(long fileSizeBytes) {
        return fileSizeBytes > getStreamingThresholdBytes();
    }

    /**
     * 是否应该异步处理
     */
    public boolean shouldProcessAsync(int rowCount) {
        return rowCount > performance.getAsyncThreshold();
    }

    /**
     * 获取文件最大大小（字节）
     */
    public long getMaxFileSizeBytes() {
        return storage.getMaxFileSizeMB() * 1024 * 1024;
    }
}
