package com.excelsql.engine.storage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
/**
 * @Description: TODO::Need To Do
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:41
 */
public class SheetMetadata {
    private String name;
    private String workbookName;
    private int rowCount;
    private int columnCount;
    private List<String> columnNames;
    private Map<String, String> columnTypes;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    public SheetMetadata(String name, String workbookName) {
        this.name = name;
        this.workbookName = workbookName;
        this.createdAt = LocalDateTime.now();
        this.modifiedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getWorkbookName() { return workbookName; }
    public void setWorkbookName(String workbookName) { this.workbookName = workbookName; }

    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }

    public int getColumnCount() { return columnCount; }
    public void setColumnCount(int columnCount) { this.columnCount = columnCount; }

    public List<String> getColumnNames() { return columnNames; }
    public void setColumnNames(List<String> columnNames) { this.columnNames = columnNames; }

    public Map<String, String> getColumnTypes() { return columnTypes; }
    public void setColumnTypes(Map<String, String> columnTypes) { this.columnTypes = columnTypes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }
}
