package com.excelsql.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
/**
 * @Description: 主配置类
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:40
 */

@Configuration
@ConfigurationProperties(prefix = "excel-sql")
public class ExcelSQLConfig {

    private Cache cache = new Cache();
    private Performance performance = new Performance();
    private Storage storage = new Storage();

    public static class Cache {
        private int maxSize = 1000;
        private String expireAfterWrite = "30m";

        // getters and setters
        public int getMaxSize() { return maxSize; }
        public void setMaxSize(int maxSize) { this.maxSize = maxSize; }
        public String getExpireAfterWrite() { return expireAfterWrite; }
        public void setExpireAfterWrite(String expireAfterWrite) { this.expireAfterWrite = expireAfterWrite; }
    }

    public static class Performance {
        private String streamingThreshold = "10MB";
        private int asyncThreshold = 1000;
        private String maxMemoryUsage = "512MB";

        // getters and setters
        public String getStreamingThreshold() { return streamingThreshold; }
        public void setStreamingThreshold(String streamingThreshold) { this.streamingThreshold = streamingThreshold; }
        public int getAsyncThreshold() { return asyncThreshold; }
        public void setAsyncThreshold(int asyncThreshold) { this.asyncThreshold = asyncThreshold; }
        public String getMaxMemoryUsage() { return maxMemoryUsage; }
        public void setMaxMemoryUsage(String maxMemoryUsage) { this.maxMemoryUsage = maxMemoryUsage; }
    }

    public static class Storage {
        private String basePath = "/data/excel-files";
        private String indexPath = "/data/excel-indexes";
        private String tempPath = "/tmp/excel-temp";

        // getters and setters
        public String getBasePath() { return basePath; }
        public void setBasePath(String basePath) { this.basePath = basePath; }
        public String getIndexPath() { return indexPath; }
        public void setIndexPath(String indexPath) { this.indexPath = indexPath; }
        public String getTempPath() { return tempPath; }
        public void setTempPath(String tempPath) { this.tempPath = tempPath; }
    }

    // Main getters and setters
    public Cache getCache() { return cache; }
    public void setCache(Cache cache) { this.cache = cache; }
    public Performance getPerformance() { return performance; }
    public void setPerformance(Performance performance) { this.performance = performance; }
    public Storage getStorage() { return storage; }
    public void setStorage(Storage storage) { this.storage = storage; }
}
