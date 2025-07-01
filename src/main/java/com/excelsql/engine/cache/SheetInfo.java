package com.excelsql.engine.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.bval.util.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 工作表信息类
 * 存储Excel工作表的基本信息和元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SheetInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 工作表名称
     */
    private String sheetName;
    
    /**
     * 工作表索引（从0开始）
     */
    private int sheetIndex;
    
    /**
     * 数据行数（不包括表头）
     */
    private int rowCount;
    
    /**
     * 列数
     */
    private int columnCount;
    
    /**
     * 是否有表头
     */
    private boolean hasHeader;
    
    /**
     * 表头信息
     */
    private List<ColumnInfo> columnInfos;
    
    /**
     * 数据起始行（通常为1，如果有表头）
     */
    private int dataStartRow;
    
    /**
     * 数据结束行
     */
    private int dataEndRow;
    
    /**
     * 工作表类型
     */
    private SheetType sheetType;
    
    /**
     * 是否受保护
     */
    private boolean isProtected;
    
    /**
     * 工作表可见性
     */
    private SheetVisibility visibility;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdatedTime;
    
    /**
     * 数据校验和（用于检测数据变化）
     */
    private String dataChecksum;
    
    /**
     * 自定义属性
     */
    private Map<String, Object> customProperties;
    
    @Builder
    public SheetInfo(String sheetName, int sheetIndex, int rowCount, int columnCount, 
                    boolean hasHeader, List<ColumnInfo> columnInfos) {
        this.sheetName = sheetName;
        this.sheetIndex = sheetIndex;
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.hasHeader = hasHeader;
        this.columnInfos = columnInfos != null ? new ArrayList<>(columnInfos) : new ArrayList<>();
        this.dataStartRow = hasHeader ? 1 : 0;
        this.dataEndRow = rowCount > 0 ? rowCount - 1 : 0;
        this.sheetType = SheetType.WORKSHEET;
        this.isProtected = false;
        this.visibility = SheetVisibility.VISIBLE;
        this.createdTime = LocalDateTime.now();
        this.lastUpdatedTime = LocalDateTime.now();
        this.customProperties = new HashMap<>();
    }
    
    /**
     * 获取指定名称的列信息
     */
    public Optional<ColumnInfo> getColumnInfo(String columnName) {
        if (columnInfos == null || StringUtils.isBlank(columnName)) {
            return Optional.empty();
        }
        
        return columnInfos.stream()
                .filter(col -> columnName.equals(col.getColumnName()))
                .findFirst();
    }
    
    /**
     * 工作表类型枚举
     */
    public enum SheetType {
        WORKSHEET("普通工作表"),
        CHART("图表工作表"),
        MACRO("宏工作表"),
        DIALOG("对话框工作表");
        
        private final String description;
        
        SheetType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 工作表可见性枚举
     */
    public enum SheetVisibility {
        VISIBLE("可见"),
        HIDDEN("隐藏"),
        VERY_HIDDEN("完全隐藏");
        
        private final String description;
        
        SheetVisibility(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}