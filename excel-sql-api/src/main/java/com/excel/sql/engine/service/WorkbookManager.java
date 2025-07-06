package com.excel.sql.engine.service;

import com.excel.sql.engine.model.excel.ExcelWorkbook;

import java.util.Map;

/**
 * 工作簿管理器接口
 * 用于管理当前会话的工作簿
 */
public interface WorkbookManager {
    
    /**
     * 创建工作簿
     *
     * @param workbookName 工作簿名称
     * @param options 创建选项
     * @return 创建的工作簿
     */
    ExcelWorkbook createWorkbook(String workbookName, Map<String, Object> options);
    
    /**
     * 创建工作表
     *
     * @param sheetName 工作表名称
     * @param options 创建选项
     * @return 是否创建成功
     */
    boolean createSheet(String sheetName, Map<String, Object> options);
    
    /**
     * 使用工作簿
     *
     * @param workbookName 工作簿名称
     * @return 是否成功
     */
    boolean useWorkbook(String workbookName);
    
    /**
     * 删除工作簿
     *
     * @param workbookName 工作簿名称
     * @return 是否成功
     */
    boolean dropWorkbook(String workbookName);
    
    /**
     * 删除工作表
     *
     * @param sheetName 工作表名称
     * @return 是否成功
     */
    boolean dropSheet(String sheetName);
    
    /**
     * 获取当前工作簿
     *
     * @return 当前工作簿
     */
    ExcelWorkbook getCurrentWorkbook();
    
    /**
     * 获取当前工作簿名称
     *
     * @return 当前工作簿名称
     */
    String getCurrentWorkbookName();
} 