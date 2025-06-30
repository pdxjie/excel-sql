package com.excelsql.engine.storage;

import java.util.List;
import java.util.Map;
/**
 * @Description: TODO::Need To Do
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:41
 */
public interface ExcelStorage {

    // Workbook operations
    boolean createWorkbook(String workbookName);
    boolean workbookExists(String workbookName);
    boolean deleteWorkbook(String workbookName);
    List<String> listWorkbooks();

    // Sheet operations
    boolean createSheet(String workbookName, String sheetName, Map<String, Object> columnDefinitions);
    boolean sheetExists(String workbookName, String sheetName);
    boolean deleteSheet(String workbookName, String sheetName);
    List<String> listSheets(String workbookName);

    // Data operations
    List<Map<String, Object>> selectData(String workbookName, String sheetName,
                                         List<String> columns, Map<String, Object> conditions,
                                         String orderBy, String groupBy, Integer limit, Integer offset);

    int insertData(String workbookName, String sheetName,
                   List<String> columns, Map<String, Object> values);

    int updateData(String workbookName, String sheetName,
                   Map<String, Object> values, Map<String, Object> conditions);

    int deleteData(String workbookName, String sheetName,
                   Map<String, Object> conditions);

    // Metadata operations
    WorkbookMetadata getWorkbookMetadata(String workbookName);
    SheetMetadata getSheetMetadata(String workbookName, String sheetName);
}
