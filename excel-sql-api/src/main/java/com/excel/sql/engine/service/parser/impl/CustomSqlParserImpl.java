package com.excel.sql.engine.service.parser.impl;

import com.excel.sql.engine.model.dto.SqlQueryResult;
import com.excel.sql.engine.service.parser.ParsedSql;
import com.excel.sql.engine.service.parser.SqlParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自定义SQL解析器实现
 * 用于处理特殊的Excel DDL语句，如CREATE WORKBOOK, CREATE SHEET, USE WORKBOOK等
 */
@Slf4j
@Service
@Order(1) // 优先于JSqlParser执行
public class CustomSqlParserImpl implements SqlParser {
    
    private final JSqlParserImpl jsqlParser;
    
    @Autowired
    public CustomSqlParserImpl(@Qualifier("JSqlParserImpl") JSqlParserImpl jsqlParser) {
        this.jsqlParser = jsqlParser;
    }
    
    // 正则表达式模式
    private static final Pattern CREATE_WORKBOOK_PATTERN = 
            Pattern.compile("^\\s*CREATE\\s+WORKBOOK\\s+([\\w.-]+)\\s*(?:\\((.*)\\))?\\s*;?\\s*$", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern CREATE_SHEET_PATTERN = 
            Pattern.compile("^\\s*CREATE\\s+SHEET\\s+([\\w.-]+)\\s*(?:\\((.*)\\))?\\s*;?\\s*$", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern USE_WORKBOOK_PATTERN = 
            Pattern.compile("^\\s*USE\\s+WORKBOOK\\s+([\\w.-]+)\\s*;?\\s*$", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern DROP_WORKBOOK_PATTERN = 
            Pattern.compile("^\\s*DROP\\s+WORKBOOK\\s+([\\w.-]+)\\s*;?\\s*$", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern DROP_SHEET_PATTERN = 
            Pattern.compile("^\\s*DROP\\s+SHEET\\s+([\\w.-]+)\\s*;?\\s*$", Pattern.CASE_INSENSITIVE);
    
    @Override
    public ParsedSql parse(String sql) {
        // 尝试解析自定义SQL语句
        SqlQueryResult.SqlType sqlType = getSqlType(sql);
        
        // 如果不是自定义语句，则委托给JSqlParser处理
        if (sqlType == SqlQueryResult.SqlType.UNKNOWN) {
            return jsqlParser.parse(sql);
        }
        
        try {
            ParsedSql.ParsedSqlBuilder builder = ParsedSql.builder()
                    .originalSql(sql)
                    .sqlType(sqlType)
                    .success(true);
            
            switch (sqlType) {
                case CREATE_WORKBOOK:
                    return parseCreateWorkbook(sql, builder);
                case CREATE_SHEET:
                    return parseCreateSheet(sql, builder);
                case USE_WORKBOOK:
                    return parseUseWorkbook(sql, builder);
                case DROP_WORKBOOK:
                    return parseDropWorkbook(sql, builder);
                case DROP_SHEET:
                    return parseDropSheet(sql, builder);
                default:
                    return jsqlParser.parse(sql);
            }
        } catch (Exception e) {
            log.error("自定义SQL解析异常: {}", e.getMessage(), e);
            return ParsedSql.builder()
                    .originalSql(sql)
                    .success(false)
                    .errorMessage("SQL解析异常: " + e.getMessage())
                    .build();
        }
    }
    
    @Override
    public String validate(String sql) {
        SqlQueryResult.SqlType sqlType = getSqlType(sql);
        
        if (sqlType == SqlQueryResult.SqlType.UNKNOWN) {
            return jsqlParser.validate(sql);
        }
        
        try {
            switch (sqlType) {
                case CREATE_WORKBOOK:
                    validateCreateWorkbook(sql);
                    break;
                case CREATE_SHEET:
                    validateCreateSheet(sql);
                    break;
                case USE_WORKBOOK:
                    validateUseWorkbook(sql);
                    break;
                case DROP_WORKBOOK:
                    validateDropWorkbook(sql);
                    break;
                case DROP_SHEET:
                    validateDropSheet(sql);
                    break;
                default:
                    return jsqlParser.validate(sql);
            }
            return null; // 验证成功
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    @Override
    public SqlQueryResult.SqlType getSqlType(String sql) {
        if (sql == null) {
            return SqlQueryResult.SqlType.UNKNOWN;
        }
        
        String trimmedSql = sql.trim();
        
        if (CREATE_WORKBOOK_PATTERN.matcher(trimmedSql).matches()) {
            return SqlQueryResult.SqlType.CREATE_WORKBOOK;
        } else if (CREATE_SHEET_PATTERN.matcher(trimmedSql).matches()) {
            return SqlQueryResult.SqlType.CREATE_SHEET;
        } else if (USE_WORKBOOK_PATTERN.matcher(trimmedSql).matches()) {
            return SqlQueryResult.SqlType.USE_WORKBOOK;
        } else if (DROP_WORKBOOK_PATTERN.matcher(trimmedSql).matches()) {
            return SqlQueryResult.SqlType.DROP_WORKBOOK;
        } else if (DROP_SHEET_PATTERN.matcher(trimmedSql).matches()) {
            return SqlQueryResult.SqlType.DROP_SHEET;
        }
        
        // 尝试使用JSqlParser判断类型
        return jsqlParser.getSqlType(sql);
    }
    
    /**
     * 解析CREATE WORKBOOK语句
     */
    private ParsedSql parseCreateWorkbook(String sql, ParsedSql.ParsedSqlBuilder builder) {
        Matcher matcher = CREATE_WORKBOOK_PATTERN.matcher(sql);
        if (matcher.find()) {
            String workbookName = matcher.group(1);
            String options = matcher.group(2);
            
            Map<String, Object> createOptions = parseOptions(options);
            
            return builder
                    .targetTables(List.of(workbookName))
                    .updateValues(createOptions)
                    .build();
        }
        
        return builder
                .success(false)
                .errorMessage("无效的CREATE WORKBOOK语句")
                .build();
    }
    
    /**
     * 解析CREATE SHEET语句
     */
    private ParsedSql parseCreateSheet(String sql, ParsedSql.ParsedSqlBuilder builder) {
        Matcher matcher = CREATE_SHEET_PATTERN.matcher(sql);
        if (matcher.find()) {
            String sheetName = matcher.group(1);
            String options = matcher.group(2);
            
            Map<String, Object> createOptions = parseOptions(options);
            
            return builder
                    .targetTables(List.of(sheetName))
                    .updateValues(createOptions)
                    .build();
        }
        
        return builder
                .success(false)
                .errorMessage("无效的CREATE SHEET语句")
                .build();
    }
    
    /**
     * 解析USE WORKBOOK语句
     */
    private ParsedSql parseUseWorkbook(String sql, ParsedSql.ParsedSqlBuilder builder) {
        Matcher matcher = USE_WORKBOOK_PATTERN.matcher(sql);
        if (matcher.find()) {
            String workbookName = matcher.group(1);
            
            return builder
                    .targetTables(List.of(workbookName))
                    .build();
        }
        
        return builder
                .success(false)
                .errorMessage("无效的USE WORKBOOK语句")
                .build();
    }
    
    /**
     * 解析DROP WORKBOOK语句
     */
    private ParsedSql parseDropWorkbook(String sql, ParsedSql.ParsedSqlBuilder builder) {
        Matcher matcher = DROP_WORKBOOK_PATTERN.matcher(sql);
        if (matcher.find()) {
            String workbookName = matcher.group(1);
            
            return builder
                    .targetTables(List.of(workbookName))
                    .build();
        }
        
        return builder
                .success(false)
                .errorMessage("无效的DROP WORKBOOK语句")
                .build();
    }
    
    /**
     * 解析DROP SHEET语句
     */
    private ParsedSql parseDropSheet(String sql, ParsedSql.ParsedSqlBuilder builder) {
        Matcher matcher = DROP_SHEET_PATTERN.matcher(sql);
        if (matcher.find()) {
            String sheetName = matcher.group(1);
            
            return builder
                    .targetTables(List.of(sheetName))
                    .build();
        }
        
        return builder
                .success(false)
                .errorMessage("无效的DROP SHEET语句")
                .build();
    }
    
    /**
     * 验证CREATE WORKBOOK语句
     */
    private void validateCreateWorkbook(String sql) throws Exception {
        Matcher matcher = CREATE_WORKBOOK_PATTERN.matcher(sql);
        if (!matcher.find()) {
            throw new Exception("无效的CREATE WORKBOOK语句");
        }
    }
    
    /**
     * 验证CREATE SHEET语句
     */
    private void validateCreateSheet(String sql) throws Exception {
        Matcher matcher = CREATE_SHEET_PATTERN.matcher(sql);
        if (!matcher.find()) {
            throw new Exception("无效的CREATE SHEET语句");
        }
    }
    
    /**
     * 验证USE WORKBOOK语句
     */
    private void validateUseWorkbook(String sql) throws Exception {
        Matcher matcher = USE_WORKBOOK_PATTERN.matcher(sql);
        if (!matcher.find()) {
            throw new Exception("无效的USE WORKBOOK语句");
        }
    }
    
    /**
     * 验证DROP WORKBOOK语句
     */
    private void validateDropWorkbook(String sql) throws Exception {
        Matcher matcher = DROP_WORKBOOK_PATTERN.matcher(sql);
        if (!matcher.find()) {
            throw new Exception("无效的DROP WORKBOOK语句");
        }
    }
    
    /**
     * 验证DROP SHEET语句
     */
    private void validateDropSheet(String sql) throws Exception {
        Matcher matcher = DROP_SHEET_PATTERN.matcher(sql);
        if (!matcher.find()) {
            throw new Exception("无效的DROP SHEET语句");
        }
    }
    
    /**
     * 解析选项字符串，格式为 key=value, key=value
     * 特殊处理 columns 选项，确保不会被逗号分隔
     */
    private Map<String, Object> parseOptions(String optionsStr) {
        Map<String, Object> options = new HashMap<>();
        
        if (optionsStr == null || optionsStr.trim().isEmpty()) {
            return options;
        }
        
        // 首先检查是否包含 columns= 选项
        int columnsIndex = optionsStr.indexOf("columns=");
        if (columnsIndex >= 0) {
            // 找到 columns 选项的开始位置
            int startIndex = columnsIndex + "columns=".length();
            String columnsValue;
            
            // 查找下一个选项的开始位置（以逗号开头且后面跟着字母和等号）
            int nextOptionIndex = -1;
            for (int i = startIndex; i < optionsStr.length(); i++) {
                if (optionsStr.charAt(i) == ',' && 
                    i + 1 < optionsStr.length() && 
                    Character.isLetter(optionsStr.charAt(i + 1))) {
                    
                    // 查找等号，确认这是一个新选项的开始
                    int equalsIndex = optionsStr.indexOf('=', i + 1);
                    if (equalsIndex > 0 && equalsIndex - i < 20) { // 合理的选项名长度
                        nextOptionIndex = i;
                        break;
                    }
                }
            }
            
            // 提取 columns 值
            if (nextOptionIndex > 0) {
                columnsValue = optionsStr.substring(startIndex, nextOptionIndex);
                // 处理剩余的选项
                String remainingOptions = optionsStr.substring(0, columnsIndex) + optionsStr.substring(nextOptionIndex + 1);
                parseRegularOptions(remainingOptions, options);
            } else {
                columnsValue = optionsStr.substring(startIndex);
                // 处理 columns 前面的选项
                if (columnsIndex > 0) {
                    String beforeOptions = optionsStr.substring(0, columnsIndex - 1);
                    parseRegularOptions(beforeOptions, options);
                }
            }
            
            // 移除可能的引号
            if (columnsValue.startsWith("'") && columnsValue.endsWith("'") || 
                columnsValue.startsWith("\"") && columnsValue.endsWith("\"")) {
                columnsValue = columnsValue.substring(1, columnsValue.length() - 1);
            }
            
            options.put("columns", columnsValue);
        } else {
            // 没有 columns 选项，使用常规解析
            parseRegularOptions(optionsStr, options);
        }
        
        return options;
    }
    
    /**
     * 解析常规选项（不包含复杂值的选项）
     */
    private void parseRegularOptions(String optionsStr, Map<String, Object> options) {
        if (optionsStr == null || optionsStr.trim().isEmpty()) {
            return;
        }
        
        String[] optionPairs = optionsStr.split(",");
        for (String pair : optionPairs) {
            if (pair.trim().isEmpty()) {
                continue;
            }
            
            String[] keyValue = pair.trim().split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                
                // 移除可能的引号
                if (value.startsWith("'") && value.endsWith("'") || 
                    value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                
                options.put(key, value);
            }
        }
    }
} 