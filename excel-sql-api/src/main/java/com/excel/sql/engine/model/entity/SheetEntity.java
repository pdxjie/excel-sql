package com.excel.sql.engine.model.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作表实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SheetEntity {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 所属工作簿ID
     */
    private Long workbookId;
    
    /**
     * 工作表名称
     */
    private String name;
    
    /**
     * 工作表索引
     */
    private Integer sheetIndex;
    
    /**
     * 表头行索引（从0开始）
     */
    private Integer headerRowIndex;
    
    /**
     * 数据开始行索引（从0开始）
     */
    private Integer dataStartRowIndex;
    
    /**
     * 总行数
     */
    private Integer totalRows;
    
    /**
     * 是否已创建索引
     */
    private Boolean indexed;
    
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