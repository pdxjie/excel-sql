package com.excel.sql.engine.service;

import com.excel.sql.engine.model.dto.FileUploadRequest;
import com.excel.sql.engine.model.excel.ExcelWorkbook;
import com.excel.sql.engine.model.excel.ExcelSheet;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Excel文件服务接口
 */
public interface ExcelFileService {
    
    /**
     * 上传Excel文件
     *
     * @param file 文件
     * @param request 上传请求
     * @return 工作簿
     * @throws IOException IO异常
     */
    ExcelWorkbook uploadFile(MultipartFile file, FileUploadRequest request) throws IOException;
    
    /**
     * 加载工作簿
     *
     * @param workbookName 工作簿名称
     * @return 工作簿
     */
    ExcelWorkbook loadWorkbook(String workbookName);
    
    /**
     * 获取所有工作簿
     *
     * @return 工作簿列表
     */
    List<ExcelWorkbook> getAllWorkbooks();
    
    /**
     * 删除工作簿
     *
     * @param workbookName 工作簿名称
     * @return 是否成功
     */
    boolean deleteWorkbook(String workbookName);
    
    /**
     * 获取工作表
     *
     * @param workbookName 工作簿名称
     * @param sheetName 工作表名称
     * @return 工作表
     */
    ExcelSheet getSheet(String workbookName, String sheetName);
    
    /**
     * 检查工作簿是否存在
     *
     * @param workbookName 工作簿名称
     * @return 是否存在
     */
    boolean existsWorkbook(String workbookName);
    
    /**
     * 检查工作表是否存在
     *
     * @param workbookName 工作簿名称
     * @param sheetName 工作表名称
     * @return 是否存在
     */
    boolean existsSheet(String workbookName, String sheetName);
    
    /**
     * 获取基础路径
     *
     * @return 基础路径
     */
    String getBasePath();
    
    /**
     * 根据工作簿ID获取工作簿文件路径
     *
     * @param workbookId 工作簿ID
     * @return 工作簿文件路径
     */
    String getWorkbookPathById(Long workbookId);
} 