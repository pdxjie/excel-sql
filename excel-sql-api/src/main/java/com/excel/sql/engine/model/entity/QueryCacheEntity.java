package com.excel.sql.engine.model.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 查询缓存实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class QueryCacheEntity {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 工作簿ID
     */
    private Long workbookId;
    
    /**
     * 缓存键
     */
    private String cacheKey;
    
    /**
     * SQL查询语句
     */
    private String sqlQuery;
    
    /**
     * 查询结果（JSON格式）
     */
    private String resultJson;
    
    /**
     * 执行时间（毫秒）
     */
    private Long executionTime;
    
    /**
     * 命中次数
     */
    private Integer hitCount;
    
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 是否删除（0-未删除，1-已删除）
     */
    private Integer deleted;
} 