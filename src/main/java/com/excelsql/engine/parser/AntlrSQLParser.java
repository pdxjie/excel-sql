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

    // 改进的 SELECT 正则表达式：支持 FROM table 和 FROM SHEET table 两种格式
    private static final Pattern SELECT_PATTERN =
            Pattern.compile("SELECT\\s+(.*?)\\s+FROM\\s+(?:SHEET\\s+)?" + IDENTIFIER_PATTERN +
                            "(?:\\s+WHERE\\s+(.*?))?(?:\\s+GROUP\\s+BY\\s+(.*?))?(?:\\s+HAVING\\s+(.*?))?(?:\\s+ORDER\\s+BY\\s+(.*?))?(?:\\s+LIMIT\\s+(\\d+)(?:\\s+OFFSET\\s+(\\d+))?)?$",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern INSERT_PATTERN =
            Pattern.compile("INSERT\\s+INTO\\s+(?:SHEET\\s+)?" + IDENTIFIER_PATTERN + "(?:\\s*\\(([^)]+)\\))?\\s+VALUES\\s*\\(([^)]+)\\)",
                    Pattern.CASE_INSENSITIVE);

    // 改进的 UPDATE 正则表达式
    private static final Pattern UPDATE_PATTERN =
            Pattern.compile("UPDATE\\s+(?:SHEET\\s+)?" + IDENTIFIER_PATTERN + "\\s+SET\\s+(.*?)(?:\\s+WHERE\\s+(.*))?$",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern DELETE_PATTERN =
            Pattern.compile("DELETE\\s+FROM\\s+(?:SHEET\\s+)?" + IDENTIFIER_PATTERN + "(?:\\s+WHERE\\s+(.*?))?$",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

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

        // 清理SQL，移除末尾的分号
        sql = sql.trim();
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }

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

            // SELECT - 重新实现更完整的解析
            matcher = SELECT_PATTERN.matcher(sql);
            if (matcher.find()) {
                query.setQueryType(QueryType.SELECT);

                // 解析 SELECT 列（第1组）
                String selectClause = matcher.group(1);
                query.setColumns(parseSelectColumns(selectClause));

                // 解析表名（第2-5组是标识符）
                query.setSheetName(extractIdentifier(matcher, 2));

                // WHERE子句（第6组）
                String whereClause = matcher.group(6);
                if (whereClause != null && !whereClause.trim().isEmpty()) {
                    query.setConditions(parseWhereConditions(whereClause.trim()));
                }

                // GROUP BY（第7组）
                String groupByClause = matcher.group(7);
                if (groupByClause != null && !groupByClause.trim().isEmpty()) {
                    query.setGroupBy(parseGroupByClause(groupByClause.trim()));
                }

                // HAVING（第8组）
                String havingClause = matcher.group(8);
                if (havingClause != null && !havingClause.trim().isEmpty()) {
                    query.setHaving(parseHavingClause(havingClause.trim()));
                }

                // ORDER BY（第9组）
                String orderByClause = matcher.group(9);
                if (orderByClause != null && !orderByClause.trim().isEmpty()) {
                    query.setOrderBy(parseOrderByClause(orderByClause.trim()));
                }

                // LIMIT（第10组）
                String limitClause = matcher.group(10);
                if (limitClause != null) {
                    query.setLimit(Integer.parseInt(limitClause));
                }

                // OFFSET（第11组）
                String offsetClause = matcher.group(11);
                if (offsetClause != null) {
                    query.setOffset(Integer.parseInt(offsetClause));
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

                // SET 子句在第5个组
                String setClause = matcher.group(5);
                if (setClause != null) {
                    query.setValues(parseSetClause(setClause.trim()));
                }

                // WHERE子句在第6个组
                String whereClause = matcher.group(6);
                if (whereClause != null) {
                    query.setConditions(parseWhereConditions(whereClause.trim()));
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

    /**
     * 解析 SELECT 列，返回 List 类型（符合 ParsedQuery 的 columns 字段类型）
     */
    private List<String> parseSelectColumns(String selectClause) {
        List<String> columns = new ArrayList<>();

        if ("*".equals(selectClause.trim())) {
            columns.add("*");
            return columns;
        }

        // 智能分割列，处理函数调用中的逗号
        List<String> columnExpressions = splitSelectColumns(selectClause);

        for (String expr : columnExpressions) {
            expr = expr.trim();
            if (expr.isEmpty()) {
                continue;
            }

            SelectColumn column = parseSelectColumn(expr);
            // 使用列的显示名称（别名或表达式）
            columns.add(column.getDisplayName());
        }

        return columns;
    }

    /**
     * 智能分割 SELECT 列，处理函数调用中的逗号和括号
     */
    private List<String> splitSelectColumns(String selectClause) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenthesesLevel = 0;
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < selectClause.length(); i++) {
            char c = selectClause.charAt(i);

            if (!inQuotes && (c == '\'' || c == '"')) {
                inQuotes = true;
                quoteChar = c;
                current.append(c);
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false;
                quoteChar = 0;
                current.append(c);
            } else if (!inQuotes && c == '(') {
                parenthesesLevel++;
                current.append(c);
            } else if (!inQuotes && c == ')') {
                parenthesesLevel--;
                current.append(c);
            } else if (!inQuotes && c == ',' && parenthesesLevel == 0) {
                // 这个逗号不在引号内且不在括号内，是分隔符
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        // 添加最后一个表达式
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        return result;
    }

    /**
     * 解析单个 SELECT 列表达式
     */
    private SelectColumn parseSelectColumn(String expression) {
        String alias = null;
        String columnExpr = expression;
        boolean isFunction = false;

        // 检查是否有 AS 别名
        Pattern asPattern = Pattern.compile("^(.+?)\\s+(?:AS\\s+)?" + IDENTIFIER_PATTERN + "$", Pattern.CASE_INSENSITIVE);
        Matcher asMatcher = asPattern.matcher(expression);

        if (asMatcher.find()) {
            columnExpr = asMatcher.group(1).trim();
            alias = extractIdentifier(asMatcher, 2);
        }

        // 检查是否为函数调用
        if (columnExpr.matches(".*\\w+\\s*\\(.*\\).*")) {
            isFunction = true;
        }

        return new SelectColumn(columnExpr, alias, isFunction);
    }

    /**
     * 解析 GROUP BY 子句，返回 String 类型
     */
    private String parseGroupByClause(String groupByClause) {
        return groupByClause; // 简单实现，可以扩展为解析多个字段
    }

    /**
     * 解析 HAVING 子句，返回 String 类型（符合 ParsedQuery 的 having 字段类型）
     */
    private String parseHavingClause(String havingClause) {
        return havingClause; // 简单实现，保持原始字符串
    }

    /**
     * 解析 ORDER BY 子句，返回 String 类型
     */
    private String parseOrderByClause(String orderByClause) {
        return orderByClause; // 简单实现，可以扩展为解析多个字段和排序方向
    }

    /**
     * SelectColumn 内部类，用于存储列信息
     */
    public static class SelectColumn {

        private String expression;

        private String alias;

        private boolean isFunction;

        public SelectColumn(String expression, String alias, boolean isFunction) {
            this.expression = expression;
            this.alias = alias;
            this.isFunction = isFunction;
        }

        public String getExpression() { return expression; }
        public String getAlias() { return alias; }
        public boolean isFunction() { return isFunction; }
        public String getDisplayName() { return alias != null ? alias : expression; }

        @Override
        public String toString() {
            return alias != null ? expression + " AS " + alias : expression;
        }
    }

    /**
     * 解析列名，返回 List 类型（符合 ParsedQuery 的 columns 字段类型）
     */
    private List<String> parseColumns(String columnsStr) {
        List<String> columns = new ArrayList<>();

        if ("*".equals(columnsStr.trim())) {
            columns.add("*");
            return columns;
        }

        String[] parts = columnsStr.split(",");
        for (String part : parts) {
            columns.add(part.trim());
        }
        return columns;
    }

    /**
     * 解析列定义，返回 Map 类型（符合 ParsedQuery 的 columnDefinitions 字段类型）
     */
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

    /**
     * 解析 WHERE 子句，返回 Map 类型（符合 ParsedQuery 的 conditions 字段类型）
     */
    private Map<String, Object> parseWhereConditions(String whereClause) {
        Map<String, Object> conditions = new HashMap<>();

        if (whereClause == null || whereClause.trim().isEmpty()) {
            return conditions;
        }

        // 使用智能分割，处理 AND 连接的条件
        List<String> conditionParts = splitConditionsByAnd(whereClause);

        for (String part : conditionParts) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }

            // 解析单个条件：column operator value
            parseCondition(part, conditions);
        }

        return conditions;
    }

    /**
     * 智能分割 WHERE 条件，处理 AND 关键字，同时考虑引号内的内容
     */
    private List<String> splitConditionsByAnd(String whereClause) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;

        String[] words = whereClause.split("\\s+");

        for (int i = 0; i < words.length; i++) {
            String word = words[i];

            // 检查是否进入或离开引号
            for (char c : word.toCharArray()) {
                if (!inQuotes && (c == '\'' || c == '"')) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (inQuotes && c == quoteChar) {
                    inQuotes = false;
                    quoteChar = 0;
                }
            }

            // 如果当前词是 AND 且不在引号内，则分割条件
            if ("AND".equalsIgnoreCase(word) && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                if (current.length() > 0) {
                    current.append(" ");
                }
                current.append(word);
            }
        }

        // 添加最后一个条件
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        return result;
    }

    /**
     * 解析单个条件
     */
    private void parseCondition(String condition, Map<String, Object> conditions) {
        // 支持的操作符：=, !=, <>, <, >, <=, >=, LIKE, IN
        String[] operators = {"<=", ">=", "!=", "<>", "=", "<", ">", "LIKE", "IN"};

        for (String op : operators) {
            String regex = "\\s+" + Pattern.quote(op) + "\\s+";
            if (op.equals("LIKE") || op.equals("IN")) {
                regex = "\\s+" + op + "\\s+";
            }

            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(condition);

            if (matcher.find()) {
                String column = condition.substring(0, matcher.start()).trim();
                String value = condition.substring(matcher.end()).trim();

                // 清理列名的引号
                column = cleanIdentifier(column);

                // 解析值
                Object parsedValue = parseConditionValue(value, op);

                // 存储条件（可以扩展为支持复杂条件对象）
                conditions.put(column, parsedValue);
                return;
            }
        }

        // 如果没有找到支持的操作符，抛出异常
        throw new ParseException("Unsupported condition format: " + condition);
    }

    /**
     * 清理标识符的引号
     */
    private String cleanIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }

        identifier = identifier.trim();

        if ((identifier.startsWith("`") && identifier.endsWith("`")) ||
                (identifier.startsWith("\"") && identifier.endsWith("\"")) ||
                (identifier.startsWith("'") && identifier.endsWith("'"))) {
            return identifier.substring(1, identifier.length() - 1);
        }

        return identifier;
    }

    /**
     * 解析条件值
     */
    private Object parseConditionValue(String value, String operator) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = value.trim();

        // 处理 IN 操作符的值列表
        if ("IN".equalsIgnoreCase(operator)) {
            if (value.startsWith("(") && value.endsWith(")")) {
                value = value.substring(1, value.length() - 1);
                String[] values = value.split(",");
                List<Object> list = new ArrayList<>();
                for (String v : values) {
                    list.add(parseValue(v.trim()));
                }
                return list;
            }
        }

        // 移除引号
        if ((value.startsWith("'") && value.endsWith("'")) ||
                (value.startsWith("\"") && value.endsWith("\""))) {
            value = value.substring(1, value.length() - 1);
        }

        return parseValue(value);
    }

    /**
     * 改进的 SET 子句解析，支持多个字段赋值，返回 Map 类型
     */
    private Map<String, Object> parseSetClause(String setClause) {
        Map<String, Object> values = new LinkedHashMap<>();

        if (setClause == null || setClause.trim().isEmpty()) {
            return values;
        }

        // 智能分割 SET 子句中的赋值表达式
        List<String> assignments = splitAssignments(setClause);

        for (String assignment : assignments) {
            assignment = assignment.trim();
            if (assignment.isEmpty()) {
                continue;
            }

            // 解析赋值：column = value
            int equalsIndex = assignment.indexOf('=');
            if (equalsIndex > 0) {
                String column = assignment.substring(0, equalsIndex).trim();
                String value = assignment.substring(equalsIndex + 1).trim();

                // 清理列名的引号
                column = cleanIdentifier(column);

                // 解析值
                Object parsedValue = parseAssignmentValue(value);
                values.put(column, parsedValue);
            }
        }

        return values;
    }

    /**
     * 智能分割赋值表达式，处理引号内的逗号
     */
    private List<String> splitAssignments(String setClause) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;
        int parenthesesLevel = 0;

        for (int i = 0; i < setClause.length(); i++) {
            char c = setClause.charAt(i);

            if (!inQuotes && (c == '\'' || c == '"')) {
                inQuotes = true;
                quoteChar = c;
                current.append(c);
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false;
                quoteChar = 0;
                current.append(c);
            } else if (!inQuotes && c == '(') {
                parenthesesLevel++;
                current.append(c);
            } else if (!inQuotes && c == ')') {
                parenthesesLevel--;
                current.append(c);
            } else if (!inQuotes && c == ',' && parenthesesLevel == 0) {
                // 这个逗号不在引号内且不在括号内，是分隔符
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        // 添加最后一个赋值
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        return result;
    }

    /**
     * 解析赋值的值
     */
    private Object parseAssignmentValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = value.trim();

        // 移除引号
        if ((value.startsWith("'") && value.endsWith("'")) ||
                (value.startsWith("\"") && value.endsWith("\""))) {
            value = value.substring(1, value.length() - 1);
        }

        return parseValue(value);
    }

    /**
     * 解析 VALUES 子句，返回 Map 类型（符合 ParsedQuery 的 values 字段类型）
     */
    private Map<String, Object> parseValues(String valuesStr, List<String> columns) {
        Map<String, Object> values = new HashMap<>();

        String[] valueParts = valuesStr.split(",");

        if (columns != null && !columns.isEmpty()) {
            for (int i = 0; i < valueParts.length && i < columns.size(); i++) {
                String value = valueParts[i].trim();

                // Remove quotes if present
                if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                } else if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                // 获取列名
                String columnName = columns.get(i);
                values.put(columnName, parseValue(value));
            }
        } else {
            for (int i = 0; i < valueParts.length; i++) {
                String value = valueParts[i].trim();

                // Remove quotes if present
                if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                } else if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                values.put("value" + i, parseValue(value));
            }
        }

        return values;
    }

    /**
     * 改进的值解析方法，支持更多数据类型
     */
    private Object parseValue(String value) {
        if (value == null || "NULL".equalsIgnoreCase(value)) {
            return null;
        }

        // 尝试解析为数字
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                // 尝试解析为长整型，如果超出范围则使用字符串
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException e) {
                    // 如果数字太大，返回字符串
                    return value;
                }
            }
        } catch (NumberFormatException e) {
            // 尝试解析为布尔值
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                return Boolean.parseBoolean(value);
            }

            // 默认返回字符串
            return value;
        }
    }
}