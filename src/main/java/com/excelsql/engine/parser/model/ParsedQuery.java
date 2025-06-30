package com.excelsql.engine.parser.model;

import lombok.Data;

import java.util.List;
import java.util.Map;
/**
 * @Description: 解析后的查询对象
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:40
 */
@Data
public class ParsedQuery {
    private QueryType queryType;
    private String workbookName;
    private String sheetName;
    private List<String> columns;
    private Map<String, Object> conditions;
    private Map<String, Object> values;
    private String orderBy;
    private String groupBy;
    private String having;
    private Integer limit;
    private Integer offset;
    private String rawSQL;
    private Map<String, Object> additionalParams;

    public ParsedQuery(QueryType queryType) {
        this.queryType = queryType;
    }
}
