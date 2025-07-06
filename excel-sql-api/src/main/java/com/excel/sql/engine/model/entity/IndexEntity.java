package com.excel.sql.engine.model.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 索引实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class IndexEntity {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 所属工作表ID
     */
    private Long sheetId;
    
    /**
     * 索引名称
     */
    private String name;
    
    /**
     * 索引类型（PRIMARY, SECONDARY, COMPOSITE）
     */
    private String indexType;
    
    /**
     * 索引列（多列索引用逗号分隔）
     */
    private String columns;
    
    /**
     * 是否唯一索引
     */
    private Boolean unique;
    
    /**
     * 索引元数据（JSON格式）
     */
    private String metadata;
    
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
     * 获取索引列数组
     *
     * @return 索引列数组
     */
    public String[] getColumnArray() {
        return columns != null ? columns.split(",") : new String[0];
    }
    
    /**
     * 设置索引列数组
     *
     * @param columnArray 索引列数组
     */
    public void setColumnArray(String[] columnArray) {
        this.columns = String.join(",", columnArray);
    }
} 