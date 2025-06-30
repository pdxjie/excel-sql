package com.excelsql.service;

import com.excelsql.engine.parser.model.ParsedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * @Description: 异步服务
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:43
 */

@Service
public class AsyncExcelService {

    @Autowired
    private ExcelSQLService excelSQLService;

    @Async("excelTaskExecutor")
    public CompletableFuture<Object> executeQueryAsync(String sql) {
        try {
            Object result = excelSQLService.executeSQL(sql);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            CompletableFuture<Object> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Async("excelTaskExecutor")
    public CompletableFuture<Void> preloadWorkbook(String workbookName) {
        // TODO: Implement workbook preloading logic
        return CompletableFuture.completedFuture(null);
    }
}
