package com.excel.sql.engine.service;

import com.excel.sql.engine.model.dto.ExcelFileInfo;

import java.util.List;

/**
 * Excel存储服务接口
 * 用于管理Excel文件的存储路径和文件列表
 */
public interface ExcelStorageService {
    
    /**
     * 获取当前存储路径
     * 
     * @return 存储路径
     */
    String getCurrentStoragePath();
    
    /**
     * 更改存储路径
     * 
     * @param newPath 新的存储路径
     * @param migrateFiles 是否迁移现有文件
     * @return 是否成功
     */
    boolean changeStoragePath(String newPath, boolean migrateFiles);
    
    /**
     * 获取所有Excel文件列表
     * 
     * @return Excel文件信息列表
     */
    List<ExcelFileInfo> getAllExcelFiles();
    
    /**
     * 获取指定路径下的所有Excel文件列表
     * 
     * @param path 指定路径
     * @return Excel文件信息列表
     */
    List<ExcelFileInfo> getExcelFilesInPath(String path);
} 