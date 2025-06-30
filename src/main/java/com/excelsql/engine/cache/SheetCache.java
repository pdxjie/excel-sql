package com.excelsql.engine.cache;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @Description: Sheet 数据缓存
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:42
 */
@Data
public class SheetCache {
    private String workbookName;
    private String sheetName;
    private List<Map<String, Object>> data;
    private LocalDateTime lastAccessed;
    private long accessCount;
    private boolean dirty;

    public SheetCache(String workbookName, String sheetName, List<Map<String, Object>> data) {
        this.workbookName = workbookName;
        this.sheetName = sheetName;
        this.data = data;
        this.lastAccessed = LocalDateTime.now();
        this.accessCount = 1;
        this.dirty = false;
    }

    public void access() {
        this.lastAccessed = LocalDateTime.now();
        this.accessCount++;
    }

    public void markDirty() {
        this.dirty = true;
    }
}
