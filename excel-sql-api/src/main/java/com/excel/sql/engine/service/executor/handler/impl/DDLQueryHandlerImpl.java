package com.excel.sql.engine.service.executor.handler.impl;

import com.excel.sql.engine.model.dto.SqlQueryResult;
import com.excel.sql.engine.model.excel.ExcelWorkbook;
import com.excel.sql.engine.service.WorkbookManager;
import com.excel.sql.engine.service.executor.handler.QueryHandler;
import com.excel.sql.engine.service.parser.ParsedSql;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * DDL查询处理器实现类
 * 处理CREATE WORKBOOK, CREATE SHEET, USE WORKBOOK等DDL操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DDLQueryHandlerImpl implements QueryHandler {
    
    private final WorkbookManager workbookManager;
    
    @Override
    public SqlQueryResult handle(ParsedSql parsedSql, String workbook) {
        long startTime = System.currentTimeMillis();
        
        try {
            SqlQueryResult.SqlType sqlType = parsedSql.getSqlType();
            
            switch (sqlType) {
                case CREATE_WORKBOOK:
                    return handleCreateWorkbook(parsedSql, startTime);
                case CREATE_SHEET:
                    return handleCreateSheet(parsedSql, startTime);
                case USE_WORKBOOK:
                    return handleUseWorkbook(parsedSql, startTime);
                case DROP_WORKBOOK:
                    return handleDropWorkbook(parsedSql, startTime);
                case DROP_SHEET:
                    return handleDropSheet(parsedSql, startTime);
                default:
                    return SqlQueryResult.error("不支持的DDL操作: " + sqlType);
            }
        } catch (Exception e) {
            log.error("执行DDL操作异常: {}", e.getMessage(), e);
            return SqlQueryResult.error("执行DDL操作异常: " + e.getMessage());
        }
    }
    
    /**
     * 处理CREATE WORKBOOK语句
     */
    private SqlQueryResult handleCreateWorkbook(ParsedSql parsedSql, long startTime) {
        try {
            if (parsedSql.getTargetTables() == null || parsedSql.getTargetTables().isEmpty()) {
                return SqlQueryResult.error("未指定工作簿名称");
            }
            
            String workbookName = parsedSql.getTargetTables().get(0);
            Map<String, Object> options = parsedSql.getUpdateValues();
            
            ExcelWorkbook workbook = workbookManager.createWorkbook(workbookName, options);
            
            Map<String, Object> resultRow = new HashMap<>();
            resultRow.put("workbookName", workbook.getName());
            resultRow.put("message", "工作簿创建成功");
            
            long executionTime = System.currentTimeMillis() - startTime;
            return SqlQueryResult.successDml(1, SqlQueryResult.SqlType.CREATE_WORKBOOK, executionTime);
        } catch (Exception e) {
            log.error("创建工作簿失败: {}", e.getMessage(), e);
            return SqlQueryResult.error("创建工作簿失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理CREATE SHEET语句
     */
    private SqlQueryResult handleCreateSheet(ParsedSql parsedSql, long startTime) {
        try {
            if (parsedSql.getTargetTables() == null || parsedSql.getTargetTables().isEmpty()) {
                return SqlQueryResult.error("未指定工作表名称");
            }
            
            String sheetName = parsedSql.getTargetTables().get(0);
            Map<String, Object> options = parsedSql.getUpdateValues();
            
            boolean success = workbookManager.createSheet(sheetName, options);
            
            if (success) {
                long executionTime = System.currentTimeMillis() - startTime;
                return SqlQueryResult.successDml(1, SqlQueryResult.SqlType.CREATE_SHEET, executionTime);
            } else {
                return SqlQueryResult.error("创建工作表失败");
            }
        } catch (Exception e) {
            log.error("创建工作表失败: {}", e.getMessage(), e);
            return SqlQueryResult.error("创建工作表失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理USE WORKBOOK语句
     */
    private SqlQueryResult handleUseWorkbook(ParsedSql parsedSql, long startTime) {
        try {
            if (parsedSql.getTargetTables() == null || parsedSql.getTargetTables().isEmpty()) {
                return SqlQueryResult.error("未指定工作簿名称");
            }
            
            String workbookName = parsedSql.getTargetTables().get(0);
            
            boolean success = workbookManager.useWorkbook(workbookName);
            
            if (success) {
                long executionTime = System.currentTimeMillis() - startTime;
                return SqlQueryResult.successDml(1, SqlQueryResult.SqlType.USE_WORKBOOK, executionTime);
            } else {
                return SqlQueryResult.error("切换工作簿失败");
            }
        } catch (Exception e) {
            log.error("切换工作簿失败: {}", e.getMessage(), e);
            return SqlQueryResult.error("切换工作簿失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理DROP WORKBOOK语句
     */
    private SqlQueryResult handleDropWorkbook(ParsedSql parsedSql, long startTime) {
        try {
            if (parsedSql.getTargetTables() == null || parsedSql.getTargetTables().isEmpty()) {
                return SqlQueryResult.error("未指定工作簿名称");
            }
            
            String workbookName = parsedSql.getTargetTables().get(0);
            
            boolean success = workbookManager.dropWorkbook(workbookName);
            
            if (success) {
                long executionTime = System.currentTimeMillis() - startTime;
                return SqlQueryResult.successDml(1, SqlQueryResult.SqlType.DROP_WORKBOOK, executionTime);
            } else {
                return SqlQueryResult.error("删除工作簿失败");
            }
        } catch (Exception e) {
            log.error("删除工作簿失败: {}", e.getMessage(), e);
            return SqlQueryResult.error("删除工作簿失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理DROP SHEET语句
     */
    private SqlQueryResult handleDropSheet(ParsedSql parsedSql, long startTime) {
        try {
            if (parsedSql.getTargetTables() == null || parsedSql.getTargetTables().isEmpty()) {
                return SqlQueryResult.error("未指定工作表名称");
            }
            
            String sheetName = parsedSql.getTargetTables().get(0);
            
            boolean success = workbookManager.dropSheet(sheetName);
            
            if (success) {
                long executionTime = System.currentTimeMillis() - startTime;
                return SqlQueryResult.successDml(1, SqlQueryResult.SqlType.DROP_SHEET, executionTime);
            } else {
                return SqlQueryResult.error("删除工作表失败");
            }
        } catch (Exception e) {
            log.error("删除工作表失败: {}", e.getMessage(), e);
            return SqlQueryResult.error("删除工作表失败: " + e.getMessage());
        }
    }
} 