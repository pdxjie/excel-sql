package com.excelsql.engine.cache.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class SheetData {
    private String workbookName;
    private String sheetName;
    private List<Map<String, Object>> data;
    private LocalDateTime cacheTime;

    public SheetData(String workbookName, String sheetName, List<Map<String, Object>> data) {
        this.workbookName = workbookName;
        this.sheetName = sheetName;
        this.data = new ArrayList<>(data); // 创建副本避免外部修改
        this.cacheTime = LocalDateTime.now();
    }
}