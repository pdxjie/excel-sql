package com.excel.sql.engine.service;

import com.excel.sql.engine.service.executor.handler.impl.SelectQueryHandlerImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 缓存服务，用于管理查询缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {
    
    private final SelectQueryHandlerImpl selectQueryHandler;
    
    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        selectQueryHandler.clearCache();
        log.info("已清除所有查询缓存");
    }
    
    /**
     * 清除特定工作簿的缓存
     *
     * @param workbook 工作簿名称或ID
     */
    public void clearWorkbookCache(String workbook) {
        selectQueryHandler.clearCache(workbook);
        log.info("已清除工作簿 {} 的查询缓存", workbook);
    }
    
    /**
     * 清除与特定文件路径相关的缓存
     *
     * @param filePath 文件路径
     */
    public void clearFilePathCache(String filePath) {
        selectQueryHandler.clearCacheByFilePath(filePath);
        log.info("已清除文件 {} 相关的查询缓存", filePath);
    }
} 