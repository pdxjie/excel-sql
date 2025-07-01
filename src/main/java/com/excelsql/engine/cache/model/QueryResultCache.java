package com.excelsql.engine.cache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.bval.util.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 查询结果缓存类
 * 缓存SQL查询的结果，提高重复查询性能
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class QueryResultCache implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 查询SQL语句
     */
    private String querySQL;

    /**
     * 查询参数
     */
    private Map<String, Object> queryParameters;

    /**
     * 查询结果数据
     */
    private List<Map<String, Object>> resultData;

    /**
     * 结果集元数据
     */
    private QueryMetadata metadata;

    /**
     * 查询执行时间（毫秒）
     */
    private long executionTimeMs;

    /**
     * 缓存创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;

    /**
     * 访问次数
     */
    private AtomicLong accessCount;

    /**
     * 涉及的工作簿列表
     */
    private Set<String> involvedWorkbooks;

    /**
     * 涉及的工作表列表
     */
    private Set<String> involvedSheets;

    /**
     * 查询结果大小（字节）
     */
    private long resultSizeBytes;

    /**
     * 查询类型
     */
    private QueryType queryType;

    /**
     * 是否为分页查询结果
     */
    private boolean isPaginated;

    /**
     * 分页信息
     */
    private PaginationInfo paginationInfo;

    /**
     * 查询状态
     */
    private QueryStatus status;

    @Builder
    public QueryResultCache(String querySQL, Map<String, Object> queryParameters,
                            List<Map<String, Object>> resultData, long executionTimeMs) {
        this.querySQL = querySQL;
        this.queryParameters = queryParameters != null ? new HashMap<>(queryParameters) : new HashMap<>();
        this.resultData = resultData != null ? new ArrayList<>(resultData) : new ArrayList<>();
        this.executionTimeMs = executionTimeMs;
        this.createdTime = LocalDateTime.now();
        this.lastAccessTime = LocalDateTime.now();
        this.accessCount = new AtomicLong(1);
        this.involvedWorkbooks = new HashSet<>();
        this.involvedSheets = new HashSet<>();
        this.resultSizeBytes = estimateResultSize();
        this.queryType = determineQueryType(querySQL);
        this.isPaginated = false;
        this.status = QueryStatus.SUCCESS;

        log.debug("Created QueryResultCache for query with {} results", this.resultData.size());
    }

    /**
     * 获取查询结果
     */
    public List<Map<String, Object>> getResultData() {
        updateLastAccessTime();
        return new ArrayList<>(resultData);
    }

    /**
     * 获取结果行数
     */
    public int getResultCount() {
        return resultData != null ? resultData.size() : 0;
    }

    /**
     * 更新最后访问时间
     */
    public void updateLastAccessTime() {
        this.lastAccessTime = LocalDateTime.now();
        this.accessCount.incrementAndGet();
    }

    /**
     * 添加涉及的工作簿
     */
    public void addInvolvedWorkbook(String workbookName) {
        if (StringUtils.isNotBlank(workbookName)) {
            if (this.involvedWorkbooks == null) {
                this.involvedWorkbooks = new HashSet<>();
            }
            this.involvedWorkbooks.add(workbookName);
        }
    }

    /**
     * 添加涉及的工作表
     */
    public void addInvolvedSheet(String sheetName) {
        if (StringUtils.isNotBlank(sheetName)) {
            if (this.involvedSheets == null) {
                this.involvedSheets = new HashSet<>();
            }
            this.involvedSheets.add(sheetName);
        }
    }

    /**
     * 估算结果大小
     */
    private long estimateResultSize() {
        if (resultData == null || resultData.isEmpty()) {
            return 0L;
        }

        return resultData.stream()
                .mapToLong(row -> row.values().stream()
                        .mapToLong(value -> {
                            if (value == null) return 0;
                            if (value instanceof String) return ((String) value).length() * 2;
                            if (value instanceof Number) return 8;
                            return 32;
                        })
                        .sum())
                .sum();
    }

    /**
     * 确定查询类型
     */
    private QueryType determineQueryType(String sql) {
        if (StringUtils.isBlank(sql)) {
            return QueryType.UNKNOWN;
        }

        String upperSQL = sql.trim().toUpperCase();
        if (upperSQL.startsWith("SELECT")) {
            return QueryType.SELECT;
        } else if (upperSQL.startsWith("INSERT")) {
            return QueryType.INSERT;
        } else if (upperSQL.startsWith("UPDATE")) {
            return QueryType.UPDATE;
        } else if (upperSQL.startsWith("DELETE")) {
            return QueryType.DELETE;
        } else if (upperSQL.startsWith("CREATE")) {
            return QueryType.CREATE;
        } else if (upperSQL.startsWith("DROP")) {
            return QueryType.DROP;
        } else {
            return QueryType.OTHER;
        }
    }

    /**
     * 查询元数据类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryMetadata {
        private List<String> columnNames;
        private Map<String, String> columnTypes;
        private int totalRows;
        private int affectedRows;
        private boolean hasMoreData;
    }

    /**
     * 分页信息类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationInfo {
        private int currentPage;
        private int pageSize;
        private int totalPages;
        private long totalRows;
        private boolean hasNextPage;
        private boolean hasPreviousPage;
    }

    /**
     * 查询类型枚举
     */
    public enum QueryType {
        SELECT("查询"),
        INSERT("插入"),
        UPDATE("更新"),
        DELETE("删除"),
        CREATE("创建"),
        DROP("删除"),
        OTHER("其他"),
        UNKNOWN("未知");

        private final String description;

        QueryType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 查询状态枚举
     */
    public enum QueryStatus {
        SUCCESS("成功"),
        ERROR("错误"),
        TIMEOUT("超时"),
        CANCELLED("已取消");

        private final String description;

        QueryStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}

