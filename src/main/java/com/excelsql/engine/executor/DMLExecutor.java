package com.excelsql.engine.executor;

import com.excelsql.engine.parser.model.ParsedQuery;
import com.excelsql.engine.storage.ExcelStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/**
 * @Description: DML 语句执行
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:41
 */
@Component
public class DMLExecutor implements QueryExecutor {

    @Autowired
    private ExcelStorage excelStorage;

    @Autowired
    private DDLExecutor ddlExecutor;

    @Override
    public Object executeQuery(ParsedQuery query) {
        switch (query.getQueryType()) {
            case INSERT:
                return insertData(query);
            case UPDATE:
                return updateData(query);
            case DELETE:
                return deleteData(query);
            default:
                throw new UnsupportedOperationException("Unsupported DML operation: " + query.getQueryType());
        }
    }

    private Object insertData(ParsedQuery query) {
        String workbookName = query.getWorkbookName() != null ?
                query.getWorkbookName() : ddlExecutor.getCurrentWorkbook();

        if (workbookName == null) {
            throw new RuntimeException("No workbook selected. Use 'USE WORKBOOK' command first.");
        }

        int rowsAffected = excelStorage.insertData(
                workbookName,
                query.getSheetName(),
                query.getColumns(),
                query.getValues()
        );

        return rowsAffected + " row(s) inserted";
    }

    private Object updateData(ParsedQuery query) {
        String workbookName = query.getWorkbookName() != null ?
                query.getWorkbookName() : ddlExecutor.getCurrentWorkbook();

        if (workbookName == null) {
            throw new RuntimeException("No workbook selected. Use 'USE WORKBOOK' command first.");
        }

        int rowsAffected = excelStorage.updateData(
                workbookName,
                query.getSheetName(),
                query.getValues(),
                query.getConditions()
        );

        return rowsAffected + " row(s) updated";
    }

    private Object deleteData(ParsedQuery query) {
        String workbookName = query.getWorkbookName() != null ?
                query.getWorkbookName() : ddlExecutor.getCurrentWorkbook();

        if (workbookName == null) {
            throw new RuntimeException("No workbook selected. Use 'USE WORKBOOK' command first.");
        }

        int rowsAffected = excelStorage.deleteData(
                workbookName,
                query.getSheetName(),
                query.getConditions()
        );

        return rowsAffected + " row(s) deleted";
    }
}
