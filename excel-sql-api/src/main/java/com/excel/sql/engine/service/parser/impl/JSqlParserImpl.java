package com.excel.sql.engine.service.parser.impl;

import cn.hutool.core.collection.CollUtil;
import com.excel.sql.engine.model.dto.SqlQueryResult;
import com.excel.sql.engine.service.parser.ParsedSql;
import com.excel.sql.engine.service.parser.SqlParser;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * JSqlParser的SQL解析器实现
 */
@Slf4j
@Service("JSqlParserImpl")
public class JSqlParserImpl implements SqlParser {
    
    /**
     * SelectItemVisitor适配器
     */
    private abstract class SelectItemVisitorAdapter implements SelectItemVisitor {
        @Override
        public void visit(AllColumns allColumns) {
            // 默认实现为空
        }
        
        @Override
        public void visit(AllTableColumns allTableColumns) {
            // 默认实现为空
        }
        
        @Override
        public void visit(SelectExpressionItem item) {
            // 默认实现为空
        }
    }
    
    @Override
    public ParsedSql parse(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            SqlQueryResult.SqlType sqlType = getSqlType(sql);
            
            ParsedSql.ParsedSqlBuilder builder = ParsedSql.builder()
                    .originalSql(sql)
                    .sqlType(sqlType)
                    .success(true);
            
            switch (sqlType) {
                case SELECT:
                    return parseSelectStatement((Select) statement, builder);
                case INSERT:
                    return parseInsertStatement((Insert) statement, builder);
                case UPDATE:
                    return parseUpdateStatement((Update) statement, builder);
                case DELETE:
                    return parseDeleteStatement((Delete) statement, builder);
                default:
                    return builder
                            .success(false)
                            .errorMessage("不支持的SQL语句类型")
                            .build();
            }
        } catch (JSQLParserException e) {
            log.error("SQL解析错误: {}", e.getMessage());
            return ParsedSql.builder()
                    .originalSql(sql)
                    .success(false)
                    .errorMessage("SQL语法错误: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("SQL解析异常: {}", e.getMessage(), e);
            return ParsedSql.builder()
                    .originalSql(sql)
                    .success(false)
                    .errorMessage("SQL解析异常: " + e.getMessage())
                    .build();
        }
    }
    
    @Override
    public String validate(String sql) {
        try {
            CCJSqlParserUtil.parse(sql);
            return null; // 解析成功，没有错误
        } catch (JSQLParserException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "SQL验证异常: " + e.getMessage();
        }
    }
    
    @Override
    public SqlQueryResult.SqlType getSqlType(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                return SqlQueryResult.SqlType.SELECT;
            } else if (statement instanceof Insert) {
                return SqlQueryResult.SqlType.INSERT;
            } else if (statement instanceof Update) {
                return SqlQueryResult.SqlType.UPDATE;
            } else if (statement instanceof Delete) {
                return SqlQueryResult.SqlType.DELETE;
            } else {
                return SqlQueryResult.SqlType.UNKNOWN;
            }
        } catch (JSQLParserException e) {
            log.error("获取SQL类型错误: {}", e.getMessage());
            return SqlQueryResult.SqlType.UNKNOWN;
        }
    }
    
    /**
     * 解析SELECT语句
     */
    private ParsedSql parseSelectStatement(Select select, ParsedSql.ParsedSqlBuilder builder) {
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        
        // 解析目标表
        List<String> targetTables = new ArrayList<>();
        Map<String, String> tableAliases = new HashMap<>();
        
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            targetTables.add(table.getName());
            
            if (table.getAlias() != null) {
                tableAliases.put(table.getName(), table.getAlias().getName());
            }
        }
        
        // 解析JOIN
        List<ParsedSql.JoinClause> joinClauses = new ArrayList<>();
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                FromItem rightItem = join.getRightItem();
                if (rightItem instanceof Table) {
                    Table rightTable = (Table) rightItem;
                    targetTables.add(rightTable.getName());
                    
                    String rightTableAlias = null;
                    if (rightTable.getAlias() != null) {
                        rightTableAlias = rightTable.getAlias().getName();
                        tableAliases.put(rightTable.getName(), rightTableAlias);
                    }
                    
                    ParsedSql.JoinClause joinClause = ParsedSql.JoinClause.builder()
                            .joinType(getJoinType(join))
                            .rightTable(rightTable.getName())
                            .rightTableAlias(rightTableAlias)
                            .onCondition(join.getOnExpression() != null ? join.getOnExpression().toString() : null)
                            .build();
                    
                    joinClauses.add(joinClause);
                }
            }
        }
        
        // 解析选择的列
        final List<String> selectedColumns = new ArrayList<>();
        final Map<String, String> columnAliases = new HashMap<>();
        final Map<String, String> aggregateFunctions = new HashMap<>();
        
        // 使用访问者模式处理SELECT项目
        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            try {
                selectItem.accept(new SelectItemVisitorAdapter() {
                    @Override
                    public void visit(AllColumns allColumns) {
                        selectedColumns.add("*");
                    }
                    
                    @Override
                    public void visit(AllTableColumns allTableColumns) {
                        selectedColumns.add(allTableColumns.getTable().getName() + ".*");
                    }
                    
                    @Override
                    public void visit(SelectExpressionItem item) {
                        Expression expression = item.getExpression();
                        
                        if (expression instanceof Column) {
                            Column column = (Column) expression;
                            String columnName = column.getColumnName();
                            selectedColumns.add(columnName);
                            
                            if (item.getAlias() != null) {
                                columnAliases.put(columnName, item.getAlias().getName());
                            }
                        } else if (expression instanceof Function) {
                            Function function = (Function) expression;
                            String functionName = function.getName();
                            
                            // 获取别名或生成默认别名
                            String alias = item.getAlias() != null ? 
                                    item.getAlias().getName() : 
                                    function.toString();
                            
                            // 处理函数参数
                            ExpressionList parameters = function.getParameters();
                            String columnName;
                            String aggregateKey;
                            
                            if (function.isAllColumns() || (parameters == null || parameters.getExpressions().isEmpty())) {
                                // 处理无参数的函数，如 COUNT(*)
                                columnName = "*";
                                aggregateKey = functionName + "(" + columnName + ")";
                                
                                // 添加聚合函数映射
                                if (isAggregateFunction(functionName)) {
                                    aggregateFunctions.put(aggregateKey, functionName);
                                }
                                
                                selectedColumns.add(aggregateKey);
                                columnAliases.put(aggregateKey, alias);
                            } else {
                                // 处理有参数的函数
                                Expression paramExpr = parameters.getExpressions().get(0);
                                if (paramExpr instanceof Column) {
                                    Column paramColumn = (Column) paramExpr;
                                    columnName = paramColumn.getColumnName();
                                } else {
                                    columnName = paramExpr.toString();
                                }
                                
                                aggregateKey = functionName + "(" + columnName + ")";
                                
                                // 添加聚合函数映射
                                if (isAggregateFunction(functionName)) {
                                    aggregateFunctions.put(aggregateKey, functionName);
                                }
                                
                                selectedColumns.add(aggregateKey);
                                columnAliases.put(aggregateKey, alias);
                            }
                        } else {
                            // 处理其他类型的表达式
                            String expressionStr = expression.toString();
                            String alias = item.getAlias() != null ? 
                                    item.getAlias().getName() : 
                                    expressionStr;
                            
                            selectedColumns.add(expressionStr);
                            columnAliases.put(expressionStr, alias);
                        }
                    }
                });
            } catch (Exception e) {
                log.warn("解析SELECT项目异常: {}", e.getMessage());
                // 继续处理下一个项目
            }
        }
        
        // 解析WHERE条件
        String whereCondition = null;
        if (plainSelect.getWhere() != null) {
            whereCondition = plainSelect.getWhere().toString();
        }
        
        // 解析GROUP BY
        List<String> groupByColumns = new ArrayList<>();
        try {
            if (plainSelect.getGroupBy() != null) {
                List<Expression> groupByExpressions = plainSelect.getGroupBy().getGroupByExpressions();
                if (groupByExpressions != null) {
                    for (Expression groupByExpression : groupByExpressions) {
                        groupByColumns.add(groupByExpression.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析GROUP BY子句异常: {}", e.getMessage());
        }
        
        // 解析HAVING
        String havingCondition = null;
        if (plainSelect.getHaving() != null) {
            havingCondition = plainSelect.getHaving().toString();
        }
        
        // 解析ORDER BY
        List<ParsedSql.OrderByClause> orderByClauses = new ArrayList<>();
        try {
            if (plainSelect.getOrderByElements() != null) {
                for (OrderByElement orderByElement : plainSelect.getOrderByElements()) {
                    ParsedSql.OrderByClause orderByClause = ParsedSql.OrderByClause.builder()
                            .column(orderByElement.getExpression().toString())
                            .descending(!orderByElement.isAsc())
                            .build();
                    orderByClauses.add(orderByClause);
                }
            }
        } catch (Exception e) {
            log.warn("解析ORDER BY子句异常: {}", e.getMessage());
        }
        
        // 解析LIMIT和OFFSET
        Integer limit = null;
        Integer offset = null;
        try {
            if (plainSelect.getLimit() != null) {
                if (plainSelect.getLimit().getRowCount() != null) {
                    limit = Integer.parseInt(plainSelect.getLimit().getRowCount().toString());
                }
                
                if (plainSelect.getLimit().getOffset() != null) {
                    offset = Integer.parseInt(plainSelect.getLimit().getOffset().toString());
                }
            }
        } catch (Exception e) {
            log.warn("解析LIMIT/OFFSET子句异常: {}", e.getMessage());
        }
        
        // 构建解析结果
        return builder
                .targetTables(targetTables)
                .tableAliases(tableAliases)
                .selectedColumns(selectedColumns)
                .columnAliases(columnAliases)
                .whereCondition(whereCondition)
                .groupByColumns(groupByColumns)
                .havingCondition(havingCondition)
                .orderByClauses(orderByClauses)
                .limit(limit)
                .offset(offset)
                .joinClauses(joinClauses)
                .aggregateFunctions(aggregateFunctions)
                .build();
    }
    
    /**
     * 解析INSERT语句
     */
    private ParsedSql parseInsertStatement(Insert insert, ParsedSql.ParsedSqlBuilder builder) {
        // 获取目标表
        List<String> targetTables = Collections.singletonList(insert.getTable().getName());
        
        // 获取插入的列
        List<String> columns = new ArrayList<>();
        if (insert.getColumns() != null) {
            for (Column column : insert.getColumns()) {
                columns.add(column.getColumnName());
            }
        }
        
        // 获取插入的值
        List<Map<String, Object>> insertValues = new ArrayList<>();
        
        try {
            if (insert.getItemsList() != null) {
                // 处理 VALUES 子句
                if (insert.getItemsList() instanceof net.sf.jsqlparser.expression.operators.relational.ExpressionList) {
                    net.sf.jsqlparser.expression.operators.relational.ExpressionList expressionList = 
                            (net.sf.jsqlparser.expression.operators.relational.ExpressionList) insert.getItemsList();
                    
                    List<Expression> expressions = expressionList.getExpressions();
                    
                    if (expressions != null && !expressions.isEmpty()) {
                        Map<String, Object> rowValues = new HashMap<>();
                        
                        // 检查列和值的数量是否匹配
                        if (columns.isEmpty()) {
                            // 没有指定列，这里不再直接返回错误
                            // 而是标记这是一个没有指定列的INSERT语句，由处理器根据表结构自动处理
                            rowValues.put("__AUTO_COLUMNS__", true);
                            
                            // 将所有值按顺序存储，键名为"__VALUE_i__"
                            for (int i = 0; i < expressions.size(); i++) {
                                Expression valueExpr = expressions.get(i);
                                Object value = parseExpressionValue(valueExpr);
                                rowValues.put("__VALUE_" + i + "__", value);
                            }
                            
                            insertValues.add(rowValues);
                        } else if (columns.size() != expressions.size()) {
                            // 列和值的数量不匹配
                            return builder
                                    .success(false)
                                    .errorMessage("INSERT语句中列和值的数量不匹配：列数=" + columns.size() + "，值数=" + expressions.size())
                                    .build();
                        } else {
                            // 将值与列关联
                            for (int i = 0; i < columns.size(); i++) {
                                String columnName = columns.get(i);
                                Expression valueExpr = expressions.get(i);
                                Object value = parseExpressionValue(valueExpr);
                                rowValues.put(columnName, value);
                            }
                            
                            insertValues.add(rowValues);
                        }
                    }
                }
                // 处理 VALUES 多行插入
                else if (insert.getItemsList() instanceof net.sf.jsqlparser.expression.operators.relational.MultiExpressionList) {
                    net.sf.jsqlparser.expression.operators.relational.MultiExpressionList multiExprList = 
                            (net.sf.jsqlparser.expression.operators.relational.MultiExpressionList) insert.getItemsList();
                    
                    List<net.sf.jsqlparser.expression.operators.relational.ExpressionList> exprLists = multiExprList.getExpressionLists();
                    
                    for (net.sf.jsqlparser.expression.operators.relational.ExpressionList exprList : exprLists) {
                        List<Expression> expressions = exprList.getExpressions();
                        
                        if (expressions != null && !expressions.isEmpty()) {
                            Map<String, Object> rowValues = new HashMap<>();
                            
                            // 检查列和值的数量是否匹配
                            if (columns.isEmpty()) {
                                // 没有指定列，这里不再直接返回错误
                                // 而是标记这是一个没有指定列的INSERT语句，由处理器根据表结构自动处理
                                rowValues.put("__AUTO_COLUMNS__", true);
                                
                                // 将所有值按顺序存储，键名为"__VALUE_i__"
                                for (int i = 0; i < expressions.size(); i++) {
                                    Expression valueExpr = expressions.get(i);
                                    Object value = parseExpressionValue(valueExpr);
                                    rowValues.put("__VALUE_" + i + "__", value);
                                }
                                
                                insertValues.add(rowValues);
                            } else if (columns.size() != expressions.size()) {
                                // 列和值的数量不匹配
                                return builder
                                        .success(false)
                                        .errorMessage("INSERT语句中列和值的数量不匹配：列数=" + columns.size() + "，值数=" + expressions.size())
                                        .build();
                            } else {
                                // 将值与列关联
                                for (int i = 0; i < columns.size(); i++) {
                                    String columnName = columns.get(i);
                                    Expression valueExpr = expressions.get(i);
                                    Object value = parseExpressionValue(valueExpr);
                                    rowValues.put(columnName, value);
                                }
                                
                                insertValues.add(rowValues);
                            }
                        }
                    }
                }
                // 处理 SELECT 子查询
                else if (insert.getItemsList() instanceof net.sf.jsqlparser.statement.select.SubSelect) {
                    log.warn("不支持INSERT...SELECT语句");
                    return builder
                            .success(false)
                            .errorMessage("不支持INSERT...SELECT语句")
                            .build();
                }
            }
        } catch (Exception e) {
            log.error("解析INSERT值异常: {}", e.getMessage(), e);
            return builder
                    .success(false)
                    .errorMessage("解析INSERT值异常: " + e.getMessage())
                    .build();
        }
        
        // 构建解析结果
        return builder
                .targetTables(targetTables)
                .selectedColumns(columns)
                .insertValues(insertValues)
                .build();
    }
    
    /**
     * 解析表达式值
     */
    private Object parseExpressionValue(Expression expression) {
        if (expression == null) {
            return null;
        }
        
        String exprStr = expression.toString();
        
        // 移除字符串的引号
        if ((exprStr.startsWith("'") && exprStr.endsWith("'")) || 
            (exprStr.startsWith("\"") && exprStr.endsWith("\""))) {
            return exprStr.substring(1, exprStr.length() - 1);
        }
        
        // 尝试解析为数字
        try {
            if (exprStr.contains(".")) {
                return Double.parseDouble(exprStr);
            } else {
                return Integer.parseInt(exprStr);
            }
        } catch (NumberFormatException e) {
            // 不是数字，返回原始字符串
            return exprStr;
        }
    }
    
    /**
     * 解析UPDATE语句
     */
    private ParsedSql parseUpdateStatement(Update update, ParsedSql.ParsedSqlBuilder builder) {
        // 获取目标表
        List<String> targetTables = new ArrayList<>();
        targetTables.add(update.getTable().getName());
        
        // 获取更新的列
        List<String> columns = new ArrayList<>();
//        for (Column column : update.getColumns()) {
//            columns.add(column.getColumnName());
//        }
        for (UpdateSet updateSet : update.getUpdateSets()) {
            ArrayList<Column> updateColumns = updateSet.getColumns();
            if (CollUtil.isNotEmpty(updateColumns)) {
                final Column column = updateColumns.get(0);
                columns.add(column.getColumnName());
            }
        }
        
        // 获取更新的值
        Map<String, Object> updateValues = new HashMap<>();
        try {
            List<Expression> expressions = new ArrayList<>();

            for (UpdateSet updateSet : update.getUpdateSets()) {
                ArrayList<Expression> updateColumns = updateSet.getExpressions();
                if (CollUtil.isNotEmpty(updateColumns)) {
                    final Expression expression = updateColumns.get(0);
                    expressions.add(expression);
                }
            }
            
            // 检查列和值的数量是否匹配
            if (columns.size() != expressions.size()) {
                return builder
                        .success(false)
                        .errorMessage("UPDATE语句中列和值的数量不匹配：列数=" + columns.size() + "，值数=" + expressions.size())
                        .build();
            }
            
            // 将值与列关联 - 支持多个字段更新（逗号分隔）
            log.debug("处理UPDATE语句，更新 {} 个字段", columns.size());
            for (int i = 0; i < columns.size(); i++) {
                String columnName = columns.get(i);
                Expression valueExpr = expressions.get(i);
                
                log.debug("解析表达式: 列={}, 表达式类型={}, 表达式值={}", 
                        columnName, 
                        valueExpr.getClass().getName(), 
                        valueExpr.toString());
                
                // 检查是否是算术表达式
                if (valueExpr instanceof net.sf.jsqlparser.expression.operators.arithmetic.Addition ||
                    valueExpr instanceof net.sf.jsqlparser.expression.operators.arithmetic.Subtraction ||
                    valueExpr instanceof net.sf.jsqlparser.expression.operators.arithmetic.Multiplication ||
                    valueExpr instanceof net.sf.jsqlparser.expression.operators.arithmetic.Division) {
                    
                    // 将表达式转换为字符串形式，以便后续处理
                    String exprString = valueExpr.toString();
                    log.debug("解析到算术表达式: {} = {}", columnName, exprString);
                    updateValues.put(columnName, exprString);
                    
                    // 检查表达式是否涉及当前列（如 column = column + 5）
                    if (exprString.contains(columnName)) {
                        log.debug("表达式涉及当前列 {}: {}", columnName, exprString);
                    }
                } else {
                    // 普通值
                    Object value = parseExpressionValue(valueExpr);
                    log.debug("解析到普通值: {} = {} (类型: {})", 
                            columnName, 
                            value, 
                            (value != null ? value.getClass().getName() : "null"));
                    updateValues.put(columnName, value);
                }
            }
        } catch (Exception e) {
            log.error("解析UPDATE值异常: {}", e.getMessage(), e);
            return builder
                    .success(false)
                    .errorMessage("解析UPDATE值异常: " + e.getMessage())
                    .build();
        }
        
        // 获取WHERE条件
        String whereCondition = null;
        if (update.getWhere() != null) {
            whereCondition = update.getWhere().toString();
            log.debug("解析到WHERE条件: {}", whereCondition);
        }
        
        // 构建解析结果
        return builder
                .targetTables(targetTables)
                .selectedColumns(columns)
                .whereCondition(whereCondition)
                .updateValues(updateValues)
                .build();
    }
    
    /**
     * 解析DELETE语句
     */
    private ParsedSql parseDeleteStatement(Delete delete, ParsedSql.ParsedSqlBuilder builder) {
        // 获取目标表
        List<String> targetTables = Collections.singletonList(delete.getTable().getName());
        
        // 获取WHERE条件
        String whereCondition = null;
        if (delete.getWhere() != null) {
            whereCondition = delete.getWhere().toString();
        }
        
        // 构建解析结果
        return builder
                .targetTables(targetTables)
                .whereCondition(whereCondition)
                .build();
    }
    
    /**
     * 获取JOIN类型
     */
    private String getJoinType(Join join) {
        if (join.isInner()) {
            return "INNER";
        } else if (join.isLeft()) {
            return "LEFT";
        } else if (join.isRight()) {
            return "RIGHT";
        } else if (join.isFull()) {
            return "FULL";
        } else {
            return "INNER"; // 默认为INNER JOIN
        }
    }
    
    /**
     * 判断是否为聚合函数
     */
    private boolean isAggregateFunction(String functionName) {
        return "COUNT".equalsIgnoreCase(functionName) ||
                "SUM".equalsIgnoreCase(functionName) ||
                "AVG".equalsIgnoreCase(functionName) ||
                "MAX".equalsIgnoreCase(functionName) ||
                "MIN".equalsIgnoreCase(functionName);
    }
} 