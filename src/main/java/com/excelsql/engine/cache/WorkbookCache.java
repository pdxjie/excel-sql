package com.excelsql.engine.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.bval.util.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
/**
 * @Description: 工作簿缓存
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:41
 */
/**
 * 工作簿缓存对象
 * 存储Excel工作簿的元数据信息，避免重复解析文件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class WorkbookCache implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工作簿名称（文件名）
     */
    private String workbookName;

    /**
     * 工作簿完整路径
     */
    private String workbookPath;

    /**
     * 文件大小（字节）
     */
    private long fileSize;

    /**
     * 文件最后修改时间
     */
    private LocalDateTime lastModifiedTime;

    /**
     * 工作表信息列表
     */
    private List<SheetInfo> sheetInfos;

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
     * 工作簿格式类型
     */
    private WorkbookFormat format;

    /**
     * 是否启用公式计算
     */
    private boolean formulaEvaluationEnabled;

    /**
     * 自定义属性映射
     */
    private Map<String, Object> properties;

    /**
     * 构造函数初始化
     */
    @Builder
    public WorkbookCache(String workbookName, String workbookPath, long fileSize,
                         LocalDateTime lastModifiedTime, List<SheetInfo> sheetInfos) {
        this.workbookName = workbookName;
        this.workbookPath = workbookPath;
        this.fileSize = fileSize;
        this.lastModifiedTime = lastModifiedTime;
        this.sheetInfos = sheetInfos != null ? new ArrayList<>(sheetInfos) : new ArrayList<>();
        this.createdTime = LocalDateTime.now();
        this.lastAccessTime = LocalDateTime.now();
        this.accessCount = new AtomicLong(0);
        this.formulaEvaluationEnabled = false;
        this.properties = new ConcurrentHashMap<>();

        // 根据文件扩展名判断格式
        if (workbookPath != null) {
            if (workbookPath.toLowerCase().endsWith(".xlsx") || workbookPath.toLowerCase().endsWith(".xlsm")) {
                this.format = WorkbookFormat.XLSX;
            } else if (workbookPath.toLowerCase().endsWith(".xls")) {
                this.format = WorkbookFormat.XLS;
            } else if (workbookPath.toLowerCase().endsWith(".csv")) {
                this.format = WorkbookFormat.CSV;
            } else {
                this.format = WorkbookFormat.UNKNOWN;
            }
        }

        log.debug("Created WorkbookCache for: {}", workbookName);
    }

    /**
     * 更新最后访问时间
     */
    public void updateLastAccess() {
        this.lastAccessTime = LocalDateTime.now();
        this.accessCount.incrementAndGet();
        log.trace("Updated last access time for workbook: {}", workbookName);
    }

    /**
     * 获取指定名称的工作表信息
     */
    public Optional<SheetInfo> getSheetInfo(String sheetName) {
        if (sheetInfos == null || StringUtils.isBlank(sheetName)) {
            return Optional.empty();
        }

        return sheetInfos.stream()
                .filter(sheet -> sheetName.equals(sheet.getSheetName()))
                .findFirst();
    }

    /**
     * 添加工作表信息
     */
    public void addSheetInfo(SheetInfo sheetInfo) {
        if (sheetInfo != null) {
            if (this.sheetInfos == null) {
                this.sheetInfos = new ArrayList<>();
            }
            this.sheetInfos.add(sheetInfo);
            log.debug("Added sheet info: {} to workbook: {}", sheetInfo.getSheetName(), workbookName);
        }
    }

    /**
     * 移除工作表信息
     */
    public boolean removeSheetInfo(String sheetName) {
        if (sheetInfos != null && StringUtils.isNotBlank(sheetName)) {
            boolean removed = sheetInfos.removeIf(sheet -> sheetName.equals(sheet.getSheetName()));
            if (removed) {
                log.debug("Removed sheet info: {} from workbook: {}", sheetName, workbookName);
            }
            return removed;
        }
        return false;
    }

    /**
     * 检查文件是否已被修改
     */
    public boolean isFileModified() {
        try {
            Path path = Paths.get(workbookPath);
            if (!Files.exists(path)) {
                log.warn("Workbook file not found: {}", workbookPath);
                return true;
            }

            LocalDateTime currentLastModified = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(path).toInstant(),
                    ZoneId.systemDefault()
            );

            return !currentLastModified.equals(this.lastModifiedTime);
        } catch (IOException e) {
            log.error("Error checking file modification time for: {}", workbookPath, e);
            return true;
        }
    }

    /**
     * 工作簿格式枚举
     */
    public enum WorkbookFormat {
        XLS("Excel 97-2003"),
        XLSX("Excel 2007+"),
        CSV("Comma Separated Values"),
        UNKNOWN("Unknown Format");

        private final String description;

        WorkbookFormat(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
