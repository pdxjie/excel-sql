package com.excel.sql.engine.model.excel;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Excel行模型
 * 对应数据库中的Row概念
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"sheet"})
public class ExcelRow {
    
    /**
     * 行号（从0开始）
     */
    private Integer rowNum;
    
    /**
     * 所属工作表
     */
    private transient ExcelSheet sheet;
    
    /**
     * 单元格数据，key为列名
     */
    private Map<String, Object> cells;
    
    /**
     * 原始单元格数据，key为列索引
     */
    private transient Map<Integer, String> rawCells;
    
    /**
     * 构造函数
     *
     * @param rowNum 行号
     * @param sheet 工作表
     */
    public ExcelRow(Integer rowNum, ExcelSheet sheet) {
        this.rowNum = rowNum;
        this.sheet = sheet;
        this.cells = new HashMap<>();
        this.rawCells = new HashMap<>();
    }
    
    /**
     * 设置单元格值
     *
     * @param columnName 列名
     * @param value 值
     */
    public void setCellValue(String columnName, Object value) {
        if (cells == null) {
            cells = new HashMap<>();
        }
        cells.put(columnName, value);
    }
    
    /**
     * 设置原始单元格值
     *
     * @param columnIndex 列索引
     * @param value 原始值
     */
    public void setRawCellValue(Integer columnIndex, String value) {
        if (rawCells == null) {
            rawCells = new HashMap<>();
        }
        rawCells.put(columnIndex, value);
    }
    
    /**
     * 获取单元格值
     *
     * @param columnName 列名
     * @return 单元格值
     */
    public Object getCellValue(String columnName) {
        return cells != null ? cells.get(columnName) : null;
    }
    
    /**
     * 获取原始单元格值
     *
     * @param columnIndex 列索引
     * @return 原始单元格值
     */
    public String getRawCellValue(Integer columnIndex) {
        return rawCells != null ? rawCells.get(columnIndex) : null;
    }
    
    /**
     * 从原始数据解析并填充单元格值
     */
    public void parseFromRawData() {
        if (sheet == null || sheet.getColumns() == null || rawCells == null) {
            return;
        }
        
        List<ExcelColumn> columns = sheet.getColumns();
        for (ExcelColumn column : columns) {
            String rawValue = rawCells.get(column.getColumnIndex());
            Object parsedValue = column.parseValue(rawValue);
            setCellValue(column.getName(), parsedValue);
        }
    }
    
    /**
     * 获取行的所有列名
     *
     * @return 列名集合
     */
    public Iterable<String> getColumnNames() {
        return cells != null ? cells.keySet() : List.of();
    }
    
    /**
     * 检查行是否包含指定列
     *
     * @param columnName 列名
     * @return 是否包含
     */
    public boolean hasColumn(String columnName) {
        return cells != null && cells.containsKey(columnName);
    }
} 