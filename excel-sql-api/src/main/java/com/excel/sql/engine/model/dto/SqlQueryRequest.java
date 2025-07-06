package com.excel.sql.engine.model.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * SQL查询请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlQueryRequest {
    
    /**
     * 工作簿名称（文件名）
     * 如果不提供，则使用默认工作簿
     */
    private String workbook;
    
    /**
     * SQL查询语句
     */
    @NotBlank(message = "SQL查询语句不能为空")
    private String sql;
    
    /**
     * 是否使用缓存
     */
    @Builder.Default
    private Boolean useCache = true;
    
    /**
     * 查询超时时间（秒）
     */
    @Builder.Default
    private Integer timeout = 30;
    
    /**
     * 最大返回行数
     */
    @Builder.Default
    private Integer maxRows = 10000;
} 