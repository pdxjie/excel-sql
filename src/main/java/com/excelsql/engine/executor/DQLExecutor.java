package com.excelsql.engine.executor;

import com.excelsql.engine.parser.model.ParsedQuery;
import com.excelsql.engine.parser.model.QueryType;
import com.excelsql.engine.storage.ExcelStorage;
import com.excelsql.engine.function.FunctionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
/**
 * @Description: DQL 语句执行
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:41
 */
@Component
public class DQLExecutor implements QueryExecutor {

    @Autowired
    private ExcelStorage excelStorage;

    @Autowired
    private DDLExecutor ddlExecutor;

    @Override
    public Object executeQuery(ParsedQuery query) {
        if (query.getQueryType() != com.excelsql.engine.parser.model.QueryType.SELECT) {
            throw new UnsupportedOperationException("Unsupported DQL operation: " + query.getQueryType());
        }

        return selectData(query);
    }

    private Object selectData(ParsedQuery query) {
        String workbookName = query.getWorkbookName() != null ?
                query.getWorkbookName() : ddlExecutor.getCurrentWorkbook();

        if (workbookName == null) {
            throw new RuntimeException("No workbook selected. Use 'USE WORKBOOK' command first.");
        }

        List<Map<String, Object>> result = excelStorage.selectData(
                workbookName,
                query.getSheetName(),
                query.getColumns(),
                query.getConditions(),
                query.getOrderBy(),
                query.getGroupBy(),
                query.getLimit(),
                query.getOffset()
        );

        return result;
    }
}