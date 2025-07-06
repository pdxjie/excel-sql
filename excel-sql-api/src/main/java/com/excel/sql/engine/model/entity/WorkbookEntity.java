package com.excel.sql.engine.model.entity;

import com.excel.sql.engine.model.excel.ExcelFileType;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作簿实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WorkbookEntity {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 工作簿名称
     */
    private String name;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;
    
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
    
    /**
     * Excel文件类型枚举（非数据库字段）
     */
    private ExcelFileType excelFileType;
    
    /**
     * 获取Excel文件类型枚举
     *
     * @return Excel文件类型
     */
    public ExcelFileType getExcelFileType() {
        return fileType != null ? ExcelFileType.valueOf(fileType) : null;
    }
    
    /**
     * 设置Excel文件类型枚举
     *
     * @param excelFileType Excel文件类型
     */
    public void setExcelFileType(ExcelFileType excelFileType) {
        this.fileType = excelFileType != null ? excelFileType.name() : null;
        this.excelFileType = excelFileType;
    }
} 