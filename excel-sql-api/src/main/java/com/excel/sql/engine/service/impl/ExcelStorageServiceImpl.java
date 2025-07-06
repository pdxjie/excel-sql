package com.excel.sql.engine.service.impl;

import com.excel.sql.engine.config.ExcelConfig;
import com.excel.sql.engine.model.dto.ExcelFileInfo;
import com.excel.sql.engine.service.ExcelStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Excel存储服务实现类
 */
@Slf4j
@Service
public class ExcelStorageServiceImpl implements ExcelStorageService {
    
    private final ExcelConfig excelConfig;
    
    public ExcelStorageServiceImpl(ExcelConfig excelConfig) {
        this.excelConfig = excelConfig;
    }
    
    @PostConstruct
    public void init() {
        // 确保存储目录存在
        File storageDir = new File(excelConfig.getStorage().getBasePath());
        if (!storageDir.exists()) {
            boolean created = storageDir.mkdirs();
            if (created) {
                log.info("创建Excel存储目录: {}", storageDir.getAbsolutePath());
            } else {
                log.error("无法创建Excel存储目录: {}", storageDir.getAbsolutePath());
            }
        }
        
        // 确保临时目录存在
        File tempDir = new File(excelConfig.getStorage().getTempPath());
        if (!tempDir.exists()) {
            boolean created = tempDir.mkdirs();
            if (created) {
                log.info("创建Excel临时目录: {}", tempDir.getAbsolutePath());
            } else {
                log.error("无法创建Excel临时目录: {}", tempDir.getAbsolutePath());
            }
        }
        
        log.info("Excel存储路径: {}", storageDir.getAbsolutePath());
        log.info("Excel临时路径: {}", tempDir.getAbsolutePath());
    }
    
    @Override
    public String getCurrentStoragePath() {
        return new File(excelConfig.getStorage().getBasePath()).getAbsolutePath();
    }
    
    @Override
    public boolean changeStoragePath(String newPath, boolean migrateFiles) {
        try {
            // 验证新路径
            File newDir = new File(newPath);
            if (!newDir.exists()) {
                boolean created = newDir.mkdirs();
                if (!created) {
                    log.error("无法创建新的存储目录: {}", newDir.getAbsolutePath());
                    return false;
                }
            }
            
            // 获取当前路径下的所有Excel文件
            File currentDir = new File(excelConfig.getStorage().getBasePath());
            List<File> excelFiles = new ArrayList<>();
            
            if (currentDir.exists() && currentDir.isDirectory()) {
                File[] files = currentDir.listFiles((dir, name) -> 
                        name.toLowerCase().endsWith(".xlsx") || name.toLowerCase().endsWith(".xls"));
                
                if (files != null) {
                    excelFiles.addAll(Arrays.asList(files));
                }
            }
            
            // 如果需要迁移文件
            if (migrateFiles && !excelFiles.isEmpty()) {
                log.info("开始迁移Excel文件，共{}个文件", excelFiles.size());
                
                for (File file : excelFiles) {
                    try {
                        Path source = file.toPath();
                        Path target = Paths.get(newDir.getAbsolutePath(), file.getName());
                        
                        // 复制文件到新目录
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        log.info("已迁移文件: {} -> {}", source, target);
                        
                        // 迁移成功后删除原文件
                        Files.delete(source);
                        log.info("已删除原文件: {}", source);
                    } catch (IOException e) {
                        log.error("迁移文件失败: {}, 错误: {}", file.getName(), e.getMessage(), e);
                    }
                }
            }
            
            // 更新存储路径
            excelConfig.getStorage().setBasePath(newDir.getAbsolutePath());
            
            // 更新临时目录路径
            String newTempPath = Paths.get(newDir.getAbsolutePath(), "temp").toString();
            File newTempDir = new File(newTempPath);
            if (!newTempDir.exists()) {
                boolean created = newTempDir.mkdirs();
                if (created) {
                    log.info("创建新的Excel临时目录: {}", newTempDir.getAbsolutePath());
                } else {
                    log.error("无法创建新的Excel临时目录: {}", newTempDir.getAbsolutePath());
                }
            }
            excelConfig.getStorage().setTempPath(newTempDir.getAbsolutePath());
            
            log.info("已更新Excel存储路径: {}", excelConfig.getStorage().getBasePath());
            log.info("已更新Excel临时路径: {}", excelConfig.getStorage().getTempPath());
            
            return true;
        } catch (Exception e) {
            log.error("更改存储路径失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public List<ExcelFileInfo> getAllExcelFiles() {
        return getExcelFilesInPath(excelConfig.getStorage().getBasePath());
    }
    
    @Override
    public List<ExcelFileInfo> getExcelFilesInPath(String path) {
        List<ExcelFileInfo> result = new ArrayList<>();
        
        try {
            File dir = new File(path);
            if (!dir.exists() || !dir.isDirectory()) {
                log.error("指定的路径不存在或不是目录: {}", path);
                return result;
            }
            
            File[] files = dir.listFiles((dir1, name) -> 
                    name.toLowerCase().endsWith(".xlsx") || name.toLowerCase().endsWith(".xls"));
            
            if (files == null || files.length == 0) {
                log.info("指定路径下没有Excel文件: {}", path);
                return result;
            }
            
            for (File file : files) {
                try {
                    // 获取文件基本信息
                    String fileName = file.getName();
                    String name = fileName.substring(0, fileName.lastIndexOf('.'));
                    long fileSize = file.length();
                    LocalDateTime lastModified = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault());
                    
                    // 获取工作表列表
                    List<String> sheets = getSheetNames(file);
                    
                    // 创建ExcelFileInfo对象
                    ExcelFileInfo fileInfo = ExcelFileInfo.builder()
                            .name(name)
                            .filePath(file.getAbsolutePath())
                            .fileSize(fileSize)
                            .lastModified(lastModified)
                            .sheets(sheets)
                            .build();
                    
                    result.add(fileInfo);
                } catch (Exception e) {
                    log.error("处理Excel文件信息失败: {}, 错误: {}", file.getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("获取Excel文件列表失败: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 获取Excel文件中的工作表名称列表
     * 
     * @param file Excel文件
     * @return 工作表名称列表
     */
    private List<String> getSheetNames(File file) {
        List<String> sheetNames = new ArrayList<>();
        
        try (Workbook workbook = WorkbookFactory.create(file)) {
            int sheetCount = workbook.getNumberOfSheets();
            for (int i = 0; i < sheetCount; i++) {
                sheetNames.add(workbook.getSheetName(i));
            }
        } catch (Exception e) {
            log.error("读取Excel工作表列表失败: {}, 错误: {}", file.getName(), e.getMessage());
        }
        
        return sheetNames;
    }
} 