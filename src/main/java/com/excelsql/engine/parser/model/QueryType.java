package com.excelsql.engine.parser.model;

/**
 * @Description: 查询类型枚举
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:40
 */
public enum QueryType {
    SELECT,
    INSERT,
    UPDATE,
    DELETE,
    CREATE_WORKBOOK,
    CREATE_SHEET,
    DROP_WORKBOOK,
    DROP_SHEET,
    ALTER_SHEET,
    USE_WORKBOOK,
    COPY_SHEET,
    RENAME_SHEET,
    MERGE_SHEETS,
    SPLIT_SHEET
}
