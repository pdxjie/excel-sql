package com.excelsql.engine.storage;

import com.excelsql.config.ExcelSQLConfig;
import com.excelsql.engine.cache.ExcelCacheManager;
import com.excelsql.exception.ExcelSQLException;
import com.excelsql.util.ExcelUtils;
import com.excelsql.util.TypeConverter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
/**
 * @Description: TODO::Need To Do
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:41
 */
@Component
public class FileSystemStorage implements ExcelStorage {

    @Resource
    private ExcelSQLConfig config;

    @Autowired
    private ExcelCacheManager excelCacheManager;

    @Resource
    private TypeConverter typeConverter;

    @Override
    public boolean createWorkbook(String workbookName) {
        try {
            Path workbookPath = getWorkbookPath(workbookName);

            if (Files.exists(workbookPath)) {
                return false; // Already exists
            }

            // Create directories if they don't exist
            Files.createDirectories(workbookPath.getParent());

            // Create new workbook
            try (Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
                // Create a default sheet
                workbook.createSheet("Sheet1");

                try (FileOutputStream fos = new FileOutputStream(workbookPath.toFile())) {
                    workbook.write(fos);
                }
            }

            return true;
        } catch (IOException e) {
            throw new ExcelSQLException("Failed to create workbook: " + workbookName, e);
        }
    }

    @Override
    public boolean workbookExists(String workbookName) {
        Path workbookPath = getWorkbookPath(workbookName);
        return Files.exists(workbookPath);
    }

    @Override
    public boolean deleteWorkbook(String workbookName) {
        try {
            Path workbookPath = getWorkbookPath(workbookName);

            if (!Files.exists(workbookPath)) {
                return false;
            }

            Files.delete(workbookPath);
            excelCacheManager.evictWorkbook(workbookName);

            return true;
        } catch (IOException e) {
            throw new ExcelSQLException("Failed to delete workbook: " + workbookName, e);
        }
    }

    @Override
    public List<String> listWorkbooks() {
        try {
            Path basePath = Paths.get(config.getStorage().getBasePath());

            if (!Files.exists(basePath)) {
                return Collections.emptyList();
            }

            return Files.list(basePath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".xlsx") || path.toString().endsWith(".csv"))
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new ExcelSQLException("Failed to list workbooks", e);
        }
    }

    @Override
    public boolean createSheet(String workbookName, String sheetName, Map<String, Object> columnDefinitions) {
        try {
            Path workbookPath = getWorkbookPath(workbookName);

            if (!Files.exists(workbookPath)) {
                createWorkbook(workbookName);
            }

            try (FileInputStream fis = new FileInputStream(workbookPath.toFile());
                 Workbook workbook = WorkbookFactory.create(fis)) {

                // Check if sheet already exists
                Sheet existingSheet = workbook.getSheet(sheetName);
                if (existingSheet != null) {
                    return false;
                }

                // Create new sheet
                Sheet sheet = workbook.createSheet(sheetName);

                // Create header row if column definitions provided
                if (columnDefinitions != null && !columnDefinitions.isEmpty()) {
                    Row headerRow = sheet.createRow(0);
                    int colIndex = 0;

                    for (String columnName : columnDefinitions.keySet()) {
                        Cell cell = headerRow.createCell(colIndex++);
                        cell.setCellValue(columnName);
                    }
                }

                // Save workbook
                try (FileOutputStream fos = new FileOutputStream(workbookPath.toFile())) {
                    workbook.write(fos);
                }

                return true;
            }
        } catch (IOException e) {
            throw new ExcelSQLException("Failed to create sheet: " + sheetName, e);
        }
    }

    @Override
    public boolean sheetExists(String workbookName, String sheetName) {
        try {
            Path workbookPath = getWorkbookPath(workbookName);

            if (!Files.exists(workbookPath)) {
                return false;
            }

            try (FileInputStream fis = new FileInputStream(workbookPath.toFile());
                 Workbook workbook = WorkbookFactory.create(fis)) {

                return workbook.getSheet(sheetName) != null;
            }
        } catch (IOException e) {
            throw new ExcelSQLException("Failed to check sheet existence: " + sheetName, e);
        }
    }

    @Override
    public boolean deleteSheet(String workbookName, String sheetName) {
        try {
            Path workbookPath = getWorkbookPath(workbookName);

            if (!Files.exists(workbookPath)) {
                return false;
            }

            try (FileInputStream fis = new FileInputStream(workbookPath.toFile());
                 Workbook workbook = WorkbookFactory.create(fis)) {

                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    return false;
                }

                int sheetIndex = workbook.getSheetIndex(sheet);
                workbook.removeSheetAt(sheetIndex);

                // Save workbook
                try (FileOutputStream fos = new FileOutputStream(workbookPath.toFile())) {
                    workbook.write(fos);
                }

                // Clear cache
                excelCacheManager.evictSheetData(workbookName, sheetName);

                return true;
            }
        } catch (IOException e) {
            throw new ExcelSQLException("Failed to delete sheet: " + sheetName, e);
        }
    }

    @Override
    public List<String> listSheets(String workbookName) {
        try {
            Path workbookPath = getWorkbookPath(workbookName);

            if (!Files.exists(workbookPath)) {
                return Collections.emptyList();
            }

            try (FileInputStream fis = new FileInputStream(workbookPath.toFile());
                 Workbook workbook = WorkbookFactory.create(fis)) {

                List<String> sheetNames = new ArrayList<>();
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    sheetNames.add(workbook.getSheetName(i));
                }

                return sheetNames;
            }
        } catch (IOException e) {
            throw new ExcelSQLException("Failed to list sheets for workbook: " + workbookName, e);
        }
    }

    @Override
    public List<Map<String, Object>> selectData(String workbookName, String sheetName,
                                                List<String> columns, Map<String, Object> conditions,
                                                String orderBy, String groupBy, Integer limit, Integer offset) {
        // Check cache first
        var cachedData = excelCacheManager.getSheetData(workbookName, sheetName);
        if (cachedData != null && conditions == null && orderBy == null && groupBy == null) {
            return applySelectFilters(cachedData.getData(), columns, limit, offset);
        }

        try {
            Path workbookPath = getWorkbookPath(workbookName);

            if (!Files.exists(workbookPath)) {
                throw new ExcelSQLException("Workbook not found: " + workbookName);
            }

            List<Map<String, Object>> result = new ArrayList<>();

            try (FileInputStream fis = new FileInputStream(workbookPath.toFile());
                 Workbook workbook = WorkbookFactory.create(fis)) {

                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    throw new ExcelSQLException("Sheet not found: " + sheetName);
                }

                // Get header row
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    return result;
                }

                List<String> headers = new ArrayList<>();
                for (Cell cell : headerRow) {
                    headers.add(ExcelUtils.getCellValueAsString(cell));
                }

                // Process data rows
                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue;

                    Map<String, Object> rowData = new HashMap<>();

                    for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
                        String columnName = headers.get(colIndex);
                        Cell cell = row.getCell(colIndex);
                        Object value = ExcelUtils.getCellValue(cell);
                        rowData.put(columnName, value);
                    }

                    // Apply conditions
                    if (matchesConditions(rowData, conditions)) {
                        result.add(rowData);
                    }
                }
            }

            // Cache the raw data if no complex operations
            if (conditions == null && orderBy == null && groupBy == null) {
                excelCacheManager.putSheetData(workbookName, sheetName, new ArrayList<>(result));
            }

            // Apply post-processing
            if (groupBy != null) {
                result = applyGroupBy(result, groupBy);
            }

            if (orderBy != null) {
                result = applySorting(result, orderBy);
            }

            result = applySelectFilters(result, columns, limit, offset);

            return result;
        } catch (IOException e) {
            throw new ExcelSQLException("Failed to select data from sheet: " + sheetName, e);
        }
    }

    @Override
    public int insertData(String workbookName, String sheetName,
                          List<String> columns, Map<String, Object> values) {
        try {
            Path workbookPath = getWorkbookPath(workbookName);

            if (!Files.exists(workbookPath)) {
                createWorkbook(workbookName);
            }

            try (FileInputStream fis = new FileInputStream(workbookPath.toFile());
                 Workbook workbook = WorkbookFactory.create(fis)) {

                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    sheet = workbook.createSheet(sheetName);
                }

                // Find next available row
                int nextRowIndex = sheet.getLastRowNum() + 1;
                if (sheet.getRow(0) == null) {
                    nextRowIndex = 0; // First row
                }

                Row newRow = sheet.createRow(nextRowIndex);

                // If this is the first row and columns are specified, create header
                if (nextRowIndex == 0 && columns != null) {
                    for (int i = 0; i < columns.size(); i++) {
                        Cell cell = newRow.createCell(i);
                        cell.setCellValue(columns.get(i));
                    }

                    // Create data row
                    nextRowIndex = 1;
                    newRow = sheet.createRow(nextRowIndex);
                }

                // Insert data
                if (columns != null) {
                    for (int i = 0; i < columns.size(); i++) {
                        String columnName = columns.get(i);
                        Object value = values.get(columnName);
                        Cell cell = newRow.createCell(i);
                        ExcelUtils.setCellValue(cell, value);
                    }
                } else {
                    // Insert values in order
                    int colIndex = 0;
                    for (Object value : values.values()) {
                        Cell cell = newRow.createCell(colIndex++);
                        ExcelUtils.setCellValue(cell, value);
                    }
                }

                // Save workbook
                try (FileOutputStream fos = new FileOutputStream(workbookPath.toFile())) {
                    workbook.write(fos);
                }

                // Clear cache
                excelCacheManager.evictSheetData(workbookName, sheetName);

                return 1; // One row inserted
            }
        } catch (IOException e) {
            throw new ExcelSQLException("Failed to insert data into sheet: " + sheetName, e);
        }
    }

    @Override
    public int updateData(String workbookName, String sheetName,
                          Map<String, Object> values, Map<String, Object> conditions) {
        try {
            Path workbookPath = getWorkbookPath(workbookName);

            if (!Files.exists(workbookPath)) {
                throw new ExcelSQLException("Workbook not found: " + workbookName);
            }

            int updatedRows = 0;

            try (FileInputStream fis = new FileInputStream(workbookPath.toFile());
                 Workbook workbook = WorkbookFactory.create(fis)) {

                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    throw new ExcelSQLException("Sheet not found: " + sheetName);
                }

                // Get header row
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    return 0;
                }

                List<String> headers = new ArrayList<>();
                Map<String, Integer> columnIndexMap = new HashMap<>();

                for (Cell cell : headerRow) {
                    String columnName = ExcelUtils.getCellValueAsString(cell);
                    headers.add(columnName);
                    columnIndexMap.put(columnName, cell.getColumnIndex());
                }

                // Update matching rows
                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue;

                    Map<String, Object> rowData = new HashMap<>();
                    for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
                        String columnName = headers.get(colIndex);
                        Cell cell = row.getCell(colIndex);
                        Object value = ExcelUtils.getCellValue(cell);
                        rowData.put(columnName, value);
                    }

                    if (matchesConditions(rowData, conditions)) {
                        // Update the row
                        for (Map.Entry<String, Object> entry : values.entrySet()) {
                            String columnName = entry.getKey();
                            Object newValue = entry.getValue();

                            Integer colIndex = columnIndexMap.get(columnName);
                            if (colIndex != null) {
                                Cell cell = row.getCell(colIndex);
                                if (cell == null) {
                                    cell = row.createCell(colIndex);
                                }
                                ExcelUtils.setCellValue(cell, newValue);
                            }
                        }
                        updatedRows++;
                    }
                }

                // Save workbook if any updates were made
                if (updatedRows > 0) {
                    try (FileOutputStream fos = new FileOutputStream(workbookPath.toFile())) {
                        workbook.write(fos);
                    }
                    // Clear cache
                    excelCacheManager.evictSheetData(workbookName, sheetName);
                }
            }

            return updatedRows;
        } catch (IOException e) {
            throw new ExcelSQLException("Failed to update data in sheet: " + sheetName, e);
        }
    }

    @Override
    public int deleteData(String workbookName, String sheetName, Map<String, Object> conditions) {
        try {
            Path workbookPath = getWorkbookPath(workbookName);

            if (!Files.exists(workbookPath)) {
                throw new ExcelSQLException("Workbook not found: " + workbookName);
            }

            int deletedRows = 0;

            try (FileInputStream fis = new FileInputStream(workbookPath.toFile());
                 Workbook workbook = WorkbookFactory.create(fis)) {

                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    throw new ExcelSQLException("Sheet not found: " + sheetName);
                }

                // Get header row
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    return 0;
                }

                List<String> headers = new ArrayList<>();
                for (Cell cell : headerRow) {
                    headers.add(ExcelUtils.getCellValueAsString(cell));
                }

                // Find rows to delete (in reverse order to avoid index issues)
                List<Integer> rowsToDelete = new ArrayList<>();

                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue;

                    Map<String, Object> rowData = new HashMap<>();
                    for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
                        String columnName = headers.get(colIndex);
                        Cell cell = row.getCell(colIndex);
                        Object value = ExcelUtils.getCellValue(cell);
                        rowData.put(columnName, value);
                    }

                    if (matchesConditions(rowData, conditions)) {
                        rowsToDelete.add(rowIndex);
                    }
                }

                // Delete rows in reverse order
                Collections.reverse(rowsToDelete);
                for (Integer rowIndex : rowsToDelete) {
                    Row row = sheet.getRow(rowIndex);
                    if (row != null) {
                        sheet.removeRow(row);
                        deletedRows++;
                    }
                }

                // Shift rows up to fill gaps
                if (!rowsToDelete.isEmpty()) {
                    int lastRowNum = sheet.getLastRowNum();
                    for (int rowIndex = 1; rowIndex <= lastRowNum; rowIndex++) {
                        if (sheet.getRow(rowIndex) == null) {
                            // Find next non-null row
                            int nextRowIndex = rowIndex + 1;
                            while (nextRowIndex <= lastRowNum && sheet.getRow(nextRowIndex) == null) {
                                nextRowIndex++;
                            }

                            if (nextRowIndex <= lastRowNum) {
                                // Move row up
                                Row sourceRow = sheet.getRow(nextRowIndex);
                                Row targetRow = sheet.createRow(rowIndex);
                                copyRow(sourceRow, targetRow);
                                sheet.removeRow(sourceRow);
                            }
                        }
                    }
                }

                // Save workbook if any deletions were made
                if (deletedRows > 0) {
                    try (FileOutputStream fos = new FileOutputStream(workbookPath.toFile())) {
                        workbook.write(fos);
                    }
                    // Clear cache
                    excelCacheManager.evictSheetData(workbookName, sheetName);
                }
            }

            return deletedRows;
        } catch (IOException e) {
            throw new ExcelSQLException("Failed to delete data from sheet: " + sheetName, e);
        }
    }

    @Override
    public WorkbookMetadata getWorkbookMetadata(String workbookName) {
        try {
            Path workbookPath = getWorkbookPath(workbookName);

            if (!Files.exists(workbookPath)) {
                return null;
            }

            WorkbookMetadata metadata = new WorkbookMetadata();
            metadata.setName(workbookName);
            metadata.setPath(workbookPath.toString());
            metadata.setSize(Files.size(workbookPath));

            var attrs = Files.readAttributes(workbookPath, "creationTime,lastModifiedTime");
            metadata.setCreated(LocalDateTime.ofInstant(
                    ((java.nio.file.attribute.FileTime) attrs.get("creationTime")).toInstant(),
                    ZoneId.systemDefault()));
            metadata.setLastModified(LocalDateTime.ofInstant(
                    ((java.nio.file.attribute.FileTime) attrs.get("lastModifiedTime")).toInstant(),
                    ZoneId.systemDefault()));

            metadata.setSheetNames(listSheets(workbookName));
            metadata.setFormat(workbookName.endsWith(".csv") ? "csv" : "xlsx");

            return metadata;
        } catch (IOException e) {
            throw new ExcelSQLException("Failed to get workbook metadata: " + workbookName, e);
        }
    }

    @Override
    public SheetMetadata getSheetMetadata(String workbookName, String sheetName) {
        try {
            Path workbookPath = getWorkbookPath(workbookName);

            if (!Files.exists(workbookPath)) {
                return null;
            }

            try (FileInputStream fis = new FileInputStream(workbookPath.toFile());
                 Workbook workbook = WorkbookFactory.create(fis)) {

                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    return null;
                }

                SheetMetadata metadata = new SheetMetadata();
                metadata.setName(sheetName);
                metadata.setWorkbookName(workbookName);
                metadata.setRowCount(sheet.getLastRowNum() + 1);

                // Get column info from header row
                Row headerRow = sheet.getRow(0);
                if (headerRow != null) {
                    metadata.setColumnCount(headerRow.getLastCellNum());
                    metadata.setHasHeader(true);

                    List<String> columnNames = new ArrayList<>();
                    Map<String, String> columnTypes = new HashMap<>();

                    for (Cell cell : headerRow) {
                        String columnName = ExcelUtils.getCellValueAsString(cell);
                        columnNames.add(columnName);

                        // Infer column type from first data row
                        if (sheet.getLastRowNum() > 0) {
                            Row dataRow = sheet.getRow(1);
                            if (dataRow != null) {
                                Cell dataCell = dataRow.getCell(cell.getColumnIndex());
                                String type = inferColumnType(dataCell);
                                columnTypes.put(columnName, type);
                            }
                        }
                    }

                    metadata.setColumnNames(columnNames);
                    metadata.setColumnTypes(columnTypes);
                } else {
                    metadata.setColumnCount(0);
                    metadata.setHasHeader(false);
                    metadata.setColumnNames(Collections.emptyList());
                    metadata.setColumnTypes(Collections.emptyMap());
                }

                var attrs = Files.readAttributes(workbookPath, "lastModifiedTime");
                metadata.setLastModified(LocalDateTime.ofInstant(
                        ((java.nio.file.attribute.FileTime) attrs.get("lastModifiedTime")).toInstant(),
                        ZoneId.systemDefault()));

                return metadata;
            }
        } catch (IOException e) {
            throw new ExcelSQLException("Failed to get sheet metadata: " + sheetName, e);
        }
    }

    // ========================= Helper Methods =========================

    private Path getWorkbookPath(String workbookName) {
        return Paths.get(config.getStorage().getBasePath(), workbookName);
    }

    private boolean matchesConditions(Map<String, Object> rowData, Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, Object> condition : conditions.entrySet()) {
            String columnName = condition.getKey();
            Object expectedValue = condition.getValue();
            Object actualValue = rowData.get(columnName);

            if (!Objects.equals(actualValue, expectedValue)) {
                return false;
            }
        }

        return true;
    }

    private List<Map<String, Object>> applySorting(List<Map<String, Object>> data, String orderBy) {
        if (orderBy == null || orderBy.trim().isEmpty()) {
            return data;
        }

        String[] parts = orderBy.trim().split("\\s+");
        String columnName = parts[0];
        boolean ascending = parts.length < 2 || !"DESC".equalsIgnoreCase(parts[1]);

        return data.stream()
                .sorted((a, b) -> {
                    Object valueA = a.get(columnName);
                    Object valueB = b.get(columnName);

                    if (valueA == null && valueB == null) return 0;
                    if (valueA == null) return ascending ? -1 : 1;
                    if (valueB == null) return ascending ? 1 : -1;

                    int comparison = 0;
                    if (valueA instanceof Comparable && valueB instanceof Comparable) {
                        try {
                            comparison = ((Comparable) valueA).compareTo(valueB);
                        } catch (ClassCastException e) {
                            comparison = valueA.toString().compareTo(valueB.toString());
                        }
                    } else {
                        comparison = valueA.toString().compareTo(valueB.toString());
                    }

                    return ascending ? comparison : -comparison;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> applySelectFilters(List<Map<String, Object>> data,
                                                         List<String> columns, Integer limit, Integer offset) {
        List<Map<String, Object>> result = data;

        // Apply column filtering
        if (columns != null && !columns.contains("*")) {
            result = result.stream()
                    .map(row -> {
                        Map<String, Object> filteredRow = new HashMap<>();
                        for (String column : columns) {
                            if (row.containsKey(column)) {
                                filteredRow.put(column, row.get(column));
                            }
                        }
                        return filteredRow;
                    })
                    .collect(Collectors.toList());
        }

        // Apply pagination
        if (offset != null || limit != null) {
            result = applyPagination(result, offset != null ? offset : 0, limit);
        }

        return result;
    }

    private List<Map<String, Object>> applyPagination(List<Map<String, Object>> data, int offset, Integer limit) {
        int start = Math.max(0, offset);
        int end = limit != null ? Math.min(data.size(), start + limit) : data.size();

        if (start >= data.size()) {
            return Collections.emptyList();
        }

        return data.subList(start, end);
    }

    private List<Map<String, Object>> applyGroupBy(List<Map<String, Object>> data, String groupBy) {
        // Simple group by implementation - groups by column value and counts
        Map<Object, List<Map<String, Object>>> groups = data.stream()
                .collect(Collectors.groupingBy(row -> row.get(groupBy)));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Object, List<Map<String, Object>>> entry : groups.entrySet()) {
            Map<String, Object> groupResult = new HashMap<>();
            groupResult.put(groupBy, entry.getKey());
            groupResult.put("COUNT(*)", entry.getValue().size());
            result.add(groupResult);
        }

        return result;
    }

    private void copyRow(Row sourceRow, Row targetRow) {
        for (Cell sourceCell : sourceRow) {
            Cell targetCell = targetRow.createCell(sourceCell.getColumnIndex());

            switch (sourceCell.getCellType()) {
                case STRING:
                    targetCell.setCellValue(sourceCell.getStringCellValue());
                    break;
                case NUMERIC:
                    targetCell.setCellValue(sourceCell.getNumericCellValue());
                    break;
                case BOOLEAN:
                    targetCell.setCellValue(sourceCell.getBooleanCellValue());
                    break;
                case FORMULA:
                    targetCell.setCellFormula(sourceCell.getCellFormula());
                    break;
                case BLANK:
                    targetCell.setBlank();
                    break;
                default:
                    break;
            }
        }
    }

    private String inferColumnType(Cell cell) {
        if (cell == null) {
            return "STRING";
        }

        switch (cell.getCellType()) {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return "DATE";
                } else {
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value)) {
                        return "INTEGER";
                    } else {
                        return "DECIMAL";
                    }
                }
            case BOOLEAN:
                return "BOOLEAN";
            case STRING:
            default:
                return "STRING";
        }
    }
}
