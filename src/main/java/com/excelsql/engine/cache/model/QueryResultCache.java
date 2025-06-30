package com.excelsql.engine.cache.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
public class QueryResultCache {
    private String query;
    private List<Map<String, Object>> result;
    private Set<String> involvedSheets; // 涉及的表格，用于缓存失效
    private LocalDateTime cacheTime;
    private long executionTimeMs;
}
