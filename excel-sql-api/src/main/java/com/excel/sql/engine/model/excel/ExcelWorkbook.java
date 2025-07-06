package com.excel.sql.engine.model.excel;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Excel工作簿模型
 * 对应数据库中的Database概念
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"sheets"})
public class ExcelWorkbook {
    
    /**
     * 工作簿ID
     */
    private Long id;
    
    /**
     * 工作簿名称（文件名）
     */
    private String name;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 文件类型（XLSX, XLS, CSV）
     */
    private ExcelFileType fileType;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;
    
    /**
     * 工作表集合，key为工作表名称
     */
    private Map<String, ExcelSheet> sheets;
    
    /**
     * 文件对象
     */
    private transient File file;
    
    /**
     * 是否已加载
     */
    private transient boolean loaded;
    
    /**
     * 添加工作表
     *
     * @param sheet 工作表
     */
    public void addSheet(ExcelSheet sheet) {
        if (sheets == null) {
            sheets = new HashMap<>();
        }
        sheets.put(sheet.getName(), sheet);
    }
    
    /**
     * 获取工作表
     *
     * @param sheetName 工作表名称
     * @return 工作表
     */
    public ExcelSheet getSheet(String sheetName) {
        return sheets != null ? sheets.get(sheetName) : null;
    }
    
    /**
     * 检查工作簿是否需要重新加载
     *
     * @return 是否需要重新加载
     */
    public boolean needsReload() {
        if (file == null || !file.exists()) {
            return true;
        }
        
        LocalDateTime fileLastModified = LocalDateTime.ofInstant(
                java.nio.file.attribute.FileTime.fromMillis(file.lastModified()).toInstant(),
                java.time.ZoneId.systemDefault());
        
        return fileLastModified.isAfter(lastModified);
    }
} 