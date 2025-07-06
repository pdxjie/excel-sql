package com.excel.sql.engine.service.executor.handler.impl;

import com.excel.sql.engine.model.dto.SqlQueryResult;
import com.excel.sql.engine.model.excel.ExcelColumn;
import com.excel.sql.engine.model.excel.ExcelRow;
import com.excel.sql.engine.model.excel.ExcelSheet;
import com.excel.sql.engine.model.excel.ExcelWorkbook;
import com.excel.sql.engine.service.CacheService;
import com.excel.sql.engine.service.ExcelFileService;
import com.excel.sql.engine.service.executor.handler.DeleteQueryHandler;
import com.excel.sql.engine.service.parser.ParsedSql;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * DELETE查询处理器实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteQueryHandlerImpl implements DeleteQueryHandler {
    
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
                return SqlQueryResult.error("未指定删除表");
            }
            
            String sheetName = parsedSql.getTargetTables().get(0);
            
            // 检查工作表是否存在
            if (!excelFileService.existsSheet(workbook, sheetName)) {
                return SqlQueryResult.error("工作表不存在: " + sheetName);
            }
            
            // 获取工作表
            ExcelSheet sheet = excelFileService.getSheet(workbook, sheetName);
            
            // 执行删除
            int affectedRows = deleteRows(sheet, parsedSql, workbook);
            
            // 创建查询结果
            long executionTime = System.currentTimeMillis() - startTime;
            return SqlQueryResult.successDml(affectedRows, SqlQueryResult.SqlType.DELETE, executionTime);
            
        } catch (Exception e) {
            log.error("执行DELETE查询异常: {}", e.getMessage(), e);
            return SqlQueryResult.error("执行DELETE查询异常: " + e.getMessage());
        }
    }
    
    /**
     * 删除行数据
     *
     * @param sheet 工作表
     * @param parsedSql 解析后的SQL
     * @param workbookName 工作簿名称
     * @return 影响的行数
     */
    private int deleteRows(ExcelSheet sheet, ParsedSql parsedSql, String workbookName) {
        // 首先确保行数据已加载
        if (sheet.getRows() == null || sheet.getRows().isEmpty()) {
            try {
                // 通过工作簿名称获取路径
                ExcelWorkbook excelWorkbook = excelFileService.loadWorkbook(workbookName);
                if (excelWorkbook != null) {
                    String workbookPath = excelWorkbook.getFilePath();
                    if (workbookPath != null) {
                        // 加载行数据
                        boolean loaded = sheet.loadRows(workbookPath);
                        if (!loaded) {
                            log.error("无法加载工作表数据");
                            return 0;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("加载工作表数据异常: {}", e.getMessage(), e);
                return 0;
            }
        }
        
        List<ExcelRow> rows = sheet.getRows();
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        
        int deletedRows = 0;
        List<ExcelRow> rowsToDelete = new ArrayList<>();
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
            
            // 找出要删除的行
            for (ExcelRow row : rows) {
                try {
                    // 检查WHERE条件
                    if (parsedSql.getWhereCondition() != null && !parsedSql.getWhereCondition().isEmpty()) {
                        if (evaluateCondition(row, parsedSql.getWhereCondition())) {
                            rowsToDelete.add(row);
                        }
                    } else {
                        // 如果没有WHERE条件，删除所有行
                        rowsToDelete.add(row);
                    }
                } catch (Exception e) {
                    log.error("评估删除条件异常: {}", e.getMessage(), e);
                }
            }
            
            // 如果没有行需要删除，直接返回
            if (rowsToDelete.isEmpty()) {
                log.info("没有符合条件的行需要删除");
                return 0;
            }
            
            log.info("找到 {} 行符合删除条件", rowsToDelete.size());
            
            // 删除内存中的行
            for (ExcelRow row : rowsToDelete) {
                try {
                    // 从主索引中删除
                    if (sheet.getPrimaryIndex() != null) {
                        sheet.getPrimaryIndex().remove(row.getRowNum());
                    }
                    
                    // 从行列表中删除
                    rows.remove(row);
                    
                    deletedRows++;
                    log.debug("从内存中删除行: {}", row);
                    
                } catch (Exception e) {
                    log.error("删除行异常: {}", e.getMessage(), e);
                }
            }
            
            // 然后将更改写入Excel文件
            org.apache.poi.ss.usermodel.Workbook workbook = null;
            
            try {
                // 打开工作簿
                try (FileInputStream fis = new FileInputStream(file)) {
                    workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(fis);
                }
                
                // 获取工作表
                org.apache.poi.ss.usermodel.Sheet poiSheet = workbook.getSheet(sheet.getName());
                if (poiSheet == null) {
                    log.error("工作表不存在: {}", sheet.getName());
                    throw new IllegalStateException("工作表不存在: " + sheet.getName());
                }
                
                // 删除行
                for (ExcelRow row : rowsToDelete) {
                    int rowNum = row.getRowNum();
                    org.apache.poi.ss.usermodel.Row poiRow = poiSheet.getRow(rowNum);
                    if (poiRow != null) {
                        poiSheet.removeRow(poiRow);
                        log.debug("从Excel文件中删除行: {}", rowNum);
                    }
                }
                
                // 如果删除了所有行，确保保留表头行
                if (poiSheet.getLastRowNum() == 0) {
                    log.info("删除了所有数据行，保留表头行");
                }
                
                // 保存工作簿
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    workbook.write(fos);
                    fos.flush(); // 确保数据写入磁盘
                    log.info("成功将数据写入文件: {}", file.getAbsolutePath());
                    
                    // 清除相关缓存
                    cacheService.clearFilePathCache(workbookPath);
                    cacheService.clearWorkbookCache(workbookName);
                }
                
                log.info("成功删除 {} 行数据", deletedRows);
                
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
            log.error("删除数据异常: {}", e.getMessage(), e);
            throw new RuntimeException("删除数据异常: " + e.getMessage(), e);
        }
        
        return deletedRows;
    }
    
    /**
     * 条件评估
     */
    private boolean evaluateCondition(ExcelRow row, String condition) {
        log.debug("评估条件: {}", condition);
        
        // 处理多个条件（AND连接）
        if (condition.contains(" AND ")) {
            String[] conditions = condition.split(" AND ");
            for (String subCondition : conditions) {
                if (!evaluateSimpleCondition(row, subCondition.trim())) {
                    return false;
                }
            }
            return true;
        }
        
        // 处理多个条件（OR连接）
        if (condition.contains(" OR ")) {
            String[] conditions = condition.split(" OR ");
            for (String subCondition : conditions) {
                if (evaluateSimpleCondition(row, subCondition.trim())) {
                    return true;
                }
            }
            return false;
        }
        
        // 处理单个条件
        return evaluateSimpleCondition(row, condition);
    }
    
    /**
     * 评估简单条件
     */
    private boolean evaluateSimpleCondition(ExcelRow row, String condition) {
        log.debug("评估简单条件: {}", condition);
        
        // 处理等于条件 - 支持引号和无引号的值
        if (condition.contains("=") && !condition.contains("!=") && !condition.contains("<>") && !condition.contains(">=") && !condition.contains("<=")) {
            String[] parts = condition.split("=");
            if (parts.length == 2) {
                String columnName = parts[0].trim();
                String expectedValue = parts[1].trim();
                
                // 移除引号
                if ((expectedValue.startsWith("'") && expectedValue.endsWith("'")) || 
                    (expectedValue.startsWith("\"") && expectedValue.endsWith("\""))) {
                    expectedValue = expectedValue.substring(1, expectedValue.length() - 1);
                }
                
                Object actualValue = row.getCellValue(columnName);
                log.debug("比较: 列={}, 期望值={}, 实际值={}", columnName, expectedValue, actualValue);
                
                if (actualValue == null) {
                    return expectedValue.equalsIgnoreCase("null") || expectedValue.isEmpty();
                }
                
                // 尝试数值比较
                if (actualValue instanceof Number && isNumeric(expectedValue)) {
                    double actualNum = ((Number) actualValue).doubleValue();
                    double expectedNum = Double.parseDouble(expectedValue);
                    return actualNum == expectedNum;
                }
                
                // 字符串比较
                return actualValue.toString().equals(expectedValue);
            }
        }
        
        // 处理不等于条件
        if (condition.contains("!=") || condition.contains("<>")) {
            String[] parts;
            if (condition.contains("!=")) {
                parts = condition.split("!=");
            } else {
                parts = condition.split("<>");
            }
            
            if (parts.length == 2) {
                String columnName = parts[0].trim();
                String expectedValue = parts[1].trim();
                
                // 移除引号
                if ((expectedValue.startsWith("'") && expectedValue.endsWith("'")) || 
                    (expectedValue.startsWith("\"") && expectedValue.endsWith("\""))) {
                    expectedValue = expectedValue.substring(1, expectedValue.length() - 1);
                }
                
                Object actualValue = row.getCellValue(columnName);
                log.debug("比较不等: 列={}, 期望值={}, 实际值={}", columnName, expectedValue, actualValue);
                
                if (actualValue == null) {
                    return !expectedValue.equalsIgnoreCase("null") && !expectedValue.isEmpty();
                }
                
                // 尝试数值比较
                if (actualValue instanceof Number && isNumeric(expectedValue)) {
                    double actualNum = ((Number) actualValue).doubleValue();
                    double expectedNum = Double.parseDouble(expectedValue);
                    return actualNum != expectedNum;
                }
                
                // 字符串比较
                return !actualValue.toString().equals(expectedValue);
            }
        }
        
        // 处理大于条件
        if (condition.contains(">") && !condition.contains(">=")) {
            String[] parts = condition.split(">");
            if (parts.length == 2) {
                String columnName = parts[0].trim();
                String expectedValue = parts[1].trim();
                
                // 移除引号
                if ((expectedValue.startsWith("'") && expectedValue.endsWith("'")) || 
                    (expectedValue.startsWith("\"") && expectedValue.endsWith("\""))) {
                    expectedValue = expectedValue.substring(1, expectedValue.length() - 1);
                }
                
                Object actualValue = row.getCellValue(columnName);
                log.debug("比较大于: 列={}, 期望值={}, 实际值={}", columnName, expectedValue, actualValue);
                
                if (actualValue == null) {
                    return false;
                }
                
                // 尝试数值比较
                if (isNumeric(expectedValue)) {
                    double expectedNum = Double.parseDouble(expectedValue);
                    
                    if (actualValue instanceof Number) {
                        double actualNum = ((Number) actualValue).doubleValue();
                        return actualNum > expectedNum;
                    } else if (isNumeric(actualValue.toString())) {
                        double actualNum = Double.parseDouble(actualValue.toString());
                        return actualNum > expectedNum;
                    }
                }
                
                // 字符串比较
                return actualValue.toString().compareTo(expectedValue) > 0;
            }
        }
        
        // 处理大于等于条件
        if (condition.contains(">=")) {
            String[] parts = condition.split(">=");
            if (parts.length == 2) {
                String columnName = parts[0].trim();
                String expectedValue = parts[1].trim();
                
                // 移除引号
                if ((expectedValue.startsWith("'") && expectedValue.endsWith("'")) || 
                    (expectedValue.startsWith("\"") && expectedValue.endsWith("\""))) {
                    expectedValue = expectedValue.substring(1, expectedValue.length() - 1);
                }
                
                Object actualValue = row.getCellValue(columnName);
                log.debug("比较大于等于: 列={}, 期望值={}, 实际值={}", columnName, expectedValue, actualValue);
                
                if (actualValue == null) {
                    return false;
                }
                
                // 尝试数值比较
                if (isNumeric(expectedValue)) {
                    double expectedNum = Double.parseDouble(expectedValue);
                    
                    if (actualValue instanceof Number) {
                        double actualNum = ((Number) actualValue).doubleValue();
                        return actualNum >= expectedNum;
                    } else if (isNumeric(actualValue.toString())) {
                        double actualNum = Double.parseDouble(actualValue.toString());
                        return actualNum >= expectedNum;
                    }
                }
                
                // 字符串比较
                return actualValue.toString().compareTo(expectedValue) >= 0;
            }
        }
        
        // 处理小于条件
        if (condition.contains("<") && !condition.contains("<=")) {
            String[] parts = condition.split("<");
            if (parts.length == 2) {
                String columnName = parts[0].trim();
                String expectedValue = parts[1].trim();
                
                // 移除引号
                if ((expectedValue.startsWith("'") && expectedValue.endsWith("'")) || 
                    (expectedValue.startsWith("\"") && expectedValue.endsWith("\""))) {
                    expectedValue = expectedValue.substring(1, expectedValue.length() - 1);
                }
                
                Object actualValue = row.getCellValue(columnName);
                log.debug("比较小于: 列={}, 期望值={}, 实际值={}", columnName, expectedValue, actualValue);
                
                if (actualValue == null) {
                    return false;
                }
                
                // 尝试数值比较
                if (isNumeric(expectedValue)) {
                    double expectedNum = Double.parseDouble(expectedValue);
                    
                    if (actualValue instanceof Number) {
                        double actualNum = ((Number) actualValue).doubleValue();
                        return actualNum < expectedNum;
                    } else if (isNumeric(actualValue.toString())) {
                        double actualNum = Double.parseDouble(actualValue.toString());
                        return actualNum < expectedNum;
                    }
                }
                
                // 字符串比较
                return actualValue.toString().compareTo(expectedValue) < 0;
            }
        }
        
        // 处理小于等于条件
        if (condition.contains("<=")) {
            String[] parts = condition.split("<=");
            if (parts.length == 2) {
                String columnName = parts[0].trim();
                String expectedValue = parts[1].trim();
                
                // 移除引号
                if ((expectedValue.startsWith("'") && expectedValue.endsWith("'")) || 
                    (expectedValue.startsWith("\"") && expectedValue.endsWith("\""))) {
                    expectedValue = expectedValue.substring(1, expectedValue.length() - 1);
                }
                
                Object actualValue = row.getCellValue(columnName);
                log.debug("比较小于等于: 列={}, 期望值={}, 实际值={}", columnName, expectedValue, actualValue);
                
                if (actualValue == null) {
                    return false;
                }
                
                // 尝试数值比较
                if (isNumeric(expectedValue)) {
                    double expectedNum = Double.parseDouble(expectedValue);
                    
                    if (actualValue instanceof Number) {
                        double actualNum = ((Number) actualValue).doubleValue();
                        return actualNum <= expectedNum;
                    } else if (isNumeric(actualValue.toString())) {
                        double actualNum = Double.parseDouble(actualValue.toString());
                        return actualNum <= expectedNum;
                    }
                }
                
                // 字符串比较
                return actualValue.toString().compareTo(expectedValue) <= 0;
            }
        }
        
        // 处理LIKE条件
        if (condition.toLowerCase().contains(" like ")) {
            String[] parts = condition.toLowerCase().split(" like ");
            if (parts.length == 2) {
                String columnName = parts[0].trim();
                String pattern = parts[1].trim();
                
                // 移除引号
                if ((pattern.startsWith("'") && pattern.endsWith("'")) || 
                    (pattern.startsWith("\"") && pattern.endsWith("\""))) {
                    pattern = pattern.substring(1, pattern.length() - 1);
                }
                
                Object actualValue = row.getCellValue(columnName);
                log.debug("比较LIKE: 列={}, 模式={}, 实际值={}", columnName, pattern, actualValue);
                
                if (actualValue == null) {
                    return false;
                }
                
                String actualStr = actualValue.toString();
                // 将SQL LIKE模式转换为Java正则表达式
                String regex = pattern.replace("%", ".*").replace("_", ".");
                
                return actualStr.matches(regex);
            }
        }
        
        // 默认通过所有行
        log.debug("无法识别的条件，默认为true: {}", condition);
        return true;
    }
    
    /**
     * 检查字符串是否为数字
     */
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
} 