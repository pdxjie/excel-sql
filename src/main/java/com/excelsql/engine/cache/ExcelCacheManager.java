package com.excelsql.engine.cache;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description: 缓存管理
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:41
 */
@Component
public class ExcelCacheManager {

    private final Map<String, WorkbookCache> workbookCache = new ConcurrentHashMap<>();
    private final Map<String, SheetCache> sheetCache = new ConcurrentHashMap<>();

    public void cacheWorkbook(String workbookName, WorkbookCache cache) {
        workbookCache.put(workbookName, cache);
    }

    public WorkbookCache getWorkbook(String workbookName) {
        WorkbookCache cache = workbookCache.get(workbookName);
        if (cache != null) {
            cache.access();
        }
        return cache;
    }

    public void evictWorkbook(String workbookName) {
        workbookCache.remove(workbookName);
        // Also remove related sheet caches
        sheetCache.entrySet().removeIf(entry ->
                entry.getValue().getWorkbookName().equals(workbookName));
    }

    public void cacheSheet(String key, SheetCache cache) {
        sheetCache.put(key, cache);
    }

    public SheetCache getSheet(String workbookName, String sheetName) {
        String key = workbookName + ":" + sheetName;
        SheetCache cache = sheetCache.get(key);
        if (cache != null) {
            cache.access();
        }
        return cache;
    }

    public void evictSheet(String workbookName, String sheetName) {
        String key = workbookName + ":" + sheetName;
        sheetCache.remove(key);
    }

    public void clearAll() {
        workbookCache.clear();
        sheetCache.clear();
    }
}
