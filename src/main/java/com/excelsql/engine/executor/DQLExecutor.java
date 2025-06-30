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

    @Resource
    private ExcelStorage excelStorage;

    @Resource
    private FunctionRegistry functionRegistry;

    @Override
    public boolean canExecute(ParsedQuery query) {
        return query.getQueryType() == QueryType.SELECT;
    }

    @Override
    public Map<String, Object> execute(ParsedQuery query) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Map<String, Object>> data = excelStorage.selectData(
                    query.getWorkbookName(),
                    query.getSheetName(),
                    query.getColumns(),
                    query.getConditions(),
                    query.getOrderBy(),
                    query.getGroupBy(),
                    query.getLimit(),
                    query.getOffset()
            );

            // Apply functions and aggregations
            if (query.getGroupBy() != null) {
                data = applyGroupBy(data, query.getGroupBy(), query.getColumns());
            }

            result.put("success", true);
            result.put("data", data);
            result.put("count", data.size());
            result.put("columns", getColumnInfo(data));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Failed to execute query: " + e.getMessage());
        }

        return result;
    }

    private List<Map<String, Object>> applyGroupBy(List<Map<String, Object>> data, String groupBy, List<String> columns) {
        Map<Object, List<Map<String, Object>>> grouped = new HashMap<>();

        // Group data by specified column
        for (Map<String, Object> row : data) {
            Object groupValue = row.get(groupBy);
            grouped.computeIfAbsent(groupValue, k -> new ArrayList<>()).add(row);
        }

        List<Map<String, Object>> result = new ArrayList<>();

        // Apply aggregation functions
        for (Map.Entry<Object, List<Map<String, Object>>> entry : grouped.entrySet()) {
            Map<String, Object> aggregatedRow = new HashMap<>();
            aggregatedRow.put(groupBy, entry.getKey());

            for (String column : columns) {
                if (!column.equals(groupBy) && !column.equals("*")) {
                    if (column.contains("(")) {
                        // Handle aggregate functions
                        Object aggregatedValue = applyAggregateFunction(column, entry.getValue());
                        aggregatedRow.put(column, aggregatedValue);
                    }
                }
            }

            result.add(aggregatedRow);
        }

        return result;
    }

    private Object applyAggregateFunction(String functionExpression, List<Map<String, Object>> groupData) {
        // Parse function name and column
        String functionName = functionExpression.substring(0, functionExpression.indexOf('('));
        String columnName = functionExpression.substring(
                functionExpression.indexOf('(') + 1,
                functionExpression.lastIndexOf(')')
        );

        switch (functionName.toUpperCase()) {
            case "COUNT":
                return (long) groupData.size();
            case "SUM":
                return groupData.stream()
                        .mapToDouble(row -> {
                            Object value = row.get(columnName);
                            return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
                        })
                        .sum();
            case "AVG":
                return groupData.stream()
                        .mapToDouble(row -> {
                            Object value = row.get(columnName);
                            return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
                        })
                        .average()
                        .orElse(0.0);
            case "MAX":
                return groupData.stream()
                        .map(row -> row.get(columnName))
                        .filter(Objects::nonNull)
                        .max(Comparator.comparing(Object::toString))
                        .orElse(null);
            case "MIN":
                return groupData.stream()
                        .map(row -> row.get(columnName))
                        .filter(Objects::nonNull)
                        .min(Comparator.comparing(Object::toString))
                        .orElse(null);
            default:
                return null;
        }
    }

    private List<Map<String, Object>> getColumnInfo(List<Map<String, Object>> data) {
        if (data.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> columnInfo = new ArrayList<>();
        Map<String, Object> firstRow = data.get(0);

        for (String columnName : firstRow.keySet()) {
            Map<String, Object> column = new HashMap<>();
            column.put("name", columnName);
            column.put("type", inferColumnType(data, columnName));
            columnInfo.add(column);
        }

        return columnInfo;
    }

    private String inferColumnType(List<Map<String, Object>> data, String columnName) {
        for (Map<String, Object> row : data) {
            Object value = row.get(columnName);
            if (value != null) {
                if (value instanceof Number) {
                    return "NUMBER";
                } else if (value instanceof Boolean) {
                    return "BOOLEAN";
                } else if (value instanceof Date) {
                    return "DATE";
                } else {
                    return "TEXT";
                }
            }
        }
        return "TEXT";
    }
}
