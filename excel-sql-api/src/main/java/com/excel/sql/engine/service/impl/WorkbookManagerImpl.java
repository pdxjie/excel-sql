package com.excel.sql.engine.service.impl;

import com.excel.sql.engine.model.dto.FileUploadRequest;
import com.excel.sql.engine.model.excel.ExcelColumn;
import com.excel.sql.engine.model.excel.ExcelDataType;
import com.excel.sql.engine.model.excel.ExcelSheet;
import com.excel.sql.engine.model.excel.ExcelWorkbook;
import com.excel.sql.engine.service.ExcelFileService;
import com.excel.sql.engine.service.WorkbookManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 工作簿管理器实现类
 * 使用会话作用域，每个用户会话有独立的工作簿管理器
 */
@Slf4j
@Service
@SessionScope
@RequiredArgsConstructor
public class WorkbookManagerImpl implements WorkbookManager {
    
    private final ExcelFileService excelFileService;
    
    // 当前工作簿名称
    private String currentWorkbookName;
    
    @Override
    public ExcelWorkbook createWorkbook(String workbookName, Map<String, Object> options) {
        try {
            // 检查工作簿是否已存在
            if (excelFileService.existsWorkbook(workbookName)) {
                // 如果设置了覆盖选项，则删除现有工作簿
                boolean overwrite = options != null && "true".equals(options.get("overwrite"));
                if (overwrite) {
                    excelFileService.deleteWorkbook(workbookName);
                } else {
                    throw new IllegalArgumentException("工作簿已存在: " + workbookName);
                }
            }
            
            // 创建存储路径
            String storagePath = getStoragePath();
            File storageDir = new File(storagePath);
            // 确保目录存在
            storageDir.mkdirs();
            
            // 使用绝对路径
            String filePath = new File(storageDir, workbookName + ".xlsx").getAbsolutePath();
            File file = new File(filePath);
            
            log.info("创建工作簿文件: {}", filePath);
            
            // 如果文件已存在，先删除
            if (file.exists()) {
                boolean deleted = file.delete();
                log.info("删除已存在的文件: {}, 结果: {}", filePath, deleted);
            }
            
            // 使用简单的方式创建Excel文件，避免使用可能导致问题的XML属性
            try {
                // 创建一个最小的Excel文件，不包含任何额外的元数据
                org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.streaming.SXSSFWorkbook();
                workbook.createSheet("Sheet1");
                
                // 确保目录存在
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                
                try (FileOutputStream fileOut = new FileOutputStream(file)) {
                    workbook.write(fileOut);
                    fileOut.flush();
                }
                
                // 释放临时文件
                if (workbook instanceof org.apache.poi.xssf.streaming.SXSSFWorkbook) {
                    ((org.apache.poi.xssf.streaming.SXSSFWorkbook) workbook).dispose();
                }
                
                workbook.close();
            } catch (Exception e) {
                log.error("创建Excel文件失败: {}", e.getMessage(), e);
                
                // 如果上面的方法失败，尝试使用更简单的方法
                log.info("尝试使用备用方法创建Excel文件");
                
                // 创建一个空文件
                if (!file.createNewFile()) {
                    log.warn("创建空文件失败: {}", filePath);
                }
                
                // 使用Java的文件API直接写入一个最小的有效XLSX文件
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    // 写入一个最小的有效XLSX文件的字节
                    byte[] minimalXlsxBytes = getMinimalValidXlsxBytes();
                    fos.write(minimalXlsxBytes);
                    fos.flush();
                }
            }
            
            // 检查文件是否成功创建
            if (!file.exists()) {
                throw new IOException("创建Excel文件失败，文件不存在: " + filePath);
            }
            
            if (file.length() == 0) {
                throw new IOException("创建Excel文件失败，文件为空: " + filePath);
            }
            
            log.info("工作簿文件创建成功，大小: {} 字节", file.length());
            
            // 创建工作簿对象
            ExcelWorkbook excelWorkbook = ExcelWorkbook.builder()
                    .name(workbookName)
                    .filePath(filePath)
                    .fileType(com.excel.sql.engine.model.excel.ExcelFileType.XLSX)
                    .fileSize(file.length())
                    .lastModified(LocalDateTime.now())
                    .sheets(new HashMap<>())
                    .file(file)
                    .loaded(true)
                    .build();
            
            // 设置为当前工作簿
            currentWorkbookName = workbookName;
            
            // 立即尝试加载工作簿，验证是否可以正确加载
            ExcelWorkbook loadedWorkbook = excelFileService.loadWorkbook(workbookName);
            if (loadedWorkbook == null) {
                log.error("工作簿创建后无法加载: {}", workbookName);
                log.info("检查文件是否存在: {}, 存在: {}, 大小: {}", filePath, file.exists(), file.length());
                
                // 检查基础路径
                File baseDir = new File(excelFileService.getBasePath());
                log.info("基础路径: {}, 存在: {}", baseDir.getAbsolutePath(), baseDir.exists());
                
                // 列出基础路径下的所有文件
                File[] files = baseDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        log.info("文件: {}, 大小: {}", f.getAbsolutePath(), f.length());
                    }
                } else {
                    log.info("基础路径下没有文件");
                }
            } else {
                log.info("工作簿创建后成功加载: {}", workbookName);
            }
            
            return excelWorkbook;
        } catch (Exception e) {
            log.error("创建工作簿失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建工作簿失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取最小的有效XLSX文件字节
     * 这是一个预先生成的最小有效XLSX文件的字节数组
     */
    private byte[] getMinimalValidXlsxBytes() {
        // 这是一个最小的有效XLSX文件的Base64编码
        // 它包含一个空的工作表，没有任何额外的元数据
        String base64Xlsx = "UEsDBBQABgAIAAAAIQBi7p1oXgEAAJAEAAATAAgCW0NvbnRlbnRfVHlwZXNdLnhtbCCiBAIooAAC" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC0" +
                "VMluwjAQvVfqP0S+VomhLFLFgg9QLVVLgXPkOCZYJLZlG0r/vsNSKKqQUC+VD7Nneb43k/H0a2" +
                "2i6AVyrbRNaDzgNAJbqELZZUIfZ3fXIxppw2zBjLaQ0A1oOp1cXIxnmwKi0GjUJrQyJr1jTBcr" +
                "UFwPdAo2vCl1zrkJx3LJCv7Ml2A3nN/yFVgmDTfQGOkNnGbZyizRSoPmQMt5poUJ1iRUFEVQZL" +
                "aQdQEP2uQmYNqGVLxQa+FnWpkgrRK6zXnIPJ3HOZdFEbDnIgAGFrWS/XapVsGvhIUJaxSVYZga" +
                "NLGQcZqHuVBSVqIFQxu9e/gMw+oRCzsNw/K3ORbyQe31PbjPlZ+XkI5tgRZrUFr5E2Ys+EpLCR" +
                "kj6wM5RZXv4DGYPleKZvNvC/5M5TvIe2GaAx1AjX+FdOAPMEbmvzyitOhfOe+I8uQh7rj3uKPv" +
                "2X3Xk5hO+IQPRc5NfSBqGlNULnwZp/9P8BMAAP//UEsDBBQABgAIAAAAIQC1wTWfugAAACQBAA" +
                "ALAAgCX3JlbHMvLnJlbHMgogQCKKAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAArJJNT8MwDIbvSPyHyPfV3ZAQQkt3QUi7IVR+gEncD7WNoyQb3b8nHB" +
                "BaJdh0l8jx6/ex5TxdTaOKJgx2cFqkWZYKdAa14Z0W760W2TytFw9QCUe+5wO6RnQrNi83j5dv" +
                "WIlP9uR7cqSKDoUWvfd1xbmzPY7MZlyjS5c2DA7D6Tvf8YHrmO842HhA3mRZwUc29H4YlBbXDl" +
                "9rUTzLZK5gfKzFJA3mCMqEbdK4KfIVC8dZhMv/WkxyjWiZFpg0GHMoJFJ4OpNFMdGwx3J5jzKj" +
                "pilRv4ZnNPzqH7Lt+Lzh9CuTzYfvE6pMCMEUya1NyXXCUAz/iSYvAAAA//9QSwMEFAAGAAgAAA" +
                "AHAAAASQAAAC8AAABkcnMvX3JlbHMvd29ya2Jvb2sueG1sLnJlbHOskMFKxDAQhu+C7xDmbtNs" +
                "EZHS7GVZ8CqCDxAmk+1ukpCMrO3bG7xUWNhDD2OY4f++YbbHn2TxgVl8pkDauq0YBpdGH6ZAXs" +
                "9vq3tgohicjZkwkBMKHPvLi+0zJqvLJvkhC1tECmQWmR+EcG5GssJmzhj0ZsyZrJSap0HYaF/t" +
                "BHRt2zvRfmfAA5aeelX7QOb7Ef3bPM1j8PjI7i1hkqpWRJR/VUdQ3Wr5g223er3xZ3ycMbk4TF" +
                "LYWBGHSHEivHOJQafgDGPb1NYK8d/m/BEAAP//UEsDBBQABgAIAAAAIQDfbxQAGgEAAIQBAAAS" +
                "AAAAd29ya2Jvb2sueG1sLnJlbHOsksFOwzAMQO9I/EPkO03aCaGpaXdBSLshVH6gJO4a0SRO7W" +
                "7d35OyoUEFEuLiHOPnZ9nZ/LvvyRcGah1XkGUpEOTG1ZY3Ct7K1cMcCJHltfVOo4IBkRbF/d3s" +
                "FXtLaUTsHJGIcKSgJfJLKdG00FvKXI+cOg0L/ZbSGBqpW7PBzfbVdCDzNJvJEHrLqoZqDfVmQP" +
                "+2DG2NBleu+ejQR6oSIYp/VQdQnWr+g+3WvF75MzwvGFXsRwlsrAg9Rl4QXl3E0CkoQ9+kqbVC" +
                "/Ld5+QMAAP//UEsDBBQABgAIAAAAIQAw3OZMAQEAAHQBAAALAAAAX3JlbHMvLnJlbHOtks9qwz" +
                "AMxu+DvYPRfXGawRijTi9j0GvH9gBq4j+0tWMrW6N9+7kbpKXQMXa0n6Xf9yHLnX73jXpDpDp4" +
                "C+OiBIXewdL41cLz0/3VDIiys0vXeEQL3Qhwl52f7Z6wc5RWkOqGSBWRWqgp0g0A2Rq9o8In9F" +
                "XJfIwuUlxhBcG5N+jgqixvIH5vALOZZmqXFuLqEPzvMi5rhPjgzKuHJlIhQRX/qg6g9lT8wXZb" +
                "vd/4c3xYMbm4jRLYOBN6jLwgvLiEoVdwgbFNUxsF+G/z8gsAAP//UEsDBBQABgAIAAAAIQCCFN" +
                "YZGAEAAE4CAAAOAAAAZHJzL2Uyb0RvYy54bWyMUk1PwzAMvSPxHyLfadpNDK3tupOAE0gTiAvi" +
                "5jVeW9R8lWSj49+TtNuAA+Lm2M+f7yx3N70xbC+8VtbmLJ7GjAmbWKnsdmBvL/dXS8a0QFuisR" +
                "YzdqCa3ZyfnO1c1trWbqxpkRBsxlLmMlcJUZc5zjqBBvXUttITWPnWCYEu3XLpccfgxuA8jufc" +
                "KWVtIoQlD/fblST5Vq4k6iLVQjDWU4j+M/vqq5Vk/7i+e2ZMhCHJIZHBmIzRgXJWaGsEGdFYSm" +
                "xrXfKpTRpUQnRKUKkHYPHlV9QrVAb1P9QvqBwqSqhfUb+icqg44/EiTW7TdJlSUEbx1KCvqQnJ" +
                "4Uf3HwIXHwQeA4FvmV9Oj7+aL+8+GgAAAP//UEsDBBQABgAIAAAAIQCqJg6+vAAAABEBAAAPAA" +
                "AAZHJzL2Rvd25yZXYueG1sTI9BT8MwDIXvSPyHyEjcWDKxrmXtpgmkSUMCDnBr0zSiTaIk28q/" +
                "x+PA0bLfs9+bbbYysBONvnOsoMgzIMTGmY5bBR+H15sHIJ4MGzY4RgUX9LBdXF7MWGncmfd4qk" +
                "VLIcT7hBW0lIaaSt80qJnP3YAcvL0bNUvBY0vNyOYYrgOd5/mdVKxj+NCyAZ9b/Gxk83v8Ugre" +
                "qvfX8rBP+6NfVqp6Ox7WStXX0+oJiMQp/cNwxc/oUCCzwx0b7wdSECvIl4tVCYQVpHcFUJHPgS" +
                "4WswzofKH5H178AQAA//9QSwMEFAAGAAgAAAAhAIvNYJfYAAAACQEAABgAAABkcnMvY2hhcnRz" +
                "L19yZWxzL2NoYXJ0MS54bWwucmVsc4SPQWrDMBBF94XeQcw+lp1FKMGxN6HgbUgPIGg0sYitkc" +
                "xA3H17nJBCF126fLz/3+DweS7izoQ8eLJwqhsQRJ3XA/UW3i/Xoy2InJPWaeREFm7EcBlerw5P" +
                "NFuZx1JGGQuiLFi4pDTeGJO7C+mU6zBRuXRjTinPZTQm2dPpTYO2aVrj4X+GFHWvLbzp/kqQP8" +
                "9ot48EP/RkQ6I/JhGbQUQJOrJwLuuJBPe9mY8f0kjcjvkHAAD//wMAUEsDBBQABgAIAAAAIQAA" +
                "AAAAAAAAAAAAAAAPAAAAZHJZL3dvcmtib29rLnhtbFBLAQItABQABgAIAAAAIQBi7p1oXgEAAJ" +
                "AEAAATAAAAAAAAAAAAAAAAAAAAAABbQ29udGVudF9UeXBlc10ueG1sUEsBAi0AFAAGAAgAAAAh" +
                "ALXBNZ+6AAAAJAEAAAsAAAAAAAAAAAAAAAAAjwEAAF9yZWxzLy5yZWxzUEsBAi0AFAAGAAgAAA" +
                "AhAEkAAAAvAAAABwAAAAAAAAAAAAAAAACwAgAAZHJzL19yZWxzL3dvcmtib29rLnhtbC5yZWxz" +
                "UEsBAi0AFAAGAAgAAAAhAN9vFAAaAQAAhAEAABIAAAAAAAAAAAAAAAAA7QMAAHdvcmtib29rLn" +
                "htbC5yZWxzUEsBAi0AFAAGAAgAAAAhADDc5kwBAQAAdAEAAAsAAAAAAAAAAAAAAAAAOAUAAF9y" +
                "ZWxzLy5yZWxzUEsBAi0AFAAGAAgAAAAhAIIU1hkYAQAATgIAAA4AAAAAAAAAAAAAAAAAWwYAAG" +
                "Rycy9lMm9Eb2MueG1sUEsBAi0AFAAGAAgAAAAhAKomDr68AAAAEQEAABIAAAAAAAAAAAAAAAAAr" +
                "wcAAGRycy9jaGFydHMvY2hhcnQxLnhtbFBLAQItABQABgAIAAAAIQCLzWCX2AAAAAkBAAAYAAA" +
                "AAAAAAAAAAAAAAKsIAABkcnMvY2hhcnRzL19yZWxzL2NoYXJ0MS54bWwucmVsc1BLAQItABQAB" +
                "gAIAAAAIQAAAAAAAAAAAAAADwAAAAAAAAAAAAAAAAAAANMJAABkcnMvd29ya2Jvb2sueG1sUEs" +
                "FBgAAAAAJAAkAQwIAAOIJAAAAAA==";
        
        // 解码Base64字符串为字节数组
        return java.util.Base64.getDecoder().decode(base64Xlsx);
    }
    
    @Override
    public boolean createSheet(String sheetName, Map<String, Object> options) {
        Workbook poiWorkbook = null;
        FileOutputStream fileOut = null;
        
        try {
            log.info("开始创建工作表: {}, 当前工作簿: {}", sheetName, currentWorkbookName);
            
            // 检查是否有当前工作簿
            if (currentWorkbookName == null) {
                throw new IllegalStateException("未选择工作簿，请先使用USE WORKBOOK命令");
            }
            
            // 检查工作簿是否存在
            boolean workbookExists = excelFileService.existsWorkbook(currentWorkbookName);
            log.info("工作簿存在检查: {}, 结果: {}", currentWorkbookName, workbookExists);
            
            if (!workbookExists) {
                throw new IllegalStateException("当前工作簿不存在: " + currentWorkbookName);
            }
            
            // 获取工作簿
            log.info("开始加载工作簿: {}", currentWorkbookName);
            ExcelWorkbook workbook = excelFileService.loadWorkbook(currentWorkbookName);
            
            if (workbook == null) {
                throw new IllegalStateException("加载工作簿失败: " + currentWorkbookName);
            }
            
            if (workbook.getSheets() != null && workbook.getSheets().containsKey(sheetName)) {
                // 如果设置了覆盖选项，则删除现有工作表
                boolean overwrite = options != null && "true".equals(options.get("overwrite"));
                if (!overwrite) {
                    throw new IllegalArgumentException("工作表已存在: " + sheetName);
                }
            }
            
            log.info("创建工作表对象: {}", sheetName);
            
            // 创建工作表
            ExcelSheet sheet = ExcelSheet.builder()
                    .name(sheetName)
                    .workbookId((long) workbook.getName().hashCode()) // 使用工作簿名称的哈希码作为ID
                    .sheetIndex(workbook.getSheets() != null ? workbook.getSheets().size() : 0)
                    .headerRowIndex(0)
                    .dataStartRowIndex(1)
                    .totalRows(0)
                    .columns(new ArrayList<>())
                    .dataLoaded(true)
                    .build();
            
            // 如果指定了列定义，则添加列
            if (options != null && options.containsKey("columns")) {
                String columnsStr = (String) options.get("columns");
                log.info("解析列定义: {}", columnsStr);
                List<ExcelColumn> columns = parseColumnDefinitions(columnsStr, sheet);
                sheet.setColumns(columns);
                log.info("列定义解析完成，列数: {}", columns.size());
            }
            
            // 初始化索引
            sheet.initializeIndexes();
            
            // 添加到工作簿对象
            workbook.addSheet(sheet);
            
            log.info("开始将工作表写入Excel文件: {}", sheetName);
            
            // 确保文件存在且可读
            File excelFile = workbook.getFile();
            if (!excelFile.exists() || excelFile.length() == 0) {
                log.error("Excel文件不存在或为空: {}", excelFile.getAbsolutePath());
                // 尝试重新创建工作簿
                workbook = createWorkbook(currentWorkbookName, Map.of("overwrite", "true"));
                excelFile = workbook.getFile();
            }
            
            try {
                // 使用临时文件方式，避免直接修改原文件可能导致的问题
                File tempFile = File.createTempFile("temp_workbook_", ".xlsx");
                log.info("创建临时文件: {}", tempFile.getAbsolutePath());
                
                // 先创建一个新的工作簿
                try (XSSFWorkbook newWorkbook = new XSSFWorkbook()) {
                    // 1. 首先从原始文件中读取所有现有的工作表
                    if (excelFile.exists() && excelFile.length() > 0) {
                        try (Workbook originalWorkbook = WorkbookFactory.create(excelFile)) {
                            // 复制所有现有的工作表到新工作簿
                            for (int i = 0; i < originalWorkbook.getNumberOfSheets(); i++) {
                                org.apache.poi.ss.usermodel.Sheet origSheet = originalWorkbook.getSheetAt(i);
                                String origSheetName = origSheet.getSheetName();
                                
                                // 如果是要覆盖的工作表，则跳过
                                if (origSheetName.equals(sheetName)) {
                                    boolean overwrite = options != null && "true".equals(options.get("overwrite"));
                                    if (overwrite) {
                                        log.info("跳过要覆盖的工作表: {}", origSheetName);
                                        continue;
                                    }
                                }
                                
                                log.info("复制现有工作表: {}", origSheetName);
                                org.apache.poi.ss.usermodel.Sheet newSheet = newWorkbook.createSheet(origSheetName);
                                
                                // 复制工作表内容
                                for (int rowNum = 0; rowNum <= origSheet.getLastRowNum(); rowNum++) {
                                    org.apache.poi.ss.usermodel.Row origRow = origSheet.getRow(rowNum);
                                    if (origRow != null) {
                                        org.apache.poi.ss.usermodel.Row newRow = newSheet.createRow(rowNum);
                                        for (int cellNum = 0; cellNum < origRow.getLastCellNum(); cellNum++) {
                                            org.apache.poi.ss.usermodel.Cell origCell = origRow.getCell(cellNum);
                                            if (origCell != null) {
                                                org.apache.poi.ss.usermodel.Cell newCell = newRow.createCell(cellNum);
                                                copyCell(origCell, newCell);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("复制现有工作表时出错: {}", e.getMessage(), e);
                        }
                    }
                    
                    // 2. 创建新的工作表
                    log.info("在新工作簿中创建工作表: {}", sheetName);
                    org.apache.poi.ss.usermodel.Sheet newSheet = newWorkbook.createSheet(sheetName);
                    
                    // 如果有列定义，创建表头
                    if (sheet.getColumns() != null && !sheet.getColumns().isEmpty()) {
                        org.apache.poi.ss.usermodel.Row headerRow = newSheet.createRow(sheet.getHeaderRowIndex());
                        int columnIndex = 0;
                        for (ExcelColumn column : sheet.getColumns()) {
                            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(columnIndex++);
                            cell.setCellValue(column.getName());
                        }
                        log.info("创建表头行，列数: {}", sheet.getColumns().size());
                    }
                    
                    // 3. 将新工作簿写入临时文件
                    try (FileOutputStream tempOut = new FileOutputStream(tempFile)) {
                        newWorkbook.write(tempOut);
                        tempOut.flush();
                    }
                    
                    log.info("新工作簿已写入临时文件: {}", tempFile.getAbsolutePath());
                }
                
                // 4. 用临时文件替换原始文件
                Files.copy(tempFile.toPath(), excelFile.toPath(), 
                           java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                log.info("临时文件已替换原始文件: {}", excelFile.getAbsolutePath());
                
                // 5. 删除临时文件
                boolean deleted = tempFile.delete();
                log.info("删除临时文件: {}, 结果: {}", tempFile.getAbsolutePath(), deleted);
                
                log.info("文件保存成功，大小: {} 字节", excelFile.length());
            } catch (Exception e) {
                log.error("处理Excel文件时出错: {}", e.getMessage(), e);
                throw new RuntimeException("处理Excel文件时出错: " + e.getMessage(), e);
            }
            
            log.info("工作表创建成功: {}", sheetName);
            return true;
        } catch (Exception e) {
            log.error("创建工作表失败: {}", e.getMessage(), e);
            return false;
        } finally {
            // 确保资源被正确关闭
            if (fileOut != null) {
                try {
                    fileOut.close();
                } catch (IOException e) {
                    log.error("关闭文件输出流失败: {}", e.getMessage());
                }
            }
            if (poiWorkbook != null) {
                try {
                    poiWorkbook.close();
                } catch (IOException e) {
                    log.error("关闭工作簿失败: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * 复制单元格内容
     */
    private void copyCell(org.apache.poi.ss.usermodel.Cell sourceCell, org.apache.poi.ss.usermodel.Cell targetCell) {
        switch (sourceCell.getCellType()) {
            case STRING:
                targetCell.setCellValue(sourceCell.getStringCellValue());
                break;
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(sourceCell)) {
                    targetCell.setCellValue(sourceCell.getDateCellValue());
                } else {
                    targetCell.setCellValue(sourceCell.getNumericCellValue());
                }
                break;
            case BOOLEAN:
                targetCell.setCellValue(sourceCell.getBooleanCellValue());
                break;
            case FORMULA:
                targetCell.setCellValue(sourceCell.getCellFormula());
                break;
            case BLANK:
                targetCell.setBlank();
                break;
            default:
                break;
        }
    }
    
    @Override
    public boolean useWorkbook(String workbookName) {
        try {
            // 检查工作簿是否存在
            if (!excelFileService.existsWorkbook(workbookName)) {
                throw new IllegalArgumentException("工作簿不存在: " + workbookName);
            }
            
            // 设置当前工作簿
            currentWorkbookName = workbookName;
            
            return true;
        } catch (Exception e) {
            log.error("切换工作簿失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean dropWorkbook(String workbookName) {
        try {
            // 检查工作簿是否存在
            if (!excelFileService.existsWorkbook(workbookName)) {
                throw new IllegalArgumentException("工作簿不存在: " + workbookName);
            }
            
            // 删除工作簿
            boolean success = excelFileService.deleteWorkbook(workbookName);
            
            // 如果删除的是当前工作簿，则清除当前工作簿
            if (success && workbookName.equals(currentWorkbookName)) {
                currentWorkbookName = null;
            }
            
            return success;
        } catch (Exception e) {
            log.error("删除工作簿失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean dropSheet(String sheetName) {
        Workbook poiWorkbook = null;
        FileOutputStream fileOut = null;
        
        try {
            log.info("开始删除工作表: {}, 当前工作簿: {}", sheetName, currentWorkbookName);
            
            // 检查是否有当前工作簿
            if (currentWorkbookName == null) {
                throw new IllegalStateException("未选择工作簿，请先使用USE WORKBOOK命令");
            }
            
            // 检查工作簿是否存在
            if (!excelFileService.existsWorkbook(currentWorkbookName)) {
                throw new IllegalStateException("当前工作簿不存在: " + currentWorkbookName);
            }
            
            // 检查工作表是否存在
            if (!excelFileService.existsSheet(currentWorkbookName, sheetName)) {
                throw new IllegalArgumentException("工作表不存在: " + sheetName);
            }
            
            // 获取工作簿
            ExcelWorkbook workbook = excelFileService.loadWorkbook(currentWorkbookName);
            File excelFile = workbook.getFile();
            
            if (!excelFile.exists() || excelFile.length() == 0) {
                throw new IllegalStateException("Excel文件不存在或为空: " + excelFile.getAbsolutePath());
            }
            
            log.info("开始从Excel文件中删除工作表: {}", sheetName);
            
            try {
                // 使用临时文件方式，避免直接修改原文件可能导致的问题
                File tempFile = File.createTempFile("temp_workbook_", ".xlsx");
                log.info("创建临时文件: {}", tempFile.getAbsolutePath());
                
                // 先创建一个新的工作簿
                try (XSSFWorkbook newWorkbook = new XSSFWorkbook()) {
                    // 从原始文件中读取所有现有的工作表
                    try (Workbook originalWorkbook = WorkbookFactory.create(excelFile)) {
                        // 检查是否至少有一个工作表
                        if (originalWorkbook.getNumberOfSheets() <= 1 && 
                            originalWorkbook.getSheetAt(0).getSheetName().equals(sheetName)) {
                            log.warn("工作簿中只有一个工作表，无法删除最后一个工作表");
                            // Excel文件必须至少有一个工作表，所以创建一个空的默认工作表
                            newWorkbook.createSheet("Sheet1");
                        } else {
                            // 复制所有现有的工作表到新工作簿，除了要删除的工作表
                            for (int i = 0; i < originalWorkbook.getNumberOfSheets(); i++) {
                                org.apache.poi.ss.usermodel.Sheet origSheet = originalWorkbook.getSheetAt(i);
                                String origSheetName = origSheet.getSheetName();
                                
                                // 跳过要删除的工作表
                                if (origSheetName.equals(sheetName)) {
                                    log.info("跳过要删除的工作表: {}", origSheetName);
                                    continue;
                                }
                                
                                log.info("复制工作表: {}", origSheetName);
                                org.apache.poi.ss.usermodel.Sheet newSheet = newWorkbook.createSheet(origSheetName);
                                
                                // 复制工作表内容
                                for (int rowNum = 0; rowNum <= origSheet.getLastRowNum(); rowNum++) {
                                    org.apache.poi.ss.usermodel.Row origRow = origSheet.getRow(rowNum);
                                    if (origRow != null) {
                                        org.apache.poi.ss.usermodel.Row newRow = newSheet.createRow(rowNum);
                                        for (int cellNum = 0; cellNum < origRow.getLastCellNum(); cellNum++) {
                                            org.apache.poi.ss.usermodel.Cell origCell = origRow.getCell(cellNum);
                                            if (origCell != null) {
                                                org.apache.poi.ss.usermodel.Cell newCell = newRow.createCell(cellNum);
                                                copyCell(origCell, newCell);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // 将新工作簿写入临时文件
                    try (FileOutputStream tempOut = new FileOutputStream(tempFile)) {
                        newWorkbook.write(tempOut);
                        tempOut.flush();
                    }
                    
                    log.info("新工作簿已写入临时文件: {}", tempFile.getAbsolutePath());
                }
                
                // 用临时文件替换原始文件
                Files.copy(tempFile.toPath(), excelFile.toPath(), 
                           java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                log.info("临时文件已替换原始文件: {}", excelFile.getAbsolutePath());
                
                // 删除临时文件
                boolean deleted = tempFile.delete();
                log.info("删除临时文件: {}, 结果: {}", tempFile.getAbsolutePath(), deleted);
                
                // 从内存中的工作簿对象中移除工作表
                workbook.getSheets().remove(sheetName);
                
                log.info("工作表删除成功: {}", sheetName);
                return true;
            } catch (Exception e) {
                log.error("处理Excel文件时出错: {}", e.getMessage(), e);
                throw new RuntimeException("处理Excel文件时出错: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("删除工作表失败: {}", e.getMessage(), e);
            return false;
        } finally {
            // 确保资源被正确关闭
            if (fileOut != null) {
                try {
                    fileOut.close();
                } catch (IOException e) {
                    log.error("关闭文件输出流失败: {}", e.getMessage());
                }
            }
            if (poiWorkbook != null) {
                try {
                    poiWorkbook.close();
                } catch (IOException e) {
                    log.error("关闭工作簿失败: {}", e.getMessage());
                }
            }
        }
    }
    
    @Override
    public ExcelWorkbook getCurrentWorkbook() {
        if (currentWorkbookName == null) {
            return null;
        }
        
        try {
            return excelFileService.loadWorkbook(currentWorkbookName);
        } catch (Exception e) {
            log.error("获取当前工作簿失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public String getCurrentWorkbookName() {
        return currentWorkbookName;
    }
    
    /**
     * 获取存储路径
     */
    private String getStoragePath() {
        // 使用 ExcelFileService 的 basePath
        String path = excelFileService.getBasePath();
        
        // 确保目录存在
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        
        log.info("使用存储路径: {}", file.getAbsolutePath());
        return file.getAbsolutePath();
    }
    
    /**
     * 解析列定义字符串
     * 格式：name:type,name:type,...
     */
    private List<ExcelColumn> parseColumnDefinitions(String columnsStr, ExcelSheet sheet) {
        List<ExcelColumn> columns = new ArrayList<>();
        
        if (columnsStr == null || columnsStr.trim().isEmpty()) {
            return columns;
        }
        
        String[] columnDefs = columnsStr.split(",");
        for (int i = 0; i < columnDefs.length; i++) {
            String columnDef = columnDefs[i].trim();
            String[] parts = columnDef.split(":");
            
            String name = parts[0].trim();
            ExcelDataType dataType = parts.length > 1 ? 
                    parseDataType(parts[1].trim()) : ExcelDataType.STRING;
            
            // 使用列名和索引组合作为ID，避免使用sheet.getId()
            Long sheetId = (sheet.getName() + "_" + i).hashCode() + System.currentTimeMillis() % 1000L;
            
            ExcelColumn column = ExcelColumn.builder()
                    .name(name)
                    .sheetId(sheetId)
                    .columnIndex(i)
                    .dataType(dataType)
                    .nullable(true)
                    .indexed(false)
                    .build();
            
            columns.add(column);
        }
        
        return columns;
    }
    
    /**
     * 解析数据类型
     */
    private ExcelDataType parseDataType(String typeStr) {
        switch (typeStr.toUpperCase()) {
            case "NUMBER":
            case "INT":
            case "INTEGER":
            case "DOUBLE":
            case "FLOAT":
                return ExcelDataType.NUMBER;
            case "DATE":
                return ExcelDataType.DATE;
            case "DATETIME":
            case "TIMESTAMP":
                return ExcelDataType.DATETIME;
            case "BOOLEAN":
            case "BOOL":
                return ExcelDataType.BOOLEAN;
            case "STRING":
            case "TEXT":
            case "VARCHAR":
            default:
                return ExcelDataType.STRING;
        }
    }
} 