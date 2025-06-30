package com.excelsql.engine.cache;

import lombok.Data;
import org.apache.poi.ss.usermodel.Workbook;
import java.time.LocalDateTime;
/**
 * @Description: 工作簿缓存
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:41
 */
@Data
public class WorkbookCache {
    private String workbookName;
    private Workbook workbook;
    private LocalDateTime lastAccessed;
    private long accessCount;
    private boolean dirty;

    public WorkbookCache(String workbookName, Workbook workbook) {
        this.workbookName = workbookName;
        this.workbook = workbook;
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
