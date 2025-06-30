package com.excelsql.engine.storage;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
/**
 * @Description: TODO::Need To Do
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:41
 */
@Data
public class WorkbookMetadata {
    private String name;
    private String path;
    private long size;
    private LocalDateTime created;
    private LocalDateTime lastModified;
    private List<String> sheetNames;
    private String format; // xlsx, csv
}
