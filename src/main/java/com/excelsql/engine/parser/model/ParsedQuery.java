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
    /**
     * 查询类型，用于区分不同的查询操作
     */
    private QueryType queryType;

    /**
     * 工作簿名称，标识Excel文件的名字
     */
    private String workbookName;

    /**
     * 工作表名称，标识Excel文件中的某个工作表
     */
    private String sheetName;

    /**
     * 列名列表，用于指定查询中涉及的列
     */
    private List<String> columns;

    /**
     * 值的映射，用于存储查询结果中的列值对
     */
    private Map<String, Object> values;

    /**
     * 条件映射，用于指定查询的筛选条件
     */
    private Map<String, Object> conditions;

    /**
     * 排序依据，指定查询结果的排序规则
     */
    private String orderBy;

    /**
     * 分组依据，指定查询结果的分组规则
     */
    private String groupBy;

    /**
     * 分组筛选条件，用于进一步筛选分组后的结果
     */
    private String having;

    /**
     * 结果限制数量，指定查询结果的最大数量
     */
    private Integer limit;

    /**
     * 结果偏移量，与limit配合使用，用于分页查询
     */
    private Integer offset;

    /**
     * 列定义映射，用于定义查询涉及的列的属性
     */
    private Map<String, Object> columnDefinitions;

    /**
     * 目标工作表列表，用于指定查询操作涉及的工作表
     */
    private List<String> targetSheets;

    /**
     * 分割列，用于指定查询结果中用于分割的列
     */
    private String splitColumn;

    /**
     * 原始SQL，存储与查询等效的SQL语句
     */
    private String originalSQL;

}
