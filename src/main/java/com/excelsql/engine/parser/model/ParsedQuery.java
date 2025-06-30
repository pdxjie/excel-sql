package com.excelsql.engine.parser.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
/**
 * @Description: 解析后的查询对象
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:40
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParsedQuery {
    private QueryType queryType;
    private String workbookName;
    private String sheetName;
    private List<String> columns;
    private Map<String, Object> values;
    private Map<String, Object> conditions;
    private String orderBy;
    private String groupBy;
    private String having;
    private Integer limit;
    private Integer offset;
    private Map<String, Object> columnDefinitions;
    private List<String> targetSheets;
    private String splitColumn;
    private String originalSQL;
}
