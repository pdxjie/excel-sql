package com.excel.sql.engine.model.excel;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Excel工作表模型
 * 对应数据库中的Table概念
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"rows", "primaryIndex", "secondaryIndexes"})
public class ExcelSheet {
    
    /**
     * 工作表ID
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
     * 列定义列表
     */
    private List<ExcelColumn> columns;
    
    /**
     * 数据行列表（懒加载）
     */
    private transient List<ExcelRow> rows;
    
    /**
     * 主索引，基于行号的快速访问
     */
    private transient Map<Integer, ExcelRow> primaryIndex;
    
    /**
     * 辅助索引，基于列值的快速查找
     * 格式：columnName -> columnValue -> List<ExcelRow>
     */
    private transient Map<String, Map<Object, List<ExcelRow>>> secondaryIndexes;
    
    /**
     * 是否已加载数据
     */
    private transient boolean dataLoaded;
    
    /**
     * 是否已创建索引
     */
    private transient boolean indexed;
    
    /**
     * 初始化索引
     */
    public void initializeIndexes() {
        if (primaryIndex == null) {
            primaryIndex = new ConcurrentHashMap<>();
        }
        
        if (secondaryIndexes == null) {
            secondaryIndexes = new ConcurrentHashMap<>();
        }
        
        indexed = true;
    }
    
    /**
     * 添加行到主索引
     *
     * @param row 行
     */
    public void addToPrimaryIndex(ExcelRow row) {
        if (primaryIndex == null) {
            initializeIndexes();
        }
        
        primaryIndex.put(row.getRowNum(), row);
        
        // 如果rows为null，初始化它
        if (rows == null) {
            rows = new java.util.ArrayList<>();
        }
        
        // 添加到rows列表
        rows.add(row);
    }
    
    /**
     * 获取列定义
     *
     * @param columnName 列名
     * @return 列定义
     */
    public ExcelColumn getColumn(String columnName) {
        if (columns == null) {
            return null;
        }
        
        for (ExcelColumn column : columns) {
            if (column.getName().equals(columnName)) {
                return column;
            }
        }
        
        return null;
    }
    
    /**
     * 获取所有列名
     *
     * @return 列名列表
     */
    public List<String> getColumnNames() {
        if (columns == null) {
            return java.util.Collections.emptyList();
        }
        
        return columns.stream()
                .map(ExcelColumn::getName)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 加载Excel文件中的行数据
     * 
     * @param filePath Excel文件路径
     * @return 是否加载成功
     */
    public boolean loadRows(String filePath) {
        if (dataLoaded) {
            return true; // 已经加载过数据，不需要重复加载
        }
        
        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) {
            return false;
        }
        
        // 默认加载全部数据
        return loadRows(filePath, 0, Integer.MAX_VALUE);
    }
    
    /**
     * 分页加载Excel文件中的行数据
     * 
     * @param filePath Excel文件路径
     * @param startRow 起始行（相对于dataStartRowIndex）
     * @param maxRows 最大加载行数
     * @return 是否加载成功
     */
    public boolean loadRows(String filePath, int startRow, int maxRows) {
        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) {
            return false;
        }
        
        try {
            // 初始化行列表和索引
            if (rows == null) {
                rows = new java.util.ArrayList<>();
            } else {
                rows.clear();
            }
            
            if (primaryIndex == null) {
                initializeIndexes();
            } else {
                primaryIndex.clear();
            }
            
            // 确定数据开始行
            int dataStart = dataStartRowIndex != null ? dataStartRowIndex : 1;
            int actualStartRow = dataStart + startRow;
            
            if (filePath.toLowerCase().endsWith(".xlsx")) {
                // 对于XLSX文件，使用更简单可靠的方法
                try (java.io.FileInputStream fis = new java.io.FileInputStream(file);
                     org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(fis)) {
                    
                    // 获取工作表
                    org.apache.poi.ss.usermodel.Sheet poiSheet = null;
                    if (sheetIndex != null) {
                        if (sheetIndex < workbook.getNumberOfSheets()) {
                            poiSheet = workbook.getSheetAt(sheetIndex);
                        }
                    } else if (name != null) {
                        poiSheet = workbook.getSheet(name);
                    }
                    
                    if (poiSheet == null) {
                        return false;
                    }
                    
                    // 计算结束行
                    int endRow = Math.min(poiSheet.getLastRowNum(), actualStartRow + maxRows - 1);
                    
                    // 遍历指定范围的行
                    for (int rowIndex = actualStartRow; rowIndex <= endRow; rowIndex++) {
                        org.apache.poi.ss.usermodel.Row poiRow = poiSheet.getRow(rowIndex);
                        if (poiRow == null) {
                            continue;
                        }
                        
                        processRow(poiRow, rowIndex);
                    }
                }
            } else if (filePath.toLowerCase().endsWith(".xls")) {
                // 对于XLS文件，使用标准API
                try (java.io.FileInputStream fis = new java.io.FileInputStream(file);
                     org.apache.poi.hssf.usermodel.HSSFWorkbook workbook = new org.apache.poi.hssf.usermodel.HSSFWorkbook(fis)) {
                    
                    // 获取工作表
                    org.apache.poi.ss.usermodel.Sheet poiSheet = null;
                    if (sheetIndex != null) {
                        if (sheetIndex < workbook.getNumberOfSheets()) {
                            poiSheet = workbook.getSheetAt(sheetIndex);
                        }
                    } else if (name != null) {
                        poiSheet = workbook.getSheet(name);
                    }
                    
                    if (poiSheet == null) {
                        return false;
                    }
                    
                    // 计算结束行
                    int endRow = Math.min(poiSheet.getLastRowNum(), actualStartRow + maxRows - 1);
                    
                    // 遍历指定范围的行
                    for (int rowIndex = actualStartRow; rowIndex <= endRow; rowIndex++) {
                        org.apache.poi.ss.usermodel.Row poiRow = poiSheet.getRow(rowIndex);
                        if (poiRow == null) {
                            continue;
                        }
                        
                        processRow(poiRow, rowIndex);
                    }
                }
            } else {
                return false; // 不支持的文件类型
            }
            
            dataLoaded = true;
            return true;
            
        } catch (Exception e) {
            // 记录异常但不抛出，返回加载失败
            System.err.println("加载Excel行数据失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 处理单个Excel行
     * 
     * @param poiRow POI行对象
     * @param rowIndex 行索引
     */
    private void processRow(org.apache.poi.ss.usermodel.Row poiRow, int rowIndex) {
        // 创建行对象
        ExcelRow excelRow = new ExcelRow(rowIndex, this);
        
        // 处理每个单元格
        for (ExcelColumn column : columns) {
            if (column.getIndex() == null) {
                continue;
            }
            
            org.apache.poi.ss.usermodel.Cell cell = poiRow.getCell(column.getIndex());
            if (cell == null) {
                continue;
            }
            
            // 根据单元格类型获取值
            Object cellValue = getCellValue(cell);
            excelRow.setCellValue(column.getName(), cellValue);
        }
        
        // 添加行到索引和列表
        addToPrimaryIndex(excelRow);
    }
    
    /**
     * 从POI单元格获取值
     * 
     * @param cell POI单元格
     * @return 单元格值
     */
    private Object getCellValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // 检查是否为日期
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }
                // 避免科学计数法显示
                double numericValue = cell.getNumericCellValue();
                if ((numericValue == Math.floor(numericValue)) && !Double.isInfinite(numericValue)) {
                    return (long) numericValue;
                }
                return numericValue;
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                try {
                    // 尝试获取公式计算结果
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception ex) {
                        return cell.getCellFormula();
                    }
                }
            case BLANK:
                return null;
            default:
                return null;
        }
    }
} 