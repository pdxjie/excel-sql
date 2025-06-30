package com.excelsql.engine.executor;

import com.excelsql.engine.parser.model.ParsedQuery;
import com.excelsql.engine.parser.model.QueryType;
import com.excelsql.engine.storage.ExcelStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
/**
 * @Description: DDL 语句执行
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:41
 */
@Component
public class DDLExecutor implements QueryExecutor {

    @Autowired
    private ExcelStorage excelStorage;

    private String currentWorkbook;

    @Override
    public Object executeQuery(ParsedQuery query) {
        switch (query.getQueryType()) {
            case CREATE_WORKBOOK:
                return createWorkbook(query);
            case CREATE_SHEET:
                return createSheet(query);
            case DROP_WORKBOOK:
                return dropWorkbook(query);
            case DROP_SHEET:
                return dropSheet(query);
            case USE_WORKBOOK:
                return useWorkbook(query);
            case SHOW_WORKBOOKS:
                return showWorkbooks();
            case SHOW_SHEETS:
                return showSheets();
            default:
                throw new UnsupportedOperationException("Unsupported DDL operation: " + query.getQueryType());
        }
    }

    private Object createWorkbook(ParsedQuery query) {
        boolean created = excelStorage.createWorkbook(query.getWorkbookName());
        return created ? "Workbook created successfully" : "Workbook already exists";
    }

    private Object createSheet(ParsedQuery query) {
        String workbookName = query.getWorkbookName() != null ? query.getWorkbookName() : currentWorkbook;
        if (workbookName == null) {
            throw new RuntimeException("No workbook selected. Use 'USE WORKBOOK' command first.");
        }

        boolean created = excelStorage.createSheet(workbookName, query.getSheetName(), query.getColumnDefinitions());
        return created ? "Sheet created successfully" : "Sheet already exists";
    }

    private Object dropWorkbook(ParsedQuery query) {
        boolean deleted = excelStorage.deleteWorkbook(query.getWorkbookName());
        return deleted ? "Workbook deleted successfully" : "Workbook not found";
    }

    private Object dropSheet(ParsedQuery query) {
        String workbookName = query.getWorkbookName() != null ? query.getWorkbookName() : currentWorkbook;
        if (workbookName == null) {
            throw new RuntimeException("No workbook selected. Use 'USE WORKBOOK' command first.");
        }

        boolean deleted = excelStorage.deleteSheet(workbookName, query.getSheetName());
        return deleted ? "Sheet deleted successfully" : "Sheet not found";
    }

    private Object useWorkbook(ParsedQuery query) {
        if (excelStorage.workbookExists(query.getWorkbookName())) {
            currentWorkbook = query.getWorkbookName();
            return "Using workbook: " + currentWorkbook;
        } else {
            throw new RuntimeException("Workbook not found: " + query.getWorkbookName());
        }
    }

    private Object showWorkbooks() {
        return excelStorage.listWorkbooks();
    }

    private Object showSheets() {
        if (currentWorkbook == null) {
            throw new RuntimeException("No workbook selected. Use 'USE WORKBOOK' command first.");
        }
        return excelStorage.listSheets(currentWorkbook);
    }

    public String getCurrentWorkbook() {
        return currentWorkbook;
    }

    public void setCurrentWorkbook(String workbook) {
        this.currentWorkbook = workbook;
    }
}