package com.excelsql.engine.storage;

import com.excelsql.config.ExcelSQLConfig;
import com.excelsql.engine.cache.ExcelCacheManager;
import com.excelsql.util.ExcelUtils;
import com.excelsql.util.TypeConverter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private ExcelSQLConfig config;

    @Resource
    private ExcelCacheManager cacheManager;

    @Resource
    private TypeConverter typeConverter;

    private Path getWorkbookPath(String workbookName) {
        return Paths.get(config.getStorage().getBasePath(), workbookName);
    }

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
            try (Workbook workbook = new XSSFWorkbook()) {
                // Create a default sheet
                workbook.createSheet("Sheet1");

                try (FileOutputStream fos = new FileOutputStream(workbookPath.toFile())) {
                    workbook.write(fos);
                }
            }

            return true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create workbook: " + workbookName, e);
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
            cacheManager.evictWorkbook(workbookName);

            return true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete workbook: " + workbookName, e);
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
            throw new RuntimeException("Failed to list workbooks", e);
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
            throw new RuntimeException("Failed to create sheet: " + sheetName, e);
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
            throw new RuntimeException("Failed to check sheet existence: " + sheetName, e);
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

                return true;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete sheet: " + sheetName, e);
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
            throw new RuntimeException("Failed to list sheets for workbook: " + workbookName, e);
        }
    }

    @Override
    public List<Map<String, Object>> selectData(String workbookName, String sheetName,
                                                List<String> columns, Map<String, Object> conditions,
                                                String orderBy, String groupBy, Integer limit, Integer offset) {
        try {
            Path workbookPath = getWorkbookPath(workbookName);

            if (!Files.exists(workbookPath)) {
                throw new RuntimeException("Workbook not found: " + workbookName);
            }

            List<Map<String, Object>> result = new ArrayList<>();

            try (FileInputStream fis = new FileInputStream(workbookPath.toFile());
                 Workbook workbook = WorkbookFactory.create(fis)) {

                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    throw new RuntimeException("Sheet not found: " + sheetName);
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
                        // Filter columns
                        if (columns != null && !columns.contains("*")) {
                            Map<String, Object> filteredRow = new HashMap<>();
                            for (String column : columns) {
                                if (rowData.containsKey(column)) {
                                    filteredRow.put(column, rowData.get(column));
                                }
                            }
                            result.add(filteredRow);
                        } else {
                            result.add(rowData);
                        }
                    }
                }
            }

            // Apply ordering
            if (orderBy != null) {
                result = applySorting(result, orderBy);
            }

            // Apply pagination
            if (offset != null || limit != null) {
                result = applyPagination(result, offset != null ? offset : 0, limit);
            }

            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to select data from sheet: " + sheetName, e);
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
                        Object value = values.get("value" + i);
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

                return 1; // One row inserted
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to insert data into sheet: " + sheetName, e);
        }
    }

    @Override
    public int updateData(String workbookName, String sheetName,
                          Map<String, Object> values, Map<String, Object> conditions) {
        try {
            Path workbookPath = getWorkbookPath(workbookName);

            if (!Files.exists(workbookPath)) {
                throw new RuntimeException("Workbook not found: " + workbookName);
            }

            int updatedRows = 0;

            try (FileInputStream fis = new FileInputStream(workbookPath.toFile());
                 Workbook workbook = WorkbookFactory.create(fis)) {

                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    throw new RuntimeException("Sheet not found: " + sheetName);
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
                }
            }

            return updatedRows;
        } catch (IOException e) {
            throw new RuntimeException("Failed to update data in sheet: " + sheetName, e);
        }
    }

    @Override
    public int deleteData(String workbookName, String sheetName, Map<String, Object> conditions) {
        try {
            Path workbookPath = getWorkbookPath(workbookName);

            if (!Files.exists(workbookPath)) {
                throw new RuntimeException("Workbook not found: " + workbookName);
            }

            int deletedRows = 0;

            try (FileInputStream fis = new FileInputStream(workbookPath.toFile());
                 Workbook workbook = WorkbookFactory.create(fis)) {

                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    throw new RuntimeException("Sheet not found: " + sheetName);
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

                // Collect rows to delete (in reverse order to avoid index issues)
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

                // Shift remaining rows up
                if (deletedRows > 0) {
                    // Save workbook
                    try (FileOutputStream fos = new FileOutputStream(workbookPath.toFile())) {
                        workbook.write(fos);
                    }
                }
            }

            return deletedRows;
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete data from sheet: " + sheetName, e);
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

            // Get file timestamps
            metadata.setCreatedTime(
                    LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(workbookPath).toInstant(),
                            ZoneId.systemDefault()
                    )
            );
            metadata.setModifiedTime(
                    LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(workbookPath).toInstant(),
                            ZoneId.systemDefault()
                    )
            );

            // Determine format
            metadata.setFormat(workbookName.endsWith(".csv") ? "csv" : "xlsx");

            // Get sheet names
            metadata.setSheetNames(listSheets(workbookName));

            return metadata;
        } catch (IOException e) {
            throw new RuntimeException("Failed to get workbook metadata: " + workbookName, e);
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

                // Get column information
                Row headerRow = sheet.getRow(0);
                if (headerRow != null) {
                    metadata.setColumnCount(headerRow.getLastCellNum());

                    List<String> columnNames = new ArrayList<>();
                    Map<String, String> columnTypes = new HashMap<>();

                    for (Cell cell : headerRow) {
                        String columnName = ExcelUtils.getCellValueAsString(cell);
                        columnNames.add(columnName);

                        // Infer column type from first data row
                        Row firstDataRow = sheet.getRow(1);
                        if (firstDataRow != null) {
                            Cell dataCell = firstDataRow.getCell(cell.getColumnIndex());
                            String type = inferColumnType(dataCell);
                            columnTypes.put(columnName, type);
                        } else {
                            columnTypes.put(columnName, "STRING");
                        }
                    }

                    metadata.setColumnNames(columnNames);
                    metadata.setColumnTypes(columnTypes);
                } else {
                    metadata.setColumnCount(0);
                    metadata.setColumnNames(Collections.emptyList());
                    metadata.setColumnTypes(Collections.emptyMap());
                }

                // Set timestamps (using file modification time as approximation)
                LocalDateTime fileTime = LocalDateTime.ofInstant(
                        Files.getLastModifiedTime(workbookPath).toInstant(),
                        ZoneId.systemDefault()
                );
                metadata.setCreatedTime(fileTime);
                metadata.setModifiedTime(fileTime);

                return metadata;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to get sheet metadata: " + sheetName, e);
        }
    }

    // Helper methods
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
        // Simple sorting implementation - can be enhanced
        String[] parts = orderBy.split(" ");
        String columnName = parts[0];
        boolean ascending = parts.length == 1 || "ASC".equalsIgnoreCase(parts[1]);

        return data.stream()
                .sorted((a, b) -> {
                    Object valueA = a.get(columnName);
                    Object valueB = b.get(columnName);

                    if (valueA == null && valueB == null) return 0;
                    if (valueA == null) return ascending ? -1 : 1;
                    if (valueB == null) return ascending ? 1 : -1;

                    @SuppressWarnings("unchecked")
                    int result = ((Comparable<Object>) valueA).compareTo(valueB);
                    return ascending ? result : -result;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> applyPagination(List<Map<String, Object>> data, int offset, Integer limit) {
        int fromIndex = Math.max(0, offset);
        int toIndex = limit != null ? Math.min(data.size(), fromIndex + limit) : data.size();

        if (fromIndex >= data.size()) {
            return Collections.emptyList();
        }

        return data.subList(fromIndex, toIndex);
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
                    return "NUMERIC";
                }
            case BOOLEAN:
                return "BOOLEAN";
            case FORMULA:
                return "FORMULA";
            default:
                return "STRING";
        }
    }
}
