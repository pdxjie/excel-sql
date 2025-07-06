package com.excel.sql.engine.service.executor.impl;

import com.excel.sql.engine.model.dto.SqlQueryRequest;
import com.excel.sql.engine.model.dto.SqlQueryResult;
import com.excel.sql.engine.model.excel.*;
import com.excel.sql.engine.service.WorkbookManager;
import com.excel.sql.engine.service.cache.QueryCacheService;
import com.excel.sql.engine.service.executor.SqlExecutor;
import com.excel.sql.engine.service.executor.handler.*;
import com.excel.sql.engine.service.executor.handler.impl.DDLQueryHandlerImpl;
import com.excel.sql.engine.service.parser.ParsedSql;
import com.excel.sql.engine.service.parser.SqlParser;
import com.excel.sql.engine.service.parser.impl.CustomSqlParserImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

/**
 * SQL执行器实现类
 */
@Slf4j
@Service
public class SqlExecutorImpl implements SqlExecutor {
    
    private final SqlParser sqlParser;
    private final QueryCacheService queryCacheService;
    private final SelectQueryHandler selectQueryHandler;
    private final InsertQueryHandler insertQueryHandler;
    private final UpdateQueryHandler updateQueryHandler;
    private final DeleteQueryHandler deleteQueryHandler;
    private final DDLQueryHandlerImpl ddlQueryHandler;
    private final WorkbookManager workbookManager;
    
    /**
     * 构造函数，使用 @Qualifier 注解指定要注入的 SqlParser 实现
     */
    public SqlExecutorImpl(
            @Qualifier("customSqlParserImpl") SqlParser sqlParser,
            QueryCacheService queryCacheService,
            SelectQueryHandler selectQueryHandler,
            InsertQueryHandler insertQueryHandler,
            UpdateQueryHandler updateQueryHandler,
            DeleteQueryHandler deleteQueryHandler,
            DDLQueryHandlerImpl ddlQueryHandler,
            WorkbookManager workbookManager) {
        this.sqlParser = sqlParser;
        this.queryCacheService = queryCacheService;
        this.selectQueryHandler = selectQueryHandler;
        this.insertQueryHandler = insertQueryHandler;
        this.updateQueryHandler = updateQueryHandler;
        this.deleteQueryHandler = deleteQueryHandler;
        this.ddlQueryHandler = ddlQueryHandler;
        this.workbookManager = workbookManager;
    }
    
    /**
     * 查询执行线程池
     */
    private final ExecutorService queryExecutor = new ThreadPoolExecutor(
            4, 8, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );
    
    @Override
    public SqlQueryResult execute(SqlQueryRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 解析SQL语句
            ParsedSql parsedSql = sqlParser.parse(request.getSql());
            
            if (!parsedSql.isSuccess()) {
                return SqlQueryResult.error(parsedSql.getErrorMessage());
            }
            
            // 如果没有指定工作簿，则使用当前工作簿
            String workbook = request.getWorkbook();
            if (workbook == null && !isDDLOperation(parsedSql.getSqlType())) {
                workbook = workbookManager.getCurrentWorkbookName();
                if (workbook == null) {
                    return SqlQueryResult.error("未指定工作簿，请使用USE WORKBOOK命令选择工作簿");
                }
            }
            
            // 执行查询
            return execute(parsedSql, workbook, request.getUseCache(), request.getMaxRows());
        } catch (Exception e) {
            log.error("执行SQL查询异常: {}", e.getMessage(), e);
            return SqlQueryResult.error("执行SQL查询异常: " + e.getMessage());
        } finally {
            log.debug("SQL查询执行时间: {}ms", System.currentTimeMillis() - startTime);
        }
    }
    
    @Override
    public SqlQueryResult execute(ParsedSql parsedSql, String workbook, boolean useCache, int maxRows) {
        long startTime = System.currentTimeMillis();
        
        // 检查是否可以使用缓存
        if (useCache && parsedSql.getSqlType() == SqlQueryResult.SqlType.SELECT) {
            String cacheKey = generateCacheKey(parsedSql, workbook);
            SqlQueryResult cachedResult = queryCacheService.getFromCache(cacheKey);
            
            if (cachedResult != null) {
                log.debug("使用缓存结果: {}", cacheKey);
                return cachedResult;
            }
        }
        
        // 执行查询
        SqlQueryResult result;
        try {
            switch (parsedSql.getSqlType()) {
                case SELECT:
                    result = selectQueryHandler.handle(parsedSql, workbook, maxRows);
                    break;
                case INSERT:
                    result = insertQueryHandler.handle(parsedSql, workbook);
                    break;
                case UPDATE:
                    result = updateQueryHandler.handle(parsedSql, workbook);
                    break;
                case DELETE:
                    result = deleteQueryHandler.handle(parsedSql, workbook);
                    break;
                case CREATE_WORKBOOK:
                case CREATE_SHEET:
                case USE_WORKBOOK:
                case DROP_WORKBOOK:
                case DROP_SHEET:
                    result = ddlQueryHandler.handle(parsedSql, workbook);
                    break;
                default:
                    return SqlQueryResult.error("不支持的SQL语句类型: " + parsedSql.getSqlType());
            }
            
            // 设置执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            if (result.getExecutionTime() == null) {
                if (result instanceof SqlQueryResult) {
                    ((SqlQueryResult) result).setExecutionTime(executionTime);
                }
            }
            
            // 缓存SELECT查询结果
            if (useCache && result.isSuccess() && parsedSql.getSqlType() == SqlQueryResult.SqlType.SELECT) {
                String cacheKey = generateCacheKey(parsedSql, workbook);
                queryCacheService.putToCache(cacheKey, result);
            }
            
            return result;
        } catch (Exception e) {
            log.error("执行SQL查询异常: {}", e.getMessage(), e);
            return SqlQueryResult.error("执行SQL查询异常: " + e.getMessage());
        }
    }
    
    /**
     * 生成缓存键
     *
     * @param parsedSql 已解析的SQL
     * @param workbook 工作簿名称
     * @return 缓存键
     */
    private String generateCacheKey(ParsedSql parsedSql, String workbook) {
        return (workbook != null ? workbook : "default") + ":" + 
                String.join(",", parsedSql.getTargetTables()) + ":" + 
                parsedSql.getOriginalSql().hashCode();
    }
    
    /**
     * 判断是否为DDL操作
     */
    private boolean isDDLOperation(SqlQueryResult.SqlType sqlType) {
        return sqlType == SqlQueryResult.SqlType.CREATE_WORKBOOK ||
                sqlType == SqlQueryResult.SqlType.CREATE_SHEET ||
                sqlType == SqlQueryResult.SqlType.USE_WORKBOOK ||
                sqlType == SqlQueryResult.SqlType.DROP_WORKBOOK ||
                sqlType == SqlQueryResult.SqlType.DROP_SHEET;
    }
} 