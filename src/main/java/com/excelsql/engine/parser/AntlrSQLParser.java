package com.excelsql.engine.parser;

import com.excelsql.engine.parser.model.ParsedQuery;
import com.excelsql.engine.parser.model.QueryType;
import com.excelsql.exception.ParseException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @Description: ANTLR 实现解析器
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:40
 */
@Component
public class AntlrSQLParser implements SQLParser {

    private static final Pattern SELECT_PATTERN = Pattern.compile(
            "SELECT\\s+(.*?)\\s+FROM\\s+SHEET\\s+[`\"']?([^`\"'\\s]+)[`\"']?(?:\\s+WHERE\\s+(.*?))?(?:\\s+GROUP\\s+BY\\s+(.*?))?(?:\\s+ORDER\\s+BY\\s+(.*?))?(?:\\s+LIMIT\\s+(\\d+))?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern INSERT_PATTERN = Pattern.compile(
            "INSERT\\s+INTO\\s+SHEET\\s+[`\"']?([^`\"'\\s]+)[`\"']?(?:\\s*\\(([^)]+)\\))?\\s+VALUES\\s*\\((.+)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern UPDATE_PATTERN = Pattern.compile(
            "UPDATE\\s+SHEET\\s+[`\"']?([^`\"'\\s]+)[`\"']?\\s+SET\\s+(.*?)(?:\\s+WHERE\\s+(.*?))?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern DELETE_PATTERN = Pattern.compile(
            "DELETE\\s+FROM\\s+SHEET\\s+[`\"']?([^`\"'\\s]+)[`\"']?(?:\\s+WHERE\\s+(.*?))?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern CREATE_WORKBOOK_PATTERN = Pattern.compile(
            "CREATE\\s+WORKBOOK\\s+[`\"']?([^`\"'\\s]+)[`\"']?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CREATE_SHEET_PATTERN = Pattern.compile(
            "CREATE\\s+SHEET\\s+[`\"']?([^`\"'\\s]+)[`\"']?(?:\\s*\\((.+)\\))?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern USE_WORKBOOK_PATTERN = Pattern.compile(
            "USE\\s+WORKBOOK\\s+[`\"']?([^`\"'\\s]+)[`\"']?",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public ParsedQuery parse(String sql) throws ParseException {
        if (sql == null || sql.trim().isEmpty()) {
            throw new ParseException("SQL cannot be empty");
        }

        String trimmedSQL = sql.trim();
        ParsedQuery query;

        if (trimmedSQL.toUpperCase().startsWith("SELECT")) {
            query = parseSelect(trimmedSQL);
        } else if (trimmedSQL.toUpperCase().startsWith("INSERT")) {
            query = parseInsert(trimmedSQL);
        } else if (trimmedSQL.toUpperCase().startsWith("UPDATE")) {
            query = parseUpdate(trimmedSQL);
        } else if (trimmedSQL.toUpperCase().startsWith("DELETE")) {
            query = parseDelete(trimmedSQL);
        } else if (trimmedSQL.toUpperCase().startsWith("CREATE WORKBOOK")) {
            query = parseCreateWorkbook(trimmedSQL);
        } else if (trimmedSQL.toUpperCase().startsWith("CREATE SHEET")) {
            query = parseCreateSheet(trimmedSQL);
        } else if (trimmedSQL.toUpperCase().startsWith("USE WORKBOOK")) {
            query = parseUseWorkbook(trimmedSQL);
        } else {
            throw new ParseException("Unsupported SQL statement: " + trimmedSQL);
        }

        query.setRawSQL(sql);
        return query;
    }

    private ParsedQuery parseSelect(String sql) throws ParseException {
        Matcher matcher = SELECT_PATTERN.matcher(sql);
        if (!matcher.find()) {
            throw new ParseException("Invalid SELECT statement: " + sql);
        }

        ParsedQuery query = new ParsedQuery(QueryType.SELECT);

        // Parse columns
        String columnsStr = matcher.group(1).trim();
        if ("*".equals(columnsStr)) {
            query.setColumns(Arrays.asList("*"));
        } else {
            query.setColumns(parseColumnList(columnsStr));
        }

        // Parse sheet name
        query.setSheetName(matcher.group(2));

        // Parse WHERE clause
        if (matcher.group(3) != null) {
            query.setConditions(parseWhereClause(matcher.group(3)));
        }

        // Parse GROUP BY
        if (matcher.group(4) != null) {
            query.setGroupBy(matcher.group(4).trim());
        }

        // Parse ORDER BY
        if (matcher.group(5) != null) {
            query.setOrderBy(matcher.group(5).trim());
        }

        // Parse LIMIT
        if (matcher.group(6) != null) {
            query.setLimit(Integer.parseInt(matcher.group(6)));
        }

        return query;
    }

    private ParsedQuery parseInsert(String sql) throws ParseException {
        Matcher matcher = INSERT_PATTERN.matcher(sql);
        if (!matcher.find()) {
            throw new ParseException("Invalid INSERT statement: " + sql);
        }

        ParsedQuery query = new ParsedQuery(QueryType.INSERT);
        query.setSheetName(matcher.group(1));

        // Parse columns if specified
        if (matcher.group(2) != null) {
            query.setColumns(parseColumnList(matcher.group(2)));
        }

        // Parse values
        String valuesStr = matcher.group(3);
        query.setValues(parseValuesList(valuesStr));

        return query;
    }

    private ParsedQuery parseUpdate(String sql) throws ParseException {
        Matcher matcher = UPDATE_PATTERN.matcher(sql);
        if (!matcher.find()) {
            throw new ParseException("Invalid UPDATE statement: " + sql);
        }

        ParsedQuery query = new ParsedQuery(QueryType.UPDATE);
        query.setSheetName(matcher.group(1));

        // Parse SET clause
        String setClause = matcher.group(2);
        query.setValues(parseSetClause(setClause));

        // Parse WHERE clause
        if (matcher.group(3) != null) {
            query.setConditions(parseWhereClause(matcher.group(3)));
        }

        return query;
    }

    private ParsedQuery parseDelete(String sql) throws ParseException {
        Matcher matcher = DELETE_PATTERN.matcher(sql);
        if (!matcher.find()) {
            throw new ParseException("Invalid DELETE statement: " + sql);
        }

        ParsedQuery query = new ParsedQuery(QueryType.DELETE);
        query.setSheetName(matcher.group(1));

        // Parse WHERE clause
        if (matcher.group(2) != null) {
            query.setConditions(parseWhereClause(matcher.group(2)));
        }

        return query;
    }

    private ParsedQuery parseCreateWorkbook(String sql) throws ParseException {
        Matcher matcher = CREATE_WORKBOOK_PATTERN.matcher(sql);
        if (!matcher.find()) {
            throw new ParseException("Invalid CREATE WORKBOOK statement: " + sql);
        }

        ParsedQuery query = new ParsedQuery(QueryType.CREATE_WORKBOOK);
        query.setWorkbookName(matcher.group(1));
        return query;
    }

    private ParsedQuery parseCreateSheet(String sql) throws ParseException {
        Matcher matcher = CREATE_SHEET_PATTERN.matcher(sql);
        if (!matcher.find()) {
            throw new ParseException("Invalid CREATE SHEET statement: " + sql);
        }

        ParsedQuery query = new ParsedQuery(QueryType.CREATE_SHEET);
        query.setSheetName(matcher.group(1));

        // Parse column definitions if provided
        if (matcher.group(2) != null) {
            Map<String, Object> columnDefs = parseColumnDefinitions(matcher.group(2));
            query.setAdditionalParams(Map.of("columnDefinitions", columnDefs));
        }

        return query;
    }

    private ParsedQuery parseUseWorkbook(String sql) throws ParseException {
        Matcher matcher = USE_WORKBOOK_PATTERN.matcher(sql);
        if (!matcher.find()) {
            throw new ParseException("Invalid USE WORKBOOK statement: " + sql);
        }

        ParsedQuery query = new ParsedQuery(QueryType.USE_WORKBOOK);
        query.setWorkbookName(matcher.group(1));
        return query;
    }

    private List<String> parseColumnList(String columnsStr) {
        return Arrays.stream(columnsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private Map<String, Object> parseWhereClause(String whereClause) {
        Map<String, Object> conditions = new HashMap<>();

        // Simple parsing for basic conditions
        String[] parts = whereClause.split("\\s+AND\\s+");
        for (String part : parts) {
            String[] condition = part.split("\\s*=\\s*", 2);
            if (condition.length == 2) {
                String column = condition[0].trim();
                String value = condition[1].trim().replaceAll("^['\"]|['\"]$", "");
                conditions.put(column, value);
            }
        }

        return conditions;
    }

    private Map<String, Object> parseValuesList(String valuesStr) {
        Map<String, Object> values = new HashMap<>();

        // Parse comma-separated values
        String[] valueArray = valuesStr.split(",");
        for (int i = 0; i < valueArray.length; i++) {
            String value = valueArray[i].trim().replaceAll("^['\"]|['\"]$", "");
            values.put("value" + i, parseValue(value));
        }

        return values;
    }

    private Map<String, Object> parseSetClause(String setClause) {
        Map<String, Object> values = new HashMap<>();

        String[] assignments = setClause.split(",");
        for (String assignment : assignments) {
            String[] parts = assignment.split("\\s*=\\s*", 2);
            if (parts.length == 2) {
                String column = parts[0].trim();
                String value = parts[1].trim().replaceAll("^['\"]|['\"]$", "");
                values.put(column, parseValue(value));
            }
        }

        return values;
    }

    private Map<String, Object> parseColumnDefinitions(String columnDefs) {
        Map<String, Object> definitions = new HashMap<>();

        String[] columns = columnDefs.split(",");
        for (String column : columns) {
            String[] parts = column.trim().split("\\s+");
            if (parts.length >= 2) {
                String columnName = parts[0];
                String columnType = parts[1];
                definitions.put(columnName, columnType);
            }
        }

        return definitions;
    }

    private Object parseValue(String value) {
        // Try to parse as number
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            // Return as string if not a number
            return value;
        }
    }
}
