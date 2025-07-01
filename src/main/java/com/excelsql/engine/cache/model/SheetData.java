package com.excelsql.engine.cache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.bval.util.StringUtils;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 工作表数据类
 * 缓存Excel工作表的实际数据内容
 */
@Data
@Slf4j
public class SheetData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 所属工作簿名称
     */
    private String workbookName;

    /**
     * 工作表名称
     */
    private String sheetName;

    /**
     * 数据内容（行列数据）
     */
    private List<Map<String, Object>> data;

    /**
     * 原始数据（保持原始格式）
     */
    private List<List<Object>> rawData;

    /**
     * 表头信息
     */
    private List<String> headers;

    /**
     * 数据加载时间
     */
    private LocalDateTime loadedTime;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;

    /**
     * 数据版本（用于并发控制）
     */
    private AtomicLong version;

    /**
     * 数据大小（字节）
     */
    private long dataSizeBytes;

    /**
     * 是否为部分数据（分页加载）
     */
    private boolean isPartialData;

    /**
     * 分页信息
     */
    private PageInfo pageInfo;

    /**
     * 数据状态
     */
    private DataStatus status;

    /**
     * 数据校验和
     */
    private String checksum;

    /**
     * 构造函数
     */
    public SheetData(String workbookName, String sheetName, List<Map<String, Object>> data) {
        this.workbookName = workbookName;
        this.sheetName = sheetName;
        this.data = data != null ? new ArrayList<>(data) : new ArrayList<>();
        this.headers = extractHeaders(this.data);
        this.loadedTime = LocalDateTime.now();
        this.lastAccessTime = LocalDateTime.now();
        this.version = new AtomicLong(1);
        this.dataSizeBytes = estimateDataSize();
        this.isPartialData = false;
        this.status = DataStatus.LOADED;
        this.checksum = calculateChecksum();

        log.debug("Created SheetData for {}#{} with {} rows", workbookName, sheetName,
                this.data.size());
    }

    /**
     * 获取数据行数
     */
    public int getRowCount() {
        return data != null ? data.size() : 0;
    }

    /**
     * 获取列数
     */
    public int getColumnCount() {
        return headers != null ? headers.size() : 0;
    }

    /**
     * 获取指定行的数据
     */
    public Optional<Map<String, Object>> getRow(int rowIndex) {
        if (data != null && rowIndex >= 0 && rowIndex < data.size()) {
            updateLastAccessTime();
            return Optional.of(new HashMap<>(data.get(rowIndex)));
        }
        return Optional.empty();
    }

    /**
     * 获取指定列的所有数据
     */
    public List<Object> getColumnData(String columnName) {
        if (data == null || StringUtils.isBlank(columnName)) {
            return new ArrayList<>();
        }

        updateLastAccessTime();
        return data.stream()
                .map(row -> row.get(columnName))
                .collect(Collectors.toList());
    }

    /**
     * 添加数据行
     */
    public void addRow(Map<String, Object> row) {
        if (row != null) {
            if (this.data == null) {
                this.data = new ArrayList<>();
            }
            this.data.add(new HashMap<>(row));
            this.version.incrementAndGet();
            this.dataSizeBytes = estimateDataSize();
            this.checksum = calculateChecksum();

            log.debug("Added row to {}#{}, new row count: {}", workbookName, sheetName, data.size());
        }
    }

    /**
     * 更新指定行数据
     */
    public boolean updateRow(int rowIndex, Map<String, Object> newRow) {
        if (data != null && rowIndex >= 0 && rowIndex < data.size() && newRow != null) {
            data.set(rowIndex, new HashMap<>(newRow));
            this.version.incrementAndGet();
            this.dataSizeBytes = estimateDataSize();
            this.checksum = calculateChecksum();

            log.debug("Updated row {} in {}#{}", rowIndex, workbookName, sheetName);
            return true;
        }
        return false;
    }

    /**
     * 删除指定行
     */
    public boolean removeRow(int rowIndex) {
        if (data != null && rowIndex >= 0 && rowIndex < data.size()) {
            data.remove(rowIndex);
            this.version.incrementAndGet();
            this.dataSizeBytes = estimateDataSize();
            this.checksum = calculateChecksum();

            log.debug("Removed row {} from {}#{}, new row count: {}",
                    rowIndex, workbookName, sheetName, data.size());
            return true;
        }
        return false;
    }

    /**
     * 更新最后访问时间
     */
    private void updateLastAccessTime() {
        this.lastAccessTime = LocalDateTime.now();
    }

    /**
     * 提取表头信息
     */
    private List<String> extractHeaders(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(data.get(0).keySet());
    }

    /**
     * 估算数据大小
     */
    private long estimateDataSize() {
        if (data == null || data.isEmpty()) {
            return 0L;
        }

        // 简单估算：每个字符2字节，每个数字8字节，每个对象额外开销32字节
        return data.stream()
                .mapToLong(row -> row.values().stream()
                        .mapToLong(value -> {
                            if (value == null) return 0;
                            if (value instanceof String) return ((String) value).length() * 2 + 32;
                            if (value instanceof Number) return 8 + 32;
                            return 32; // 其他类型
                        })
                        .sum())
                .sum();
    }

    /**
     * 计算数据校验和
     */
    private String calculateChecksum() {
        if (data == null || data.isEmpty()) {
            return "";
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            StringBuilder sb = new StringBuilder();

            for (Map<String, Object> row : data) {
                for (Object value : row.values()) {
                    sb.append(value != null ? value.toString() : "null");
                }
            }

            byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.warn("Failed to calculate checksum for {}#{}", workbookName, sheetName, e);
            return String.valueOf(data.hashCode());
        }
    }

    /**
     * 分页信息类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {
        private int pageNumber;
        private int pageSize;
        private int totalRows;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    /**
     * 数据状态枚举
     */
    public enum DataStatus {
        LOADING("加载中"),
        LOADED("已加载"),
        EXPIRED("已过期"),
        ERROR("错误状态");

        private final String description;

        DataStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
