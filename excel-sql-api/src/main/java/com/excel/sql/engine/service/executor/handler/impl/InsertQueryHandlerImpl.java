package com.excel.sql.engine.service.executor.handler.impl;

import com.excel.sql.engine.model.dto.SqlQueryResult;
import com.excel.sql.engine.model.excel.ExcelColumn;
import com.excel.sql.engine.model.excel.ExcelRow;
import com.excel.sql.engine.model.excel.ExcelSheet;
import com.excel.sql.engine.model.excel.ExcelWorkbook;
import com.excel.sql.engine.service.CacheService;
import com.excel.sql.engine.service.ExcelFileService;
import com.excel.sql.engine.service.executor.handler.InsertQueryHandler;
import com.excel.sql.engine.service.parser.ParsedSql;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * INSERT查询处理器实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InsertQueryHandlerImpl implements InsertQueryHandler {
    
    private final ExcelFileService excelFileService;
    private final CacheService cacheService;
    
    @Override
    public SqlQueryResult handle(ParsedSql parsedSql, String workbook) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 检查工作簿是否存在
            if (!excelFileService.existsWorkbook(workbook)) {
                return SqlQueryResult.error("工作簿不存在: " + workbook);
            }
            
            // 获取目标表（工作表）
            if (parsedSql.getTargetTables() == null || parsedSql.getTargetTables().isEmpty()) {
                return SqlQueryResult.error("未指定插入表");
            }
            
            String sheetName = parsedSql.getTargetTables().get(0);
            
            // 检查工作表是否存在
            if (!excelFileService.existsSheet(workbook, sheetName)) {
                return SqlQueryResult.error("工作表不存在: " + sheetName);
            }
            
            // 获取工作表
            ExcelSheet sheet = excelFileService.getSheet(workbook, sheetName);
            
            // 检查插入值
            if (parsedSql.getInsertValues() == null || parsedSql.getInsertValues().isEmpty()) {
                return SqlQueryResult.error("未指定插入值");
            }
            
            // 执行插入
            int affectedRows = insertRows(sheet, parsedSql.getInsertValues(), workbook);
            
            // 创建查询结果
            long executionTime = System.currentTimeMillis() - startTime;
            return SqlQueryResult.successDml(affectedRows, SqlQueryResult.SqlType.INSERT, executionTime);
            
        } catch (Exception e) {
            log.error("执行INSERT查询异常: {}", e.getMessage(), e);
            return SqlQueryResult.error("执行INSERT查询异常: " + e.getMessage());
        }
    }
    
    /**
     * 插入行数据
     *
     * @param sheet 工作表
     * @param insertValues 插入值列表
     * @param workbookName 工作簿名称（备用，当workbookId为空时使用）
     * @return 影响的行数
     */
    private int insertRows(ExcelSheet sheet, java.util.List<Map<String, Object>> insertValues, String workbookName) {
        if (sheet == null || insertValues == null || insertValues.isEmpty()) {
            return 0;
        }
        
        int insertedRows = 0;
        String workbookPath = null;
        
        try {
            // 获取工作簿文件路径
            if (sheet.getWorkbookId() != null) {
                workbookPath = excelFileService.getWorkbookPathById(sheet.getWorkbookId());
            }
            
            // 如果通过ID无法获取路径，尝试通过名称获取
            if (workbookPath == null && workbookName != null) {
                ExcelWorkbook workbook = excelFileService.loadWorkbook(workbookName);
                if (workbook != null) {
                    workbookPath = workbook.getFilePath();
                }
            }
            
            if (workbookPath == null) {
                log.error("无法获取工作簿路径");
                throw new IllegalStateException("无法获取工作簿路径");
            }
            
            // 打开工作簿文件
            java.io.File file = new java.io.File(workbookPath);
            if (!file.exists()) {
                log.error("工作簿文件不存在: {}", workbookPath);
                throw new IllegalStateException("工作簿文件不存在: " + workbookPath);
            }
            
            org.apache.poi.ss.usermodel.Workbook workbook = null;
            
            try {
                // 直接使用XSSFWorkbook，不使用SXSSF模式
                try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                    workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(fis);
                } catch (Exception e) {
                    log.warn("使用XSSFWorkbook读取文件失败: {}", e.getMessage());
                    // 如果读取失败，创建新的空工作簿
                    workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                    workbook.createSheet(sheet.getName());
                }
                
                // 获取工作表
                org.apache.poi.ss.usermodel.Sheet poiSheet = workbook.getSheet(sheet.getName());
                if (poiSheet == null) {
                    log.warn("工作表不存在: {}，创建新表", sheet.getName());
                    poiSheet = workbook.createSheet(sheet.getName());
                    
                    // 如果是新创建的表，需要创建表头行
                    if (sheet.getColumns() != null && !sheet.getColumns().isEmpty()) {
                        int headerRowIndex = sheet.getHeaderRowIndex() != null ? sheet.getHeaderRowIndex() : 0;
                        org.apache.poi.ss.usermodel.Row headerRow = poiSheet.createRow(headerRowIndex);
                        
                        for (ExcelColumn column : sheet.getColumns()) {
                            if (column.getIndex() != null) {
                                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(column.getIndex());
                                cell.setCellValue(column.getName());
                            }
                        }
                    }
                }
                
                // 获取当前最大行号
                int lastRowNum = poiSheet.getLastRowNum();
                int dataStartRowIndex = sheet.getDataStartRowIndex() != null ? sheet.getDataStartRowIndex() : 1;
                
                // 确保nextRowNum大于lastRowNum，避免尝试覆盖已写入的行
                int nextRowNum = Math.max(lastRowNum + 1, dataStartRowIndex);
                
                log.info("开始插入数据，起始行号: {}", nextRowNum);
                
                // 处理每一行数据
                for (Map<String, Object> rowData : insertValues) {
                    try {
                        // 检查是否是自动列模式（未指定列的INSERT语句）
                        boolean isAutoColumns = rowData.containsKey("__AUTO_COLUMNS__") && (Boolean) rowData.get("__AUTO_COLUMNS__");
                        
                        if (isAutoColumns) {
                            // 自动列模式，需要根据工作表的列顺序来映射值
                            if (sheet.getColumns() == null || sheet.getColumns().isEmpty()) {
                                log.error("工作表 '{}' 没有定义列，无法执行未指定列的INSERT语句", sheet.getName());
                                throw new IllegalArgumentException("工作表 '" + sheet.getName() + "' 没有定义列，无法执行未指定列的INSERT语句");
                            }
                            
                            // 提取所有值
                            List<Object> values = new ArrayList<>();
                            int valueIndex = 0;
                            while (rowData.containsKey("__VALUE_" + valueIndex + "__")) {
                                values.add(rowData.get("__VALUE_" + valueIndex + "__"));
                                valueIndex++;
                            }
                            
                            // 检查值的数量是否与列的数量匹配
                            if (values.size() != sheet.getColumns().size()) {
                                StringBuilder missingInfo = new StringBuilder();
                                if (values.size() < sheet.getColumns().size()) {
                                    // 缺少列
                                    missingInfo.append("缺少列: ");
                                    for (int i = values.size(); i < sheet.getColumns().size(); i++) {
                                        if (i > values.size()) missingInfo.append(", ");
                                        missingInfo.append(sheet.getColumns().get(i).getName());
                                    }
                                } else {
                                    // 多余的值
                                    missingInfo.append("多余的值: 提供了 ").append(values.size())
                                            .append(" 个值，但工作表只有 ").append(sheet.getColumns().size()).append(" 列");
                                }
                                
                                log.error("INSERT语句中值的数量 ({}) 与工作表 '{}' 的列数量 ({}) 不匹配. {}", 
                                        values.size(), sheet.getName(), sheet.getColumns().size(), missingInfo);
                                throw new IllegalArgumentException("INSERT语句中值的数量 (" + values.size() + 
                                        ") 与工作表 '" + sheet.getName() + "' 的列数量 (" + sheet.getColumns().size() + 
                                        ") 不匹配. " + missingInfo);
                            }
                            
                            // 创建新行
                            org.apache.poi.ss.usermodel.Row poiRow = poiSheet.createRow(nextRowNum);
                            
                            // 创建内存中的Excel行对象
                            ExcelRow newRow = new ExcelRow(nextRowNum, sheet);
                            
                            // 按列顺序设置值
                            for (int i = 0; i < sheet.getColumns().size(); i++) {
                                ExcelColumn column = sheet.getColumns().get(i);
                                String columnName = column.getName();
                                Object value = values.get(i);
                                
                                // 创建单元格
                                org.apache.poi.ss.usermodel.Cell cell = poiRow.createCell(column.getIndex());
                                
                                // 根据值类型设置单元格值
                                setCellValue(cell, value);
                                
                                // 同时更新内存中的模型
                                newRow.setCellValue(columnName, value);
                            }
                            
                            // 添加行到内存模型的索引
                            sheet.addToPrimaryIndex(newRow);
                            
                        } else {
                            // 普通模式，验证所有列是否都存在于工作表中
                            for (String columnName : rowData.keySet()) {
                                if (Objects.isNull(sheet.getColumn(columnName))) {
                                    log.error("列 '{}' 在工作表 '{}' 中不存在", columnName, sheet.getName());
                                    throw new IllegalArgumentException("列 '" + columnName + "' 在工作表 '" + sheet.getName() + "' 中不存在");
                                }
                            }
                            
                            // 创建新行
                            org.apache.poi.ss.usermodel.Row poiRow = poiSheet.createRow(nextRowNum);
                            
                            // 创建内存中的Excel行对象
                            ExcelRow newRow = new ExcelRow(nextRowNum, sheet);
                            
                            // 设置单元格值
                            for (Map.Entry<String, Object> entry : rowData.entrySet()) {
                                String columnName = entry.getKey();
                                Object value = entry.getValue();
                                
                                // 获取列定义
                                ExcelColumn column = sheet.getColumn(columnName);
                                if (column == null) {
                                    continue; // 已经验证过，这里不应该发生
                                }
                                
                                // 创建单元格
                                org.apache.poi.ss.usermodel.Cell cell = poiRow.createCell(column.getIndex());
                                
                                // 根据值类型设置单元格值
                                setCellValue(cell, value);
                                
                                // 同时更新内存中的模型
                                newRow.setCellValue(columnName, value);
                            }
                            
                            // 添加行到内存模型的索引
                            sheet.addToPrimaryIndex(newRow);
                        }
                        
                        // 更新下一行行号
                        nextRowNum++;
                        insertedRows++;
                        
                    } catch (Exception e) {
                        log.error("插入行异常: {}", e.getMessage(), e);
                        throw new RuntimeException("插入行异常: " + e.getMessage(), e);
                    }
                }
                
                // 更新工作表的总行数
                sheet.setTotalRows(Math.max(sheet.getTotalRows() != null ? sheet.getTotalRows() : 0, nextRowNum));
                
                // 直接将工作簿保存到原文件
                try (java.io.FileOutputStream fileOut = new java.io.FileOutputStream(file)) {
                    workbook.write(fileOut);
                    log.info("成功将数据写入文件: {}", file.getAbsolutePath());
                    
                    // 清除相关缓存
                    cacheService.clearFilePathCache(workbookPath);
                    cacheService.clearWorkbookCache(workbookName);
                }
                
                log.info("成功插入 {} 行数据到工作表 {}", insertedRows, sheet.getName());
                
            } finally {
                // 关闭工作簿
                if (workbook != null) {
                    try {
                        workbook.close();
                    } catch (Exception e) {
                        log.warn("关闭工作簿异常: {}", e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("插入数据异常: {}", e.getMessage(), e);
            throw new RuntimeException("插入数据异常: " + e.getMessage(), e);
        }
        
        return insertedRows;
    }
    
    /**
     * 根据值类型设置单元格值
     *
     * @param cell 单元格
     * @param value 值
     */
    private void setCellValue(org.apache.poi.ss.usermodel.Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        
        try {
            if (value instanceof String) {
                // 字符串值
                String strValue = (String) value;
                if (strValue.length() > 32767) {
                    // Excel单元格字符串长度限制为32767
                    strValue = strValue.substring(0, 32767);
                    log.warn("字符串值被截断为32767个字符");
                }
                cell.setCellValue(strValue);
            } else if (value instanceof Number) {
                // 数值类型
                if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
                    // 整数类型
                    cell.setCellValue(((Number) value).doubleValue());
                } else if (value instanceof Float || value instanceof Double) {
                    // 浮点数类型
                    double doubleValue = ((Number) value).doubleValue();
                    if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
                        cell.setCellValue(""); // Excel不支持NaN或Infinity
                    } else {
                        cell.setCellValue(doubleValue);
                    }
                } else {
                    // 其他数值类型
                    cell.setCellValue(((Number) value).doubleValue());
                }
            } else if (value instanceof Boolean) {
                // 布尔类型
                cell.setCellValue((Boolean) value);
            } else if (value instanceof java.time.LocalDate) {
                // LocalDate类型
                cell.setCellValue(java.time.LocalDate.from((java.time.LocalDate) value).toString());
            } else if (value instanceof java.time.LocalDateTime) {
                // LocalDateTime类型
                cell.setCellValue(java.time.LocalDateTime.from((java.time.LocalDateTime) value).toString());
            } else if (value instanceof java.util.Date) {
                // Date类型
                cell.setCellValue((java.util.Date) value);
            } else if (value instanceof java.util.Calendar) {
                // Calendar类型
                cell.setCellValue((java.util.Calendar) value);
            } else {
                // 其他类型转为字符串
                String strValue = value.toString();
                if (strValue.length() > 32767) {
                    // Excel单元格字符串长度限制为32767
                    strValue = strValue.substring(0, 32767);
                    log.warn("字符串值被截断为32767个字符");
                }
                cell.setCellValue(strValue);
            }
        } catch (Exception e) {
            log.warn("设置单元格值异常: {}，使用toString()方法", e.getMessage());
            try {
                cell.setCellValue(value.toString());
            } catch (Exception ex) {
                log.error("设置单元格值失败: {}", ex.getMessage());
                cell.setCellValue(""); // 设置为空字符串作为最后的回退
            }
        }
    }
} 