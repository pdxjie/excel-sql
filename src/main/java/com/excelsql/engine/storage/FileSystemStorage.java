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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    return 0;
                }

                List<String> headers = new ArrayList<>();
                for (Cell cell : headerRow) {
                    headers.add(ExcelUtils.getCellValueAsString(cell));
                }

                // Iterate in reverse to safely delete rows
                for (int i = sheet.getLastRowNum(); i > 0; i--) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    Map<String, Object> rowData = new HashMap<>();
                    for (int j = 0; j < headers.size(); j++) {
                        Cell cell = row.getCell(j);
                        Object value = ExcelUtils.getCellValue(cell);
                        rowData.put(headers.get(j), value);
                    }

                    if (matchesConditions(rowData, conditions)) {
                        removeRow(sheet, i);
                        deletedRows++;
                    }
                }

                // Save workbook if any deletion occurred
                if (deletedRows > 0) {
                    try (FileOutputStream fos = new FileOutputStream(workbookPath.toFile())) {
                        workbook.write(fos);
                    }
                }

                return deletedRows;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete data from sheet: " + sheetName, e);
        }
    }

    @Override
    public WorkbookMetadata getWorkbookMetadata(String workbookName) {
        return null;
    }

    @Override
    public SheetMetadata getSheetMetadata(String workbookName, String sheetName) {
        return null;
    }

    private boolean matchesConditions(Map<String, Object> rowData, Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            String key = entry.getKey();
            Object expected = entry.getValue();
            Object actual = rowData.get(key);

            // 支持 null 安全比较和字符串等价性
            if (expected == null) {
                if (actual != null) return false;
            } else {
                if (actual == null || !expected.toString().equalsIgnoreCase(actual.toString())) {
                    return false;
                }
            }
        }

        return true;
    }

    private void removeRow(Sheet sheet, int rowIndex) {
        int lastRowNum = sheet.getLastRowNum();
        if (rowIndex >= 0 && rowIndex < lastRowNum) {
            sheet.shiftRows(rowIndex + 1, lastRowNum, -1);
        } else if (rowIndex == lastRowNum) {
            Row removingRow = sheet.getRow(rowIndex);
            if (removingRow != null) {
                sheet.removeRow(removingRow);
            }
        }
    }

    private List<Map<String, Object>> applySorting(List<Map<String, Object>> data, String orderBy) {
        String[] parts = orderBy.trim().split("\\s+");
        if (parts.length == 0) return data;

        String sortKey = parts[0];
        boolean asc = parts.length < 2 || !"desc".equalsIgnoreCase(parts[1]);

        data.sort((a, b) -> {
            Object va = a.get(sortKey);
            Object vb = b.get(sortKey);

            if (va == null && vb == null) return 0;
            if (va == null) return asc ? -1 : 1;
            if (vb == null) return asc ? 1 : -1;

            if (va instanceof Comparable && vb instanceof Comparable) {
                return asc ? ((Comparable) va).compareTo(vb) : ((Comparable) vb).compareTo(va);
            }

            return asc ? va.toString().compareTo(vb.toString()) : vb.toString().compareTo(va.toString());
        });

        return data;
    }

    private List<Map<String, Object>> applyPagination(List<Map<String, Object>> data, int offset, Integer limit) {
        int start = Math.min(offset, data.size());
        int end = limit == null ? data.size() : Math.min(start + limit, data.size());
        return data.subList(start, end);
    }

    private Path getWorkbookPath(String workbookName) {
        String basePath = config.getStorage().getBasePath();
        if (workbookName == null || workbookName.trim().isEmpty()) {
            throw new IllegalArgumentException("Workbook name must not be null or empty");
        }

        // 默认添加扩展名（如果没有）
        if (!workbookName.endsWith(".xlsx") && !workbookName.endsWith(".csv")) {
            workbookName = workbookName + ".xlsx";
        }

        return Paths.get(basePath, workbookName);
    }


}
