package com.excelsql.engine.storage;

import java.time.LocalDateTime;
import java.util.List;
/**
 * @Description: TODO::Need To Do
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:41
 */
public class WorkbookMetadata {
    private String name;
    private String path;
    private long size;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private List<String> sheetNames;
    private String format; // xlsx, csv

    public WorkbookMetadata(String name, String path) {
        this.name = name;
        this.path = path;
        this.createdAt = LocalDateTime.now();
        this.modifiedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }

    public List<String> getSheetNames() { return sheetNames; }
    public void setSheetNames(List<String> sheetNames) { this.sheetNames = sheetNames; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}
