package com.excel.sql.engine.model.entity;

import com.excel.sql.engine.model.excel.ExcelDataType;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 列实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ColumnEntity {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 所属工作表ID
     */
    private Long sheetId;
    
    /**
     * 列名
     */
    private String name;
    
    /**
     * 列索引（从0开始）
     */
    private Integer columnIndex;
    
    /**
     * 数据类型
     */
    private String dataType;
    
    /**
     * 是否可为空
     */
    private Boolean nullable;
    
    /**
     * 是否为索引列
     */
    private Boolean indexed;
    
    /**
     * 格式模式（如日期格式）
     */
    private String formatPattern;
    
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
     * Excel数据类型枚举（非数据库字段）
     */
    private ExcelDataType excelDataType;
    
    /**
     * 获取Excel数据类型枚举
     *
     * @return Excel数据类型
     */
    public ExcelDataType getExcelDataType() {
        return dataType != null ? ExcelDataType.valueOf(dataType) : null;
    }
    
    /**
     * 设置Excel数据类型枚举
     *
     * @param excelDataType Excel数据类型
     */
    public void setExcelDataType(ExcelDataType excelDataType) {
        this.dataType = excelDataType != null ? excelDataType.name() : null;
        this.excelDataType = excelDataType;
    }
} 