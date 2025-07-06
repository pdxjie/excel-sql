package com.excel.sql.engine.service.executor.handler.impl;

import com.excel.sql.engine.model.dto.SqlQueryResult;
import com.excel.sql.engine.model.excel.ExcelColumn;
import com.excel.sql.engine.model.excel.ExcelRow;
import com.excel.sql.engine.model.excel.ExcelSheet;
import com.excel.sql.engine.model.excel.ExcelWorkbook;
import com.excel.sql.engine.service.CacheService;
import com.excel.sql.engine.service.ExcelFileService;
import com.excel.sql.engine.service.executor.handler.UpdateQueryHandler;
import com.excel.sql.engine.service.parser.ParsedSql;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UPDATE查询处理器实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateQueryHandlerImpl implements UpdateQueryHandler {
    
    private final ExcelFileService excelFileService;
    private final CacheService cacheService;
    
    // 表达式模式：列名 运算符 值
    // 更灵活的模式，支持更多格式
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\s*([a-zA-Z0-9_]+)\\s*([+\\-*/])\\s*([0-9.]+)\\s*");
    
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
                return SqlQueryResult.error("未指定更新表");
            }
            
            String sheetName = parsedSql.getTargetTables().get(0);
            
            // 检查工作表是否存在
            if (!excelFileService.existsSheet(workbook, sheetName)) {
                return SqlQueryResult.error("工作表不存在: " + sheetName);
            }
            
            // 获取工作表
            ExcelSheet sheet = excelFileService.getSheet(workbook, sheetName);
            
            // 检查更新值
            if (parsedSql.getUpdateValues() == null || parsedSql.getUpdateValues().isEmpty()) {
                return SqlQueryResult.error("未指定更新值");
            }
            
            // 执行更新
            int affectedRows = updateRows(sheet, parsedSql, workbook);
            
            // 创建查询结果
            long executionTime = System.currentTimeMillis() - startTime;
            return SqlQueryResult.successDml(affectedRows, SqlQueryResult.SqlType.UPDATE, executionTime);
            
        } catch (Exception e) {
            log.error("执行UPDATE查询异常: {}", e.getMessage(), e);
            return SqlQueryResult.error("执行UPDATE查询异常: " + e.getMessage());
        }
    }
    
    /**
     * 更新行数据
     *
     * @param sheet 工作表
     * @param parsedSql 解析后的SQL
     * @param workbookName 工作簿名称
     * @return 影响的行数
     */
    private int updateRows(ExcelSheet sheet, ParsedSql parsedSql, String workbookName) {
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
        
        int updatedRows = 0;
        Map<String, Object> updateValues = parsedSql.getUpdateValues();
        String workbookPath = null;
        
        // 记录要更新的字段数量
        log.info("准备更新 {} 个字段: {}", updateValues.size(), updateValues.keySet());
        
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
            
            // 验证所有列是否都存在于工作表中
            for (String columnName : updateValues.keySet()) {
                if (Objects.isNull(sheet.getColumn(columnName))) {
                    log.error("列 '{}' 在工作表 '{}' 中不存在", columnName, sheet.getName());
                    throw new IllegalArgumentException("列 '" + columnName + "' 在工作表 '" + sheet.getName() + "' 中不存在");
                }
            }
            
            // 先在内存中应用更新
            for (ExcelRow row : rows) {
                try {
                    // 检查WHERE条件
                    if (parsedSql.getWhereCondition() != null && !parsedSql.getWhereCondition().isEmpty()) {
                        if (!evaluateCondition(row, parsedSql.getWhereCondition())) {
                            continue;
                        }
                    }
                    
                    // 更新行数据 - 支持多个字段同时更新
                    boolean rowUpdated = false;
                    for (Map.Entry<String, Object> entry : updateValues.entrySet()) {
                        String columnName = entry.getKey();
                        Object value = entry.getValue();
                        
                        log.debug("处理更新值: 列={}, 原始值类型={}, 原始值={}", 
                                columnName, 
                                (value != null ? value.getClass().getName() : "null"), 
                                value);
                        
                        // 处理表达式计算
                        Object finalValue = value;
                        if (value instanceof String) {
                            String strValue = (String) value;
                            finalValue = evaluateExpression(row, columnName, strValue);
                            log.debug("表达式计算结果: {} -> {} (类型: {})", 
                                    strValue, 
                                    finalValue, 
                                    (finalValue != null ? finalValue.getClass().getName() : "null"));
                        }
                        
                        // 获取旧值用于日志记录
                        Object oldValue = row.getCellValue(columnName);
                        
                        // 更新内存中的模型
                        row.setCellValue(columnName, finalValue);
                        rowUpdated = true;
                        
                        log.debug("更新字段: {} 从 [{}] 到 [{}] (类型: {})", 
                                columnName, 
                                oldValue, 
                                finalValue, 
                                (finalValue != null ? finalValue.getClass().getName() : "null"));
                    }
                    
                    if (rowUpdated) {
                        updatedRows++;
                        log.debug("更新行: {}", row);
                        
                        // 检查行对象是否包含所有更新的字段
                        log.info("行对象更新后的字段:");
                        for (String columnName : updateValues.keySet()) {
                            Object value = row.getCellValue(columnName);
                            log.info("  - 列: {}, 值: {}, 类型: {}", 
                                    columnName, 
                                    value, 
                                    (value != null ? value.getClass().getName() : "null"));
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("更新行异常: {}", e.getMessage(), e);
                }
            }
            
            // 然后将更新写入Excel文件
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
                
                // 将内存中的更新应用到POI工作表
                for (ExcelRow row : rows) {
                    // 只处理已更新的行
                    if (parsedSql.getWhereCondition() != null && !parsedSql.getWhereCondition().isEmpty()) {
                        if (!evaluateCondition(row, parsedSql.getWhereCondition())) {
                            continue;
                        }
                    }
                    
                    // 获取对应的POI行
                    org.apache.poi.ss.usermodel.Row poiRow = poiSheet.getRow(row.getRowNum());
                    if (poiRow == null) {
                        log.warn("行 {} 在Excel文件中不存在，创建新行", row.getRowNum());
                        poiRow = poiSheet.createRow(row.getRowNum());
                    }
                    
                    log.info("将内存中的更新应用到POI工作表，行号: {}, 更新字段数量: {}", row.getRowNum(), updateValues.size());
                    
                    // 更新行数据 - 确保遍历所有需要更新的字段
                    for (String columnName : updateValues.keySet()) {
                        // 获取列定义
                        ExcelColumn column = sheet.getColumn(columnName);
                        if (column == null) {
                            log.error("无法获取列定义: {}", columnName);
                            continue;
                        }
                        
                        int columnIndex = column.getIndex();
                        log.debug("更新单元格: 行={}, 列={}, 索引={}", row.getRowNum(), columnName, columnIndex);
                        
                        // 创建或获取单元格
                        org.apache.poi.ss.usermodel.Cell cell = poiRow.getCell(columnIndex);
                        if (cell == null) {
                            cell = poiRow.createCell(columnIndex);
                            log.debug("创建新单元格: 行={}, 列={}", row.getRowNum(), columnName);
                        }
                        
                        // 从内存模型中获取已计算的值
                        Object value = row.getCellValue(columnName);
                        log.debug("单元格值: 列={}, 值={}, 类型={}", 
                                columnName, 
                                value, 
                                (value != null ? value.getClass().getName() : "null"));
                        
                        // 设置单元格值
                        setCellValue(cell, value);
                    }
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
                
                log.info("成功更新 {} 行数据", updatedRows);
                
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
            log.error("更新数据异常: {}", e.getMessage(), e);
            throw new RuntimeException("更新数据异常: " + e.getMessage(), e);
        }
        
        return updatedRows;
    }
    
    /**
     * 计算表达式值
     * 支持形如 "column + 5", "column - 10" 等表达式
     */
    private Object evaluateExpression(ExcelRow row, String targetColumn, String expression) {
        log.debug("计算表达式: {} 对于列 {}", expression, targetColumn);
        
        // 检查是否是字符串字面量（被单引号或双引号包围）
        if ((expression.startsWith("'") && expression.endsWith("'")) || 
            (expression.startsWith("\"") && expression.endsWith("\""))) {
            // 移除引号，返回纯字符串值
            String stringValue = expression.substring(1, expression.length() - 1);
            log.debug("检测到字符串字面量，返回值: {}", stringValue);
            return stringValue;
        }
        
        // 检查是否是算术表达式
        Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            log.debug("不是表达式模式，直接返回原始值: {}", expression);
            return expression; // 不是表达式，直接返回原始值
        }
        
        try {
            String columnName = matcher.group(1);
            String operator = matcher.group(2);
            double value = Double.parseDouble(matcher.group(3));
            
            log.debug("解析表达式: 列名={}, 运算符={}, 值={}", columnName, operator, value);
            
            // 如果表达式中的列名与目标列名相同，则进行计算
            if (columnName.equals(targetColumn)) {
                // 获取当前列的值
                Object currentValue = row.getCellValue(columnName);
                log.debug("当前列 {} 的值: {}", columnName, currentValue);
                
                if (currentValue == null) {
                    log.debug("当前值为空，返回表达式值: {}", value);
                    return value; // 如果当前值为空，则直接返回表达式中的值
                }
                
                // 转换为数值
                double currentNumber;
                if (currentValue instanceof Number) {
                    currentNumber = ((Number) currentValue).doubleValue();
                } else {
                    try {
                        currentNumber = Double.parseDouble(currentValue.toString());
                    } catch (NumberFormatException e) {
                        log.warn("列 {} 的值 {} 不是有效的数字，无法进行计算", columnName, currentValue);
                        return expression; // 无法转换为数字，返回原始表达式
                    }
                }
                
                log.debug("当前数值: {}", currentNumber);
                
                // 根据运算符进行计算
                double result;
                switch (operator) {
                    case "+":
                        result = currentNumber + value;
                        break;
                    case "-":
                        result = currentNumber - value;
                        break;
                    case "*":
                        result = currentNumber * value;
                        break;
                    case "/":
                        if (value == 0) {
                            log.warn("除数为零，无法进行除法运算");
                            return currentNumber;
                        }
                        result = currentNumber / value;
                        break;
                    default:
                        log.warn("不支持的运算符: {}", operator);
                        return expression; // 不支持的运算符，返回原始表达式
                }
                
                log.debug("计算结果: {} {} {} = {}", currentNumber, operator, value, result);
                
                // 如果结果是整数，返回整数类型
                if (result == Math.floor(result) && !Double.isInfinite(result)) {
                    return (int) result;
                }
                
                return result;
            } else {
                log.debug("表达式列名 {} 与目标列名 {} 不匹配", columnName, targetColumn);
            }
        } catch (Exception e) {
            log.warn("计算表达式 {} 异常: {}", expression, e.getMessage());
        }
        
        return expression; // 如果出现任何异常，返回原始表达式
    }
    
    /**
     * 简单条件评估
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