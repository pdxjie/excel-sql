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

    @Resource
    private ExcelStorage excelStorage;

    @Override
    public boolean canExecute(ParsedQuery query) {
        return query.getQueryType() == QueryType.CREATE_WORKBOOK ||
                query.getQueryType() == QueryType.CREATE_SHEET ||
                query.getQueryType() == QueryType.DROP_WORKBOOK ||
                query.getQueryType() == QueryType.DROP_SHEET ||
                query.getQueryType() == QueryType.USE_WORKBOOK;
    }

    @Override
    public Map<String, Object> execute(ParsedQuery query) {
        Map<String, Object> result = new HashMap<>();

        switch (query.getQueryType()) {
            case CREATE_WORKBOOK:
                return executeCreateWorkbook(query);
            case CREATE_SHEET:
                return executeCreateSheet(query);
            case USE_WORKBOOK:
                return executeUseWorkbook(query);
            default:
                result.put("success", false);
                result.put("message", "Unsupported DDL operation");
                break;
        }

        return result;
    }

    private Map<String, Object> executeCreateWorkbook(ParsedQuery query) {
        Map<String, Object> result = new HashMap<>();

        try {
            boolean created = excelStorage.createWorkbook(query.getWorkbookName());
            result.put("success", created);
            result.put("message", created ? "Workbook created successfully" : "Workbook already exists");
            result.put("workbook", query.getWorkbookName());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Failed to create workbook: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> executeCreateSheet(ParsedQuery query) {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> columnDefs = null;
            if (query.getAdditionalParams() != null) {
                columnDefs = (Map<String, Object>) query.getAdditionalParams().get("columnDefinitions");
            }

            boolean created = excelStorage.createSheet(query.getWorkbookName(), query.getSheetName(), columnDefs);
            result.put("success", created);
            result.put("message", created ? "Sheet created successfully" : "Sheet already exists");
            result.put("sheet", query.getSheetName());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Failed to create sheet: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> executeUseWorkbook(ParsedQuery query) {
        Map<String, Object> result = new HashMap<>();

        try {
            boolean exists = excelStorage.workbookExists(query.getWorkbookName());
            if (exists) {
                // Set current workbook context
                result.put("success", true);
                result.put("message", "Using workbook: " + query.getWorkbookName());
                result.put("currentWorkbook", query.getWorkbookName());
            } else {
                result.put("success", false);
                result.put("message", "Workbook does not exist: " + query.getWorkbookName());
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Failed to use workbook: " + e.getMessage());
        }

        return result;
    }
}
