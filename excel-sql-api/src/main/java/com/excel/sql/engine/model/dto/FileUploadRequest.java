package com.excel.sql.engine.model.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.Min;

/**
 * 文件上传请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRequest {
    
    /**
     * 自定义工作簿名称（可选）
     * 如果不提供，则使用文件名
     */
    private String workbookName;
    
    /**
     * 表头行索引（从0开始）
     * 默认为0，即第一行为表头
     */
    @Builder.Default
    @Min(value = 0, message = "表头行索引不能小于0")
    private Integer headerRowIndex = 0;
    
    /**
     * 数据开始行索引（从0开始）
     * 默认为1，即第二行开始为数据
     */
    @Builder.Default
    @Min(value = 0, message = "数据开始行索引不能小于0")
    private Integer dataStartRowIndex = 1;
    
    /**
     * 是否覆盖同名文件
     */
    @Builder.Default
    private Boolean overwrite = false;
    
    /**
     * 是否自动创建索引
     */
    @Builder.Default
    private Boolean createIndex = true;
}