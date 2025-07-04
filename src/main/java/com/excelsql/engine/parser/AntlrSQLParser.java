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

    // 通用标识符匹配模式：支持反引号、双引号、单引号包裹或直接的字母数字组合
    private static final String IDENTIFIER_PATTERN = "(?:`([^`]+)`|\"([^\"]+)\"|'([^']+)'|([a-zA-Z_][a-zA-Z0-9_.-]*))";

    private static final Pattern CREATE_WORKBOOK_PATTERN =
            Pattern.compile("CREATE\\s+WORKBOOK\\s+" + IDENTIFIER_PATTERN, Pattern.CASE_INSENSITIVE);

    private static final Pattern CREATE_SHEET_PATTERN =
            Pattern.compile("CREATE\\s+SHEET\\s+" + IDENTIFIER_PATTERN + "\\s*(\\(.*\\))?", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern SELECT_PATTERN =
            Pattern.compile("SELECT\\s+(.*?)\\s+FROM\\s+SHEET\\s+" + IDENTIFIER_PATTERN +
                            "(?:\\s+WHERE\\s+(.*?))?(?:\\s+GROUP\\s+BY\\s+(.*?))?(?:\\s+ORDER\\s+BY\\s+(.*?))?(?:\\s+LIMIT\\s+(\\d+)(?:\\s+OFFSET\\s+(\\d+))?)?",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern INSERT_PATTERN =
            Pattern.compile("INSERT\\s+INTO\\s+SHEET\\s+" + IDENTIFIER_PATTERN + "(?:\\s*\\(([^)]+)\\))?\\s+VALUES\\s*\\(([^)]+)\\)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern UPDATE_PATTERN =
            Pattern.compile("UPDATE\\s+SHEET\\s+" + IDENTIFIER_PATTERN + "\\s+SET\\s+(.*?)(?:\\s+WHERE\\s+(.*?))?",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern DELETE_PATTERN =
            Pattern.compile("DELETE\\s+FROM\\s+SHEET\\s+" + IDENTIFIER_PATTERN + "(?:\\s+WHERE\\s+(.*?))?",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern USE_WORKBOOK_PATTERN =
            Pattern.compile("USE\\s+(?:WORKBOOK\\s+)?" + IDENTIFIER_PATTERN, Pattern.CASE_INSENSITIVE);


    /**
     * 从匹配结果中提取标识符名称
     * @param matcher 正则匹配器
     * @param groupOffset 标识符捕获组的起始位置
     * @return 提取的标识符名称
     */
    private static String extractIdentifier(Matcher matcher, int groupOffset) {
        String backtickQuoted = matcher.group(groupOffset);     // `name`
        String doubleQuoted = matcher.group(groupOffset + 1);   // "name"
        String singleQuoted = matcher.group(groupOffset + 2);   // 'name'
        String unquoted = matcher.group(groupOffset + 3);       // name

        if (backtickQuoted != null) return backtickQuoted;
        if (doubleQuoted != null) return doubleQuoted;
        if (singleQuoted != null) return singleQuoted;
        if (unquoted != null) return unquoted;

        return null;
    }

    @Override
    public ParsedQuery parseSQL(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new ParseException("SQL statement cannot be empty");
        }

        sql = sql.trim();
        ParsedQuery query = new ParsedQuery();
        query.setOriginalSQL(sql);

        try {
            // CREATE WORKBOOK
            Matcher matcher = CREATE_WORKBOOK_PATTERN.matcher(sql);
            if (matcher.find()) {
                query.setQueryType(QueryType.CREATE_WORKBOOK);
                query.setWorkbookName(extractIdentifier(matcher, 1));
                return query;
            }

            // CREATE SHEET
            matcher = CREATE_SHEET_PATTERN.matcher(sql);
            if (matcher.find()) {
                query.setQueryType(QueryType.CREATE_SHEET);
                query.setSheetName(extractIdentifier(matcher, 1));
                // 列定义在第5个组（因为标识符占用了4个组）
                if (matcher.group(5) != null) {
                    query.setColumnDefinitions(parseColumnDefinitions(matcher.group(5)));
                }
                return query;
            }

            // SELECT
            matcher = SELECT_PATTERN.matcher(sql);
            if (matcher.find()) {
                query.setQueryType(QueryType.SELECT);
                query.setColumns(parseColumns(matcher.group(1)));
                query.setSheetName(extractIdentifier(matcher, 2));
                // WHERE子句在第6个组
                if (matcher.group(6) != null) {
                    query.setConditions(parseWhereConditions(matcher.group(6)));
                }
                // GROUP BY在第7个组
                if (matcher.group(7) != null) {
                    query.setGroupBy(matcher.group(7).trim());
                }
                // ORDER BY在第8个组
                if (matcher.group(8) != null) {
                    query.setOrderBy(matcher.group(8).trim());
                }
                // LIMIT在第9个组
                if (matcher.group(9) != null) {
                    query.setLimit(Integer.parseInt(matcher.group(9)));
                }
                // OFFSET在第10个组
                if (matcher.group(10) != null) {
                    query.setOffset(Integer.parseInt(matcher.group(10)));
                }
                return query;
            }

            // INSERT
            matcher = INSERT_PATTERN.matcher(sql);
            if (matcher.find()) {
                query.setQueryType(QueryType.INSERT);
                query.setSheetName(extractIdentifier(matcher, 1));
                // 列定义在第5个组
                if (matcher.group(5) != null) {
                    query.setColumns(parseColumns(matcher.group(5)));
                }
                // VALUES在第6个组
                query.setValues(parseValues(matcher.group(6), query.getColumns()));
                return query;
            }

            // UPDATE
            matcher = UPDATE_PATTERN.matcher(sql);
            if (matcher.find()) {
                query.setQueryType(QueryType.UPDATE);
                query.setSheetName(extractIdentifier(matcher, 1));
                // SET子句在第5个组
                query.setValues(parseSetClause(matcher.group(5)));
                // WHERE子句在第6个组
                if (matcher.group(6) != null) {
                    query.setConditions(parseWhereConditions(matcher.group(6)));
                }
                return query;
            }

            // DELETE
            matcher = DELETE_PATTERN.matcher(sql);
            if (matcher.find()) {
                query.setQueryType(QueryType.DELETE);
                query.setSheetName(extractIdentifier(matcher, 1));
                // WHERE子句在第5个组
                if (matcher.group(5) != null) {
                    query.setConditions(parseWhereConditions(matcher.group(5)));
                }
                return query;
            }

            // USE WORKBOOK
            matcher = USE_WORKBOOK_PATTERN.matcher(sql);
            if (matcher.find()) {
                query.setQueryType(QueryType.USE_WORKBOOK);
                query.setWorkbookName(extractIdentifier(matcher, 1));
                return query;
            }

            // Handle other simple commands
            if (sql.toUpperCase().startsWith("SHOW WORKBOOKS")) {
                query.setQueryType(QueryType.SHOW_WORKBOOKS);
                return query;
            }

            if (sql.toUpperCase().startsWith("SHOW SHEETS")) {
                query.setQueryType(QueryType.SHOW_SHEETS);
                return query;
            }

            throw new ParseException("Unsupported SQL statement: " + sql);

        } catch (Exception e) {
            throw new ParseException("Failed to parse SQL: " + sql, e);
        }
    }

    private List<String> parseColumns(String columnsStr) {
        if ("*".equals(columnsStr.trim())) {
            return Arrays.asList("*");
        }

        List<String> columns = new ArrayList<>();
        String[] parts = columnsStr.split(",");
        for (String part : parts) {
            columns.add(part.trim());
        }
        return columns;
    }

    private Map<String, Object> parseColumnDefinitions(String columnDefs) {
        Map<String, Object> definitions = new LinkedHashMap<>();

        if (columnDefs == null || columnDefs.trim().isEmpty()) {
            return definitions;
        }

        // Remove outer parentheses
        String cleanDefs = columnDefs.trim();
        if (cleanDefs.startsWith("(") && cleanDefs.endsWith(")")) {
            cleanDefs = cleanDefs.substring(1, cleanDefs.length() - 1);
        }

        // Split by comma, but handle nested parentheses
        List<String> parts = splitByCommaRespectingParentheses(cleanDefs);

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }

            // Parse column definition: column_name TYPE [constraints...]
            String[] tokens = part.split("\\s+");
            if (tokens.length >= 2) {
                String columnName = tokens[0];

                // Build the complete column definition (type + constraints)
                StringBuilder columnDefBuilder = new StringBuilder();
                for (int i = 1; i < tokens.length; i++) {
                    if (i > 1) {
                        columnDefBuilder.append(" ");
                    }
                    columnDefBuilder.append(tokens[i]);
                }

                definitions.put(columnName, columnDefBuilder.toString());
            }
        }

        return definitions;
    }

    /**
     * Split string by comma while respecting parentheses nesting
     */
    private List<String> splitByCommaRespectingParentheses(String input) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenthesesLevel = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '(') {
                parenthesesLevel++;
                current.append(c);
            } else if (c == ')') {
                parenthesesLevel--;
                current.append(c);
            } else if (c == ',' && parenthesesLevel == 0) {
                // This comma is not inside parentheses, so it's a separator
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        // Add the last part
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        return result;
    }

    private Map<String, Object> parseWhereConditions(String whereClause) {
        Map<String, Object> conditions = new HashMap<>();

        // Simple parsing for conditions like: column = 'value' AND column2 = 123
        String[] andParts = whereClause.split("\\s+AND\\s+");

        for (String part : andParts) {
            String[] equalsParts = part.split("\\s*=\\s*");
            if (equalsParts.length == 2) {
                String column = equalsParts[0].trim();
                String value = equalsParts[1].trim();

                // Remove quotes if present
                if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                }

                // Try to parse as number
                try {
                    if (value.contains(".")) {
                        conditions.put(column, Double.parseDouble(value));
                    } else {
                        conditions.put(column, Long.parseLong(value));
                    }
                } catch (NumberFormatException e) {
                    conditions.put(column, value);
                }
            }
        }

        return conditions;
    }

    private Map<String, Object> parseValues(String valuesStr, List<String> columns) {
        Map<String, Object> values = new HashMap<>();

        String[] valueParts = valuesStr.split(",");

        if (columns != null && !columns.isEmpty()) {
            for (int i = 0; i < valueParts.length && i < columns.size(); i++) {
                String value = valueParts[i].trim();

                // Remove quotes if present
                if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                }

                values.put(columns.get(i), parseValue(value));
            }
        } else {
            for (int i = 0; i < valueParts.length; i++) {
                String value = valueParts[i].trim();

                // Remove quotes if present
                if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                }

                values.put("value" + i, parseValue(value));
            }
        }

        return values;
    }

    private Map<String, Object> parseSetClause(String setClause) {
        Map<String, Object> values = new HashMap<>();

        String[] setParts = setClause.split(",");
        for (String part : setParts) {
            String[] equalsParts = part.split("\\s*=\\s*");
            if (equalsParts.length == 2) {
                String column = equalsParts[0].trim();
                String value = equalsParts[1].trim();

                // Remove quotes if present
                if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                }

                values.put(column, parseValue(value));
            }
        }

        return values;
    }

    private Object parseValue(String value) {
        if (value == null || "NULL".equalsIgnoreCase(value)) {
            return null;
        }

        // Try to parse as number
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            // Try boolean
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                return Boolean.parseBoolean(value);
            }

            // Default to string
            return value;
        }
    }

    // 测试方法 - 可以在开发环境中使用来验证解析结果
    public static void testColumnDefinitionParsing() {
        AntlrSQLParser parser = new AntlrSQLParser();

        String[] testCases = {
                "CREATE SHEET monthly_sales (id INT PRIMARY KEY, product_name VARCHAR(100), sales_amount DECIMAL(10,2), sale_date DATE, region VARCHAR(50))",
                "CREATE SHEET `user data` (id INT, name VARCHAR(255), email VARCHAR(100) UNIQUE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
                "CREATE SHEET simple_table (id INT, name TEXT)",
                "CREATE SHEET complex_table (id BIGINT AUTO_INCREMENT PRIMARY KEY, data JSON, coords POINT, metadata VARCHAR(1000) NOT NULL)",
                "USE `sales_data.xlsx`",
                "USE sales_data.xlsx",
                "USE \"my-workbook.xlsx\"",
                "USE 'data_2024.xlsx'",
                "CREATE WORKBOOK test-file.xlsx",
                "CREATE WORKBOOK `my workbook.xlsx`",
                "SELECT * FROM SHEET data_2024.csv",
                "SELECT * FROM SHEET `file name.xlsx`"
        };

        for (String sql : testCases) {
            System.out.println("Testing SQL: " + sql);
            try {
                ParsedQuery query = parser.parseSQL(sql);
                System.out.println("Query Type: " + query.getQueryType());
                if (query.getWorkbookName() != null) {
                    System.out.println("Workbook Name: " + query.getWorkbookName());
                }
                if (query.getSheetName() != null) {
                    System.out.println("Sheet Name: " + query.getSheetName());
                }
                if (query.getColumnDefinitions() != null && !query.getColumnDefinitions().isEmpty()) {
                    System.out.println("Column Definitions: " + query.getColumnDefinitions());
                }
                System.out.println("---");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                System.out.println("---");
            }
        }
    }

    public static void main(String[] args) {
        testColumnDefinitionParsing();
    }
}
//@Component
//public class AntlrSQLParser implements SQLParser {
//
//    private static final Pattern CREATE_WORKBOOK_PATTERN =
//            Pattern.compile("CREATE\\s+WORKBOOK\\s+`([^`]+)`", Pattern.CASE_INSENSITIVE);
//
//    private static final Pattern CREATE_SHEET_PATTERN =
//            Pattern.compile("CREATE\\s+SHEET\\s+`([^`]+)`\\s*(\\([^)]+\\))?", Pattern.CASE_INSENSITIVE);
//
//    private static final Pattern SELECT_PATTERN =
//            Pattern.compile("SELECT\\s+(.*?)\\s+FROM\\s+SHEET\\s+`([^`]+)`(?:\\s+WHERE\\s+(.*?))?(?:\\s+GROUP\\s+BY\\s+(.*?))?(?:\\s+ORDER\\s+BY\\s+(.*?))?(?:\\s+LIMIT\\s+(\\d+)(?:\\s+OFFSET\\s+(\\d+))?)?",
//                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
//
//    private static final Pattern INSERT_PATTERN =
//            Pattern.compile("INSERT\\s+INTO\\s+SHEET\\s+`([^`]+)`(?:\\s*\\(([^)]+)\\))?\\s+VALUES\\s*\\(([^)]+)\\)",
//                    Pattern.CASE_INSENSITIVE);
//
//    private static final Pattern UPDATE_PATTERN =
//            Pattern.compile("UPDATE\\s+SHEET\\s+`([^`]+)`\\s+SET\\s+(.*?)(?:\\s+WHERE\\s+(.*?))?",
//                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
//
//    private static final Pattern DELETE_PATTERN =
//            Pattern.compile("DELETE\\s+FROM\\s+SHEET\\s+`([^`]+)`(?:\\s+WHERE\\s+(.*?))?",
//                    Pattern.CASE_INSENSITIVE);
//
//    private static final Pattern USE_WORKBOOK_PATTERN =
//            Pattern.compile("USE\\s+WORKBOOK\\s+`([^`]+)`", Pattern.CASE_INSENSITIVE);
//
//    @Override
//    public ParsedQuery parseSQL(String sql) {
//        if (sql == null || sql.trim().isEmpty()) {
//            throw new ParseException("SQL statement cannot be empty");
//        }
//
//        sql = sql.trim();
//        ParsedQuery query = new ParsedQuery();
//        query.setOriginalSQL(sql);
//
//        try {
//            // CREATE WORKBOOK
//            Matcher matcher = CREATE_WORKBOOK_PATTERN.matcher(sql);
//            if (matcher.find()) {
//                query.setQueryType(QueryType.CREATE_WORKBOOK);
//                query.setWorkbookName(matcher.group(1));
//                return query;
//            }
//
//            // CREATE SHEET
//            matcher = CREATE_SHEET_PATTERN.matcher(sql);
//            if (matcher.find()) {
//                query.setQueryType(QueryType.CREATE_SHEET);
//                query.setSheetName(matcher.group(1));
//                if (matcher.group(2) != null) {
//                    query.setColumnDefinitions(parseColumnDefinitions(matcher.group(2)));
//                }
//                return query;
//            }
//
//            // SELECT
//            matcher = SELECT_PATTERN.matcher(sql);
//            if (matcher.find()) {
//                query.setQueryType(QueryType.SELECT);
//                query.setColumns(parseColumns(matcher.group(1)));
//                query.setSheetName(matcher.group(2));
//                if (matcher.group(3) != null) {
//                    query.setConditions(parseWhereConditions(matcher.group(3)));
//                }
//                if (matcher.group(4) != null) {
//                    query.setGroupBy(matcher.group(4).trim());
//                }
//                if (matcher.group(5) != null) {
//                    query.setOrderBy(matcher.group(5).trim());
//                }
//                if (matcher.group(6) != null) {
//                    query.setLimit(Integer.parseInt(matcher.group(6)));
//                }
//                if (matcher.group(7) != null) {
//                    query.setOffset(Integer.parseInt(matcher.group(7)));
//                }
//                return query;
//            }
//
//            // INSERT
//            matcher = INSERT_PATTERN.matcher(sql);
//            if (matcher.find()) {
//                query.setQueryType(QueryType.INSERT);
//                query.setSheetName(matcher.group(1));
//                if (matcher.group(2) != null) {
//                    query.setColumns(parseColumns(matcher.group(2)));
//                }
//                query.setValues(parseValues(matcher.group(3), query.getColumns()));
//                return query;
//            }
//
//            // UPDATE
//            matcher = UPDATE_PATTERN.matcher(sql);
//            if (matcher.find()) {
//                query.setQueryType(QueryType.UPDATE);
//                query.setSheetName(matcher.group(1));
//                query.setValues(parseSetClause(matcher.group(2)));
//                if (matcher.group(3) != null) {
//                    query.setConditions(parseWhereConditions(matcher.group(3)));
//                }
//                return query;
//            }
//
//            // DELETE
//            matcher = DELETE_PATTERN.matcher(sql);
//            if (matcher.find()) {
//                query.setQueryType(QueryType.DELETE);
//                query.setSheetName(matcher.group(1));
//                if (matcher.group(2) != null) {
//                    query.setConditions(parseWhereConditions(matcher.group(2)));
//                }
//                return query;
//            }
//
//            // USE WORKBOOK
//            matcher = USE_WORKBOOK_PATTERN.matcher(sql);
//            if (matcher.find()) {
//                query.setQueryType(QueryType.USE_WORKBOOK);
//                query.setWorkbookName(matcher.group(1));
//                return query;
//            }
//
//            // Handle other simple commands
//            if (sql.toUpperCase().startsWith("SHOW WORKBOOKS")) {
//                query.setQueryType(QueryType.SHOW_WORKBOOKS);
//                return query;
//            }
//
//            if (sql.toUpperCase().startsWith("SHOW SHEETS")) {
//                query.setQueryType(QueryType.SHOW_SHEETS);
//                return query;
//            }
//
//            throw new ParseException("Unsupported SQL statement: " + sql);
//
//        } catch (Exception e) {
//            throw new ParseException("Failed to parse SQL: " + sql, e);
//        }
//    }
//
//    private List<String> parseColumns(String columnsStr) {
//        if ("*".equals(columnsStr.trim())) {
//            return Arrays.asList("*");
//        }
//
//        List<String> columns = new ArrayList<>();
//        String[] parts = columnsStr.split(",");
//        for (String part : parts) {
//            columns.add(part.trim());
//        }
//        return columns;
//    }
//
//    private Map<String, Object> parseColumnDefinitions(String columnDefs) {
//        Map<String, Object> definitions = new LinkedHashMap<>();
//        String cleanDefs = columnDefs.substring(1, columnDefs.length() - 1); // Remove parentheses
//
//        String[] parts = cleanDefs.split(",");
//        for (String part : parts) {
//            String[] columnDef = part.trim().split("\\s+");
//            if (columnDef.length >= 2) {
//                String columnName = columnDef[0];
//                String columnType = columnDef[1];
//                definitions.put(columnName, columnType);
//            }
//        }
//        return definitions;
//    }
//
//    private Map<String, Object> parseWhereConditions(String whereClause) {
//        Map<String, Object> conditions = new HashMap<>();
//
//        // Simple parsing for conditions like: column = 'value' AND column2 = 123
//        String[] andParts = whereClause.split("\\s+AND\\s+");
//
//        for (String part : andParts) {
//            String[] equalsParts = part.split("\\s*=\\s*");
//            if (equalsParts.length == 2) {
//                String column = equalsParts[0].trim();
//                String value = equalsParts[1].trim();
//
//                // Remove quotes if present
//                if (value.startsWith("'") && value.endsWith("'")) {
//                    value = value.substring(1, value.length() - 1);
//                }
//
//                // Try to parse as number
//                try {
//                    if (value.contains(".")) {
//                        conditions.put(column, Double.parseDouble(value));
//                    } else {
//                        conditions.put(column, Long.parseLong(value));
//                    }
//                } catch (NumberFormatException e) {
//                    conditions.put(column, value);
//                }
//            }
//        }
//
//        return conditions;
//    }
//
//    private Map<String, Object> parseValues(String valuesStr, List<String> columns) {
//        Map<String, Object> values = new HashMap<>();
//
//        String[] valueParts = valuesStr.split(",");
//
//        if (columns != null && !columns.isEmpty()) {
//            for (int i = 0; i < valueParts.length && i < columns.size(); i++) {
//                String value = valueParts[i].trim();
//
//                // Remove quotes if present
//                if (value.startsWith("'") && value.endsWith("'")) {
//                    value = value.substring(1, value.length() - 1);
//                }
//
//                values.put(columns.get(i), parseValue(value));
//            }
//        } else {
//            for (int i = 0; i < valueParts.length; i++) {
//                String value = valueParts[i].trim();
//
//                // Remove quotes if present
//                if (value.startsWith("'") && value.endsWith("'")) {
//                    value = value.substring(1, value.length() - 1);
//                }
//
//                values.put("value" + i, parseValue(value));
//            }
//        }
//
//        return values;
//    }
//
//    private Map<String, Object> parseSetClause(String setClause) {
//        Map<String, Object> values = new HashMap<>();
//
//        String[] setParts = setClause.split(",");
//        for (String part : setParts) {
//            String[] equalsParts = part.split("\\s*=\\s*");
//            if (equalsParts.length == 2) {
//                String column = equalsParts[0].trim();
//                String value = equalsParts[1].trim();
//
//                // Remove quotes if present
//                if (value.startsWith("'") && value.endsWith("'")) {
//                    value = value.substring(1, value.length() - 1);
//                }
//
//                values.put(column, parseValue(value));
//            }
//        }
//
//        return values;
//    }
//
//    private Object parseValue(String value) {
//        if (value == null || "NULL".equalsIgnoreCase(value)) {
//            return null;
//        }
//
//        // Try to parse as number
//        try {
//            if (value.contains(".")) {
//                return Double.parseDouble(value);
//            } else {
//                return Long.parseLong(value);
//            }
//        } catch (NumberFormatException e) {
//            // Try boolean
//            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
//                return Boolean.parseBoolean(value);
//            }
//
//            // Default to string
//            return value;
//        }
//    }
//}