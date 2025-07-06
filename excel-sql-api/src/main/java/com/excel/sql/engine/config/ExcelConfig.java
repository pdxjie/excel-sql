package com.excel.sql.engine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Excel配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "excel-sql")
public class ExcelConfig {
    
    /**
     * 文件存储配置
     */
    private Storage storage = new Storage();
    
    /**
     * 缓存配置
     */
    private Cache cache = new Cache();
    
    /**
     * 索引配置
     */
    private Index index = new Index();
    
    /**
     * 查询配置
     */
    private Query query = new Query();
    
    /**
     * 文件存储配置
     */
    @Data
    public static class Storage {
        /**
         * 基础路径
         */
        private String basePath = "./excel-files";
        
        /**
         * 临时路径
         */
        private String tempPath = "./excel-files/temp";
    }
    
    /**
     * 缓存配置
     */
    @Data
    public static class Cache {
        /**
         * L1缓存大小
         */
        private int l1Size = 200;
        
        /**
         * L2缓存过期时间（秒）
         */
        private int l2Ttl = 3600;
    }
    
    /**
     * 索引配置
     */
    @Data
    public static class Index {
        /**
         * 是否自动创建索引
         */
        private boolean autoCreate = true;
        
        /**
         * 重建索引阈值
         */
        private int rebuildThreshold = 1000;
    }
    
    /**
     * 查询配置
     */
    @Data
    public static class Query {
        /**
         * 超时时间（秒）
         */
        private int timeoutSeconds = 30;
        
        /**
         * 最大返回行数
         */
        private int maxRows = 10000;
        
        /**
         * 批处理大小
         */
        private int batchSize = 1000;
        
        /**
         * 页大小
         */
        private int pageSize = 1000;
        
        /**
         * 是否使用缓存
         */
        private boolean useCache = true;
        
        /**
         * 缓存过期时间（秒）
         */
        private int cacheTtl = 300;
    }
} 