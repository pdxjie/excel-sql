package com.excelsql.engine.cache.model;

import com.excelsql.engine.storage.WorkbookMetadata;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class WorkbookCache {
    private String name;
    private String path;
    private List<String> sheetNames;
    private WorkbookMetadata metadata;
    private LocalDateTime lastAccessTime;
    private LocalDateTime cacheTime;

    public void updateLastAccess() {
        this.lastAccessTime = LocalDateTime.now();
    }
}