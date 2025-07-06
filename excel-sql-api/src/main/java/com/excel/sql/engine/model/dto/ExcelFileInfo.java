package com.excel.sql.engine.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Excel文件信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelFileInfo {
    
    /**
     * 文件名（不含扩展名）
     */
    private String name;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 文件大小（字节）
     */
    private long fileSize;
    
    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;
    
    /**
     * 工作表列表
     */
    private List<String> sheets;
} 