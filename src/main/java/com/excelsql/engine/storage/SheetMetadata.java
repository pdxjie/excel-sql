package com.excelsql.engine.storage;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
/**
 * @Description: TODO::Need To Do
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:41
 */

@Data
public class SheetMetadata {
    private String name;
    private String workbookName;
    private int rowCount;
    private int columnCount;
    private List<String> columnNames;
    private Map<String, String> columnTypes;
    private LocalDateTime lastModified;
    private boolean hasHeader;
}
