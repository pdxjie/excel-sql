package com.excel.sql.engine.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 存储路径请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoragePathRequest {
    
    /**
     * 新的存储路径
     */
    @NotBlank(message = "存储路径不能为空")
    private String path;
    
    /**
     * 是否迁移现有文件
     */
    private boolean migrateFiles = true;
} 