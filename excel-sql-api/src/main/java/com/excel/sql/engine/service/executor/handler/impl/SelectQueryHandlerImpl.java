package com.excel.sql.engine.service.executor.handler.impl;

import com.excel.sql.engine.model.dto.SqlQueryResult;
import com.excel.sql.engine.model.excel.*;
import com.excel.sql.engine.service.ExcelFileService;
import com.excel.sql.engine.service.executor.handler.SelectQueryHandler;
import com.excel.sql.engine.service.parser.ParsedSql;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/**
 * SELECT查询处理器实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SelectQueryHandlerImpl implements SelectQueryHandler {
    
    private final ExcelFileService excelFileService;
    
    @Value("${excel-sql.query.page-size:1000}")
    private int defaultPageSize;
    
    @Value("${excel-sql.query.use-cache:true}")
    private boolean useQueryCache;
    
    @Value("${excel-sql.query.cache-ttl-seconds:300}")
    private long cacheTtlSeconds;
    
    // 查询结果缓存
    private final Map<String, CachedQueryResult> queryCache = new ConcurrentHashMap<>();
    
    /**
     * 缓存的查询结果，包含过期时间和文件修改时间
     */
    private static class CachedQueryResult {
        private final SqlQueryResult result;
        private final long expiryTimeMillis;
        private final Map<String, FileTime> fileModificationTimes;
        
        public CachedQueryResult(SqlQueryResult result, long ttlSeconds, String... filePaths) {
            this.result = result;
            this.expiryTimeMillis = System.currentTimeMillis() + (ttlSeconds * 1000);
            this.fileModificationTimes = new HashMap<>();
            
            // 记录文件的修改时间
            for (String filePath : filePaths) {
                try {
                    Path path = Paths.get(filePath);
                    if (Files.exists(path)) {
                        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                        fileModificationTimes.put(filePath, attrs.lastModifiedTime());
                    }
                } catch (Exception e) {
                    log.warn("无法获取文件修改时间: {}", filePath, e);
                }
            }
        }
        
        /**
         * 检查缓存是否有效
         */
        public boolean isValid(String... filePaths) {
            // 检查是否过期
            if (System.currentTimeMillis() > expiryTimeMillis) {
                return false;
            }
            
            // 检查文件是否被修改
            for (String filePath : filePaths) {
                try {
                    Path path = Paths.get(filePath);
                    if (Files.exists(path)) {
                        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                        FileTime currentModTime = attrs.lastModifiedTime();
                        
                        // 如果文件修改时间不同，说明文件被修改过
                        FileTime cachedModTime = fileModificationTimes.get(filePath);
                        if (cachedModTime == null || !cachedModTime.equals(currentModTime)) {
                            return false;
                        }
                    } else if (fileModificationTimes.containsKey(filePath)) {
                        // 文件曾经存在但现在不存在了
                        return false;
                    }
                } catch (Exception e) {
                    log.warn("检查文件修改时间失败: {}", filePath, e);
                    return false;
                }
            }
            
            return true;
        }
        
        /**
         * 获取查询结果
         */
        public SqlQueryResult getResult() {
            return result;
        }
    }
    
    @Override
    public SqlQueryResult handle(ParsedSql parsedSql, String workbook) {
        return handle(parsedSql, workbook, 10000); // 默认最大行数
    }
    
    @Override
    public SqlQueryResult handle(ParsedSql parsedSql, String workbook, int maxRows) {
        long startTime = System.currentTimeMillis();
        
        // 生成缓存键
        String cacheKey = generateCacheKey(parsedSql, workbook, maxRows);
        
        // 获取工作簿文件路径
        String workbookPath = null;
        try {
            // 尝试将workbook转换为Long类型的ID
            Long workbookId = Long.parseLong(workbook);
            workbookPath = excelFileService.getWorkbookPathById(workbookId);
        } catch (NumberFormatException e) {
            // 如果转换失败，说明workbook不是ID而是名称，直接通过名称获取路径
            log.debug("workbook参数不是有效的ID，将作为工作簿名称处理");
        }
        
        if (workbookPath == null) {
            // 通过工作簿名称获取路径
            ExcelWorkbook excelWorkbook = excelFileService.loadWorkbook(workbook);
            if (excelWorkbook != null) {
                workbookPath = excelWorkbook.getFilePath();
            }
        }
        
        // 检查缓存
        if (useQueryCache && workbookPath != null && queryCache.containsKey(cacheKey)) {
            CachedQueryResult cachedResult = queryCache.get(cacheKey);
            if (cachedResult.isValid(workbookPath)) {
                log.info("使用查询缓存: {}", cacheKey);
                return cachedResult.getResult();
            } else {
                log.info("缓存已过期或文件已修改，重新执行查询: {}", cacheKey);
                queryCache.remove(cacheKey);
            }
        }
        
        try {
            // 检查工作簿是否存在
            if (!excelFileService.existsWorkbook(workbook)) {
                return SqlQueryResult.error("工作簿不存在: " + workbook);
            }
            
            // 获取目标表（工作表）
            if (parsedSql.getTargetTables() == null || parsedSql.getTargetTables().isEmpty()) {
                return SqlQueryResult.error("未指定查询表");
            }
            
            String sheetName = parsedSql.getTargetTables().get(0);
            
            // 检查工作表是否存在
            if (!excelFileService.existsSheet(workbook, sheetName)) {
                return SqlQueryResult.error("工作表不存在: " + sheetName);
            }
            
            // 获取工作表
            ExcelSheet sheet = excelFileService.getSheet(workbook, sheetName);
            
            if (workbookPath == null) {
                return SqlQueryResult.error("无法获取工作簿文件路径");
            }
            
            // 处理SELECT *
            List<String> selectedColumns = parsedSql.getSelectedColumns();
            if (selectedColumns == null || selectedColumns.isEmpty() || 
                (selectedColumns.size() == 1 && "*".equals(selectedColumns.get(0)))) {
                selectedColumns = sheet.getColumnNames();
            }
            
            // 构建结果列定义
            List<SqlQueryResult.ColumnDefinition> resultColumns = buildResultColumns(sheet, selectedColumns, parsedSql);
            
            // 确定是否需要分页加载
            boolean needsPaging = maxRows > defaultPageSize || parsedSql.getLimit() == null || parsedSql.getLimit() > defaultPageSize;
            List<Map<String, Object>> resultRows;
            
            if (needsPaging) {
                // 分页加载和处理
                resultRows = executePagedQuery(sheet, parsedSql, workbookPath, maxRows);
            } else {
                // 一次性加载所有数据
                if (sheet.getRows() == null || sheet.getRows().isEmpty()) {
                    boolean loaded = sheet.loadRows(workbookPath);
                    if (!loaded) {
                        return SqlQueryResult.error("加载工作表数据失败");
                    }
                }
                
                // 执行查询
                resultRows = executeQuery(sheet, parsedSql, maxRows);
            }
            
            // 创建查询结果
            long executionTime = System.currentTimeMillis() - startTime;
            SqlQueryResult result = SqlQueryResult.success(resultColumns, resultRows, executionTime);
            
            // 缓存查询结果
            if (useQueryCache && workbookPath != null) {
                queryCache.put(cacheKey, new CachedQueryResult(result, cacheTtlSeconds, workbookPath));
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("执行SELECT查询异常: {}", e.getMessage(), e);
            return SqlQueryResult.error("执行SELECT查询异常: " + e.getMessage());
        }
    }
    
    /**
     * 生成查询缓存键
     */
    private String generateCacheKey(ParsedSql parsedSql, String workbook, int maxRows) {
        StringBuilder sb = new StringBuilder();
        sb.append(workbook).append(":");
        
        if (parsedSql.getTargetTables() != null && !parsedSql.getTargetTables().isEmpty()) {
            sb.append(parsedSql.getTargetTables().get(0)).append(":");
        }
        
        if (parsedSql.getSelectedColumns() != null) {
            sb.append(String.join(",", parsedSql.getSelectedColumns())).append(":");
        }
        
        if (parsedSql.getWhereCondition() != null) {
            sb.append(parsedSql.getWhereCondition()).append(":");
        }
        
        if (parsedSql.getOrderByClauses() != null) {
            sb.append(parsedSql.getOrderByClauses().stream()
                    .map(c -> c.getColumn() + (c.isDescending() ? " DESC" : ""))
                    .collect(Collectors.joining(","))).append(":");
        }
        
        if (parsedSql.getLimit() != null) {
            sb.append("LIMIT:").append(parsedSql.getLimit()).append(":");
        }
        
        if (parsedSql.getOffset() != null) {
            sb.append("OFFSET:").append(parsedSql.getOffset()).append(":");
        }
        
        sb.append("MAX:").append(maxRows);
        
        return sb.toString();
    }
    
    /**
     * 执行分页查询
     */
    private List<Map<String, Object>> executePagedQuery(ExcelSheet sheet, ParsedSql parsedSql, String workbookPath, int maxRows) {
        // 如果有聚合函数，我们需要加载所有数据进行处理
        Map<String, String> aggregateFunctions = parsedSql.getAggregateFunctions();
        boolean hasAggregates = aggregateFunctions != null && !aggregateFunctions.isEmpty();
        
        if (hasAggregates) {
            // 加载所有数据
            boolean loaded = sheet.loadRows(workbookPath);
            if (!loaded) {
                log.error("加载数据失败");
                return Collections.emptyList();
            }
            
            // 使用常规查询处理聚合
            return executeQuery(sheet, parsedSql, maxRows);
        }
        
        // 确定分页参数
        int offset = parsedSql.getOffset() != null ? parsedSql.getOffset() : 0;
        int limit = parsedSql.getLimit() != null ? Math.min(parsedSql.getLimit(), maxRows) : maxRows;
        
        // 计算需要加载的页数
        int pageSize = defaultPageSize;
        int startPage = offset / pageSize;
        int totalPagesToLoad = (int) Math.ceil((double) (offset + limit) / pageSize);
        
        List<Map<String, Object>> allResults = new ArrayList<>();
        
        // 逐页加载和处理数据
        for (int page = startPage; page < totalPagesToLoad; page++) {
            int startRow = page * pageSize;
            
            // 加载当前页的数据
            boolean loaded = sheet.loadRows(workbookPath, startRow, pageSize);
            if (!loaded) {
                log.error("加载第 {} 页数据失败", page);
                continue;
            }
            
            // 处理当前页的数据
            List<ExcelRow> pageRows = sheet.getRows();
            if (pageRows == null || pageRows.isEmpty()) {
                break; // 没有更多数据了
            }
            
            // 应用WHERE条件过滤
            if (parsedSql.getWhereCondition() != null && !parsedSql.getWhereCondition().isEmpty()) {
                String whereCondition = resolveTableAliases(parsedSql.getWhereCondition(), parsedSql.getTableAliases());
                pageRows = filterRowsBySimpleCondition(pageRows, whereCondition);
            }
            
            // 计算当前页内的偏移量
            int pageOffset = Math.max(0, offset - startRow);
            int pageLimit = Math.min(pageSize - pageOffset, limit - allResults.size());
            
            // 如果当前页没有足够的行，跳过
            if (pageOffset >= pageRows.size()) {
                continue;
            }
            
            // 截取当前页的结果
            int pageEndIndex = Math.min(pageOffset + pageLimit, pageRows.size());
            List<ExcelRow> pageResultRows = pageRows.subList(pageOffset, pageEndIndex);
            
            // 转换为结果行并添加到总结果集
            List<String> selectedColumns = parsedSql.getSelectedColumns();
            if (selectedColumns == null || selectedColumns.isEmpty() || 
                (selectedColumns.size() == 1 && "*".equals(selectedColumns.get(0)))) {
                selectedColumns = sheet.getColumnNames();
            }
            
            Map<String, String> columnAliases = parsedSql.getColumnAliases();
            
            for (ExcelRow row : pageResultRows) {
                Map<String, Object> resultRow = new HashMap<>();
                
                for (String colName : selectedColumns) {
                    // 获取列别名
                    String outputName = columnAliases != null && columnAliases.containsKey(colName) 
                            ? columnAliases.get(colName) : colName;
                    
                    // 获取列值
                    Object value = row.getCellValue(colName);
                    resultRow.put(outputName, value);
                }
                
                allResults.add(resultRow);
            }
            
            // 如果已经收集了足够的行，停止加载
            if (allResults.size() >= limit) {
                break;
            }
        }
        
        // 应用ORDER BY（如果需要）
        if (parsedSql.getOrderByClauses() != null && !parsedSql.getOrderByClauses().isEmpty()) {
            // 注意：这里的排序是在内存中进行的，可能不适合大型数据集
            allResults = sortResultRows(allResults, parsedSql.getOrderByClauses());
        }
        
        return allResults;
    }
    
    /**
     * 排序结果行
     */
    private List<Map<String, Object>> sortResultRows(List<Map<String, Object>> rows, List<ParsedSql.OrderByClause> orderByClauses) {
        List<Map<String, Object>> sortedRows = new ArrayList<>(rows);
        
        sortedRows.sort((row1, row2) -> {
            for (ParsedSql.OrderByClause clause : orderByClauses) {
                String columnName = clause.getColumn();
                boolean descending = clause.isDescending();
                
                Object value1 = row1.get(columnName);
                Object value2 = row2.get(columnName);
                
                // 处理null值
                if (value1 == null && value2 == null) {
                    continue;
                }
                if (value1 == null) {
                    return descending ? 1 : -1;
                }
                if (value2 == null) {
                    return descending ? -1 : 1;
                }
                
                // 比较值
                if (value1 instanceof Comparable && value2 instanceof Comparable) {
                    try {
                        @SuppressWarnings("unchecked")
                        int result = ((Comparable<Object>) value1).compareTo(value2);
                        if (result != 0) {
                            return descending ? -result : result;
                        }
                    } catch (ClassCastException e) {
                        // 如果类型不兼容，尝试字符串比较
                        int result = value1.toString().compareTo(value2.toString());
                        if (result != 0) {
                            return descending ? -result : result;
                        }
                    }
                } else {
                    // 如果不是Comparable，使用toString比较
                    int result = value1.toString().compareTo(value2.toString());
                    if (result != 0) {
                        return descending ? -result : result;
                    }
                }
            }
            return 0;
        });
        
        return sortedRows;
    }
    
    /**
     * 构建结果列定义
     */
    private List<SqlQueryResult.ColumnDefinition> buildResultColumns(ExcelSheet sheet, List<String> selectedColumns, ParsedSql parsedSql) {
        List<SqlQueryResult.ColumnDefinition> resultColumns = new ArrayList<>();
        
        Map<String, String> columnAliases = parsedSql.getColumnAliases();
        Map<String, String> aggregateFunctions = parsedSql.getAggregateFunctions();
        
        for (String colName : selectedColumns) {
            // 获取列别名
            String displayName = columnAliases != null && columnAliases.containsKey(colName) 
                    ? columnAliases.get(colName) : colName;
            
            // 检查是否是聚合函数列
            boolean isAggregated = aggregateFunctions != null && aggregateFunctions.containsKey(colName);
            
            // 获取列定义
            ExcelColumn column = sheet.getColumn(colName);
            ExcelDataType dataType = ExcelDataType.AUTO;
            
            if (column != null) {
                dataType = column.getDataType();
            } else if (isAggregated) {
                // 根据聚合函数类型推断数据类型
                String functionName = aggregateFunctions.get(colName);
                dataType = inferAggregateDataType(functionName);
            }
            
            // 创建列定义
            SqlQueryResult.ColumnDefinition colDef = SqlQueryResult.ColumnDefinition.builder()
                    .name(colName)
                    .label(displayName)
                    .dataType(dataType)
                    .aggregated(isAggregated)
                    .build();
            
            resultColumns.add(colDef);
        }
        
        return resultColumns;
    }
    
    /**
     * 根据聚合函数类型推断数据类型
     */
    private ExcelDataType inferAggregateDataType(String functionName) {
        if (functionName == null) {
            return ExcelDataType.AUTO;
        }
        
        switch (functionName.toUpperCase()) {
            case "COUNT":
                return ExcelDataType.INTEGER;
            case "SUM":
            case "AVG":
                return ExcelDataType.DECIMAL;
            case "MAX":
            case "MIN":
                return ExcelDataType.AUTO; // 依赖于原始列的类型
            default:
                return ExcelDataType.AUTO;
        }
    }
    
    /**
     * 执行查询并返回结果行
     */
    private List<Map<String, Object>> executeQuery(ExcelSheet sheet, ParsedSql parsedSql, int maxRows) {
        List<ExcelRow> rows = sheet.getRows();
        
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 应用WHERE条件过滤
        if (parsedSql.getWhereCondition() != null && !parsedSql.getWhereCondition().isEmpty()) {
            // 处理表别名
            String whereCondition = resolveTableAliases(parsedSql.getWhereCondition(), parsedSql.getTableAliases());
            rows = filterRowsBySimpleCondition(rows, whereCondition);
        }
        
        // 应用GROUP BY（简化实现）
        boolean hasAggregates = parsedSql.getAggregateFunctions() != null && !parsedSql.getAggregateFunctions().isEmpty();
        if (parsedSql.getGroupByColumns() != null && !parsedSql.getGroupByColumns().isEmpty() || hasAggregates) {
            rows = groupRows(rows, parsedSql);
        }
        
        // 应用ORDER BY（简化实现）
        if (parsedSql.getOrderByClauses() != null && !parsedSql.getOrderByClauses().isEmpty()) {
            rows = sortRows(rows, parsedSql.getOrderByClauses());
        }
        
        // 应用LIMIT和OFFSET
        int offset = parsedSql.getOffset() != null ? parsedSql.getOffset() : 0;
        int limit = parsedSql.getLimit() != null ? Math.min(parsedSql.getLimit(), maxRows) : maxRows;
        
        // 截取结果集
        if (offset >= rows.size()) {
            return Collections.emptyList();
        }
        
        int endIndex = Math.min(offset + limit, rows.size());
        rows = rows.subList(offset, endIndex);
        
        // 转换为结果行
        List<String> selectedColumns = parsedSql.getSelectedColumns();
        if (selectedColumns == null || selectedColumns.isEmpty() || 
            (selectedColumns.size() == 1 && "*".equals(selectedColumns.get(0)))) {
            selectedColumns = sheet.getColumnNames();
        }

        // 处理聚合函数和列别名
        Map<String, String> columnAliases = parsedSql.getColumnAliases();
        Map<String, String> aggregateFunctions = parsedSql.getAggregateFunctions();
        
        List<Map<String, Object>> resultRows = new ArrayList<>();
        for (ExcelRow row : rows) {
            Map<String, Object> resultRow = new HashMap<>();
            
            for (String colName : selectedColumns) {
                // 确定输出的列名（使用别名）
                String outputName = columnAliases != null && columnAliases.containsKey(colName) 
                        ? columnAliases.get(colName) : colName;
                
                // 处理聚合函数列
                if (aggregateFunctions != null && aggregateFunctions.containsKey(colName)) {
                    // 聚合列的值已经在 groupRows 中计算好了
                    Object value = row.getCellValue(colName);
                    resultRow.put(outputName, value);
                } else if (colName.contains("(") && colName.contains(")")) {
                    // 处理其他函数列
                    Object value = row.getCellValue(colName);
                    resultRow.put(outputName, value);
                } else {
                    // 普通列
                    Object value = row.getCellValue(colName);
                    resultRow.put(outputName, value);
                }
            }
            
            resultRows.add(resultRow);
        }
        
        return resultRows;
    }
    
    /**
     * 解析表别名，将条件中的表别名替换为实际表名
     */
    private String resolveTableAliases(String condition, Map<String, String> tableAliases) {
        if (condition == null || tableAliases == null || tableAliases.isEmpty()) {
            return condition;
        }
        
        String result = condition;
        for (Map.Entry<String, String> entry : tableAliases.entrySet()) {
            String tableName = entry.getKey();
            String alias = entry.getValue();
            
            // 替换形如 "alias.column" 的模式为 "tableName.column"
            result = result.replaceAll(
                    "\\b" + alias + "\\.(\\w+)\\b", 
                    tableName + ".$1");
        }
        
        return result;
    }
    
    /**
     * 简单条件过滤（实际应该使用表达式解析器）
     */
    private List<ExcelRow> filterRowsBySimpleCondition(List<ExcelRow> rows, String condition) {
        // 这里只是一个简化示例，实际应该解析WHERE条件
        // 例如 "age > 30" 或 "name = 'John'"
        return rows.stream()
                .filter(row -> evaluateCondition(row, condition))
                .collect(Collectors.toList());
    }
    
    /**
     * 简单条件评估（实际应该使用表达式解析器）
     */
    private boolean evaluateCondition(ExcelRow row, String condition) {
        // 处理AND条件
        if (condition.contains(" AND ")) {
            String[] andConditions = condition.split(" AND ");
            return Arrays.stream(andConditions)
                    .allMatch(subCondition -> evaluateCondition(row, subCondition.trim()));
        }
        
        // 处理OR条件
        if (condition.contains(" OR ")) {
            String[] orConditions = condition.split(" OR ");
            return Arrays.stream(orConditions)
                    .anyMatch(subCondition -> evaluateCondition(row, subCondition.trim()));
        }

        // 处理各种比较运算符
        if (condition.contains(">=")) {
            return evaluateComparisonCondition(row, condition, ">=");
        } else if (condition.contains("<=")) {
            return evaluateComparisonCondition(row, condition, "<=");
        } else if (condition.contains("<>") || condition.contains("!=")) {
            String operator = condition.contains("<>") ? "<>" : "!=";
            return evaluateComparisonCondition(row, condition, operator);
        } else if (condition.contains(">")) {
            return evaluateComparisonCondition(row, condition, ">");
        } else if (condition.contains("<")) {
            return evaluateComparisonCondition(row, condition, "<");
        } else if (condition.contains("=")) {
            return evaluateComparisonCondition(row, condition, "=");
        } else if (condition.toLowerCase().contains(" like ")) {
            return evaluateLikeCondition(row, condition);
        } else if (condition.toLowerCase().contains(" in ")) {
            return evaluateInCondition(row, condition);
        }
        
        // 无法识别的条件，默认通过
        log.warn("无法识别的WHERE条件: {}", condition);
        return true;
    }
    
    /**
     * 评估比较条件
     */
    private boolean evaluateComparisonCondition(ExcelRow row, String condition, String operator) {
        String[] parts = condition.split(Pattern.quote(operator));
        if (parts.length != 2) {
            log.warn("无效的比较条件: {}", condition);
            return true;
        }
        
        String columnName = parts[0].trim();
        String expectedValueStr = parts[1].trim().replace("'", "").replace("\"", "");
        
        Object actualValue = row.getCellValue(columnName);
        if (actualValue == null) {
            // 对于NULL值的特殊处理
            return expectedValueStr.equalsIgnoreCase("null");
        }
        
        // 尝试进行数值比较
        if (actualValue instanceof Number && isNumeric(expectedValueStr)) {
            double actualDouble = ((Number) actualValue).doubleValue();
            double expectedDouble = Double.parseDouble(expectedValueStr);
            
            switch (operator) {
                case "=": return actualDouble == expectedDouble;
                case "<>":
                case "!=": return actualDouble != expectedDouble;
                case ">": return actualDouble > expectedDouble;
                case "<": return actualDouble < expectedDouble;
                case ">=": return actualDouble >= expectedDouble;
                case "<=": return actualDouble <= expectedDouble;
                default: return false;
            }
        }
        
        // 字符串比较
        String actualStr = actualValue.toString();
        switch (operator) {
            case "=": return actualStr.equals(expectedValueStr);
            case "<>":
            case "!=": return !actualStr.equals(expectedValueStr);
            case ">": return actualStr.compareTo(expectedValueStr) > 0;
            case "<": return actualStr.compareTo(expectedValueStr) < 0;
            case ">=": return actualStr.compareTo(expectedValueStr) >= 0;
            case "<=": return actualStr.compareTo(expectedValueStr) <= 0;
            default: return false;
        }
    }
    
    /**
     * 评估LIKE条件
     */
    private boolean evaluateLikeCondition(ExcelRow row, String condition) {
        String[] parts = condition.toLowerCase().split(" like ");
        if (parts.length != 2) {
            log.warn("无效的LIKE条件: {}", condition);
            return true;
        }
        
        String columnName = parts[0].trim();
        String pattern = parts[1].trim().replace("'", "").replace("\"", "");
        
        Object actualValue = row.getCellValue(columnName);
        if (actualValue == null) {
            return false;
        }
        
        String actualStr = actualValue.toString();
        // 将SQL LIKE模式转换为Java正则表达式
        pattern = pattern.replace("%", ".*").replace("_", ".");
        
        return actualStr.matches(pattern);
    }
    
    /**
     * 评估IN条件
     */
    private boolean evaluateInCondition(ExcelRow row, String condition) {
        String[] parts = condition.toLowerCase().split(" in ");
        if (parts.length != 2) {
            log.warn("无效的IN条件: {}", condition);
            return true;
        }
        
        String columnName = parts[0].trim();
        String valuesStr = parts[1].trim();
        
        // 提取括号中的值列表
        if (!valuesStr.startsWith("(") || !valuesStr.endsWith(")")) {
            log.warn("IN条件格式错误: {}", condition);
            return true;
        }
        
        // 移除括号并分割值
        valuesStr = valuesStr.substring(1, valuesStr.length() - 1);
        List<String> values = Arrays.stream(valuesStr.split(","))
                .map(v -> v.trim().replace("'", "").replace("\"", ""))
                .collect(Collectors.toList());
        
        Object actualValue = row.getCellValue(columnName);
        if (actualValue == null) {
            return values.contains("null");
        }
        
        return values.contains(actualValue.toString());
    }
    
    /**
     * 检查字符串是否为数值
     */
    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 简单分组，不计算聚合函数，每组返回第一行
     */
    private List<ExcelRow> simpleGroupBy(List<ExcelRow> rows, List<String> groupByColumns) {
        Map<String, ExcelRow> groups = new HashMap<>();
        
        for (ExcelRow row : rows) {
            // 构建分组键
            StringBuilder keyBuilder = new StringBuilder();
            for (String groupByColumn : groupByColumns) {
                Object value = row.getCellValue(groupByColumn);
                keyBuilder.append(value == null ? "null" : value.toString()).append(":");
            }
            String groupKey = keyBuilder.toString();
            
            // 保留每组的第一行
            groups.putIfAbsent(groupKey, row);
        }
        
        return new ArrayList<>(groups.values());
    }
    
    /**
     * 简单排序实现
     */
    private List<ExcelRow> sortRows(List<ExcelRow> rows, List<ParsedSql.OrderByClause> orderByClauses) {
        // 创建一个新的列表，避免修改原始列表
        List<ExcelRow> sortedRows = new ArrayList<>(rows);
        
        // 按照ORDER BY子句排序
        sortedRows.sort((row1, row2) -> {
            for (ParsedSql.OrderByClause clause : orderByClauses) {
                String columnName = clause.getColumn();
                boolean descending = clause.isDescending();
                
                Object value1 = row1.getCellValue(columnName);
                Object value2 = row2.getCellValue(columnName);
                
                // 处理null值
                if (value1 == null && value2 == null) {
                    continue;
                }
                if (value1 == null) {
                    return descending ? 1 : -1;
                }
                if (value2 == null) {
                    return descending ? -1 : 1;
                }
                
                // 比较值
                if (value1 instanceof Comparable && value2 instanceof Comparable) {
                    @SuppressWarnings("unchecked")
                    int result = ((Comparable<Object>) value1).compareTo(value2);
                    if (result != 0) {
                        return descending ? -result : result;
                    }
                }
            }
            return 0;
        });
        
        return sortedRows;
    }
    
    /**
     * 清除查询缓存
     */
    public void clearCache() {
        queryCache.clear();
        log.info("查询缓存已清除");
    }
    
    /**
     * 清除特定工作簿的查询缓存
     */
    public void clearCache(String workbook) {
        queryCache.keySet().removeIf(key -> key.startsWith(workbook + ":"));
        log.info("工作簿 {} 的查询缓存已清除", workbook);
    }
    
    /**
     * 文件修改后清除相关缓存
     */
    public void clearCacheByFilePath(String filePath) {
        if (filePath == null) {
            return;
        }
        
        // 移除所有与该文件相关的缓存
        Set<String> keysToRemove = new HashSet<>();
        
        for (Map.Entry<String, CachedQueryResult> entry : queryCache.entrySet()) {
            CachedQueryResult cachedResult = entry.getValue();
            if (!cachedResult.isValid(filePath)) {
                keysToRemove.add(entry.getKey());
            }
        }
        
        for (String key : keysToRemove) {
            queryCache.remove(key);
        }
        
        if (!keysToRemove.isEmpty()) {
            log.info("已清除与文件 {} 相关的 {} 个缓存项", filePath, keysToRemove.size());
        }
    }
    
    /**
     * 简单分组实现（实际应该更复杂）
     */
    private List<ExcelRow> groupRows(List<ExcelRow> rows, ParsedSql parsedSql) {
        if (rows == null || rows.isEmpty()) {
            return rows;
        }
        
        List<String> groupByColumns = parsedSql.getGroupByColumns();
        Map<String, String> aggregateFunctions = parsedSql.getAggregateFunctions();
        
        // 如果没有GROUP BY但有聚合函数，则将所有行作为一组
        if ((groupByColumns == null || groupByColumns.isEmpty()) && aggregateFunctions != null && !aggregateFunctions.isEmpty()) {
            return aggregateAllRows(rows, parsedSql);
        }
        
        // 如果没有聚合函数，则简单分组返回第一行
        if (aggregateFunctions == null || aggregateFunctions.isEmpty()) {
            return simpleGroupBy(rows, groupByColumns);
        }
        
        // 按分组键进行分组
        Map<String, List<ExcelRow>> groups = new HashMap<>();
        
        for (ExcelRow row : rows) {
            // 构建分组键
            StringBuilder keyBuilder = new StringBuilder();
            for (String groupByColumn : groupByColumns) {
                Object value = row.getCellValue(groupByColumn);
                keyBuilder.append(value == null ? "null" : value.toString()).append(":");
            }
            String groupKey = keyBuilder.toString();
            
            // 将行添加到对应的分组
            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(row);
        }
        
        // 处理每个分组，计算聚合值
        List<ExcelRow> resultRows = new ArrayList<>();
        
        for (Map.Entry<String, List<ExcelRow>> entry : groups.entrySet()) {
            List<ExcelRow> group = entry.getValue();
            if (group.isEmpty()) {
                continue;
            }
            
            // 使用第一行作为基础
            ExcelRow baseRow = group.get(0);
            ExcelRow resultRow = new ExcelRow(baseRow.getRowNum(), baseRow.getSheet());
            
            // 复制分组列的值
            for (String groupByColumn : groupByColumns) {
                resultRow.setCellValue(groupByColumn, baseRow.getCellValue(groupByColumn));
            }
            
            // 计算聚合函数
            for (Map.Entry<String, String> aggEntry : aggregateFunctions.entrySet()) {
                String columnName = aggEntry.getKey();
                String functionName = aggEntry.getValue();
                
                // 提取真正的列名（如果是COUNT(*)这样的形式）
                String actualColumnName = columnName;
                if (columnName.contains("(") && columnName.contains(")")) {
                    int startIdx = columnName.indexOf("(") + 1;
                    int endIdx = columnName.indexOf(")");
                    if (startIdx < endIdx) {
                        actualColumnName = columnName.substring(startIdx, endIdx);
                    }
                }
                
                // 计算聚合值
                Object aggregateValue = calculateAggregateValue(group, actualColumnName, functionName);
                
                // 设置聚合值
                resultRow.setCellValue(columnName, aggregateValue);
            }
            
            resultRows.add(resultRow);
        }
        
        return resultRows;
    }
    
    /**
     * 对所有行进行聚合计算（没有GROUP BY的情况）
     */
    private List<ExcelRow> aggregateAllRows(List<ExcelRow> rows, ParsedSql parsedSql) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        
        Map<String, String> aggregateFunctions = parsedSql.getAggregateFunctions();
        if (aggregateFunctions == null || aggregateFunctions.isEmpty()) {
            return rows;
        }
        
        // 创建一个聚合结果行
        ExcelRow resultRow = new ExcelRow(0, rows.get(0).getSheet());
        
        // 计算聚合函数
        for (Map.Entry<String, String> aggEntry : aggregateFunctions.entrySet()) {
            String columnName = aggEntry.getKey();
            String functionName = aggEntry.getValue();
            
            // 提取真正的列名（如果是COUNT(*)这样的形式）
            String actualColumnName = columnName;
            if (columnName.contains("(") && columnName.contains(")")) {
                int startIdx = columnName.indexOf("(") + 1;
                int endIdx = columnName.indexOf(")");
                if (startIdx < endIdx) {
                    actualColumnName = columnName.substring(startIdx, endIdx);
                }
            }
            
            // 计算聚合值
            Object aggregateValue = calculateAggregateValue(rows, actualColumnName, functionName);
            
            // 设置聚合值
            resultRow.setCellValue(columnName, aggregateValue);
        }
        
        return Collections.singletonList(resultRow);
    }
    
    /**
     * 计算聚合函数值
     */
    private Object calculateAggregateValue(List<ExcelRow> rows, String columnName, String functionName) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        
        switch (functionName.toUpperCase()) {
            case "COUNT":
                // 特殊处理COUNT(*)
                if ("*".equals(columnName)) {
                    return (long) rows.size();
                }
                // 计算非空值的数量
                return rows.stream()
                        .map(row -> row.getCellValue(columnName))
                        .filter(Objects::nonNull)
                        .count();
                
            case "SUM":
                // 计算数值列的总和
                return rows.stream()
                        .map(row -> row.getCellValue(columnName))
                        .filter(Objects::nonNull)
                        .filter(value -> value instanceof Number)
                        .mapToDouble(value -> ((Number) value).doubleValue())
                        .sum();
                
            case "AVG":
                // 计算数值列的平均值
                return rows.stream()
                        .map(row -> row.getCellValue(columnName))
                        .filter(Objects::nonNull)
                        .filter(value -> value instanceof Number)
                        .mapToDouble(value -> ((Number) value).doubleValue())
                        .average()
                        .orElse(0.0);
                
            case "MAX":
                // 计算数值列的最大值
                return rows.stream()
                        .map(row -> row.getCellValue(columnName))
                        .filter(Objects::nonNull)
                        .filter(value -> value instanceof Comparable)
                        .max((o1, o2) -> {
                            try {
                                @SuppressWarnings("unchecked")
                                Comparable<Object> c1 = (Comparable<Object>) o1;
                                return c1.compareTo(o2);
                            } catch (ClassCastException e) {
                                // 如果无法直接比较，则转换为字符串比较
                                return o1.toString().compareTo(o2.toString());
                            }
                        })
                        .orElse(null);
                
            case "MIN":
                // 计算数值列的最小值
                return rows.stream()
                        .map(row -> row.getCellValue(columnName))
                        .filter(Objects::nonNull)
                        .filter(value -> value instanceof Comparable)
                        .min((o1, o2) -> {
                            try {
                                @SuppressWarnings("unchecked")
                                Comparable<Object> c1 = (Comparable<Object>) o1;
                                return c1.compareTo(o2);
                            } catch (ClassCastException e) {
                                // 如果无法直接比较，则转换为字符串比较
                                return o1.toString().compareTo(o2.toString());
                            }
                        })
                        .orElse(null);
                
            default:
                return null;
        }
    }
} 