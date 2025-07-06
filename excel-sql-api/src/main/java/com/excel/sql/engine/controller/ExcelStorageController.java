package com.excel.sql.engine.controller;

import com.excel.sql.engine.config.ExcelConfig;
import com.excel.sql.engine.model.dto.ExcelFileInfo;
import com.excel.sql.engine.model.dto.StoragePathRequest;
import com.excel.sql.engine.service.ExcelStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel存储控制器
 */
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
@Tag(name = "Excel存储管理", description = "Excel文件存储管理相关接口")
public class ExcelStorageController {
    
    private final ExcelStorageService excelStorageService;
    private final ExcelConfig excelConfig;
    
    /**
     * 获取当前存储路径
     *
     * @return 当前存储路径
     */
    @GetMapping("/path")
    @Operation(
        summary = "获取当前存储路径",
        description = "获取Excel文件的当前存储路径",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "成功获取存储路径",
                content = @Content(schema = @Schema(implementation = Map.class))
            )
        }
    )
    public ResponseEntity<Map<String, String>> getCurrentPath() {
        String path = excelStorageService.getCurrentStoragePath();
        Map<String, String> response = new HashMap<>();
        response.put("path", path);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取存储配置信息
     *
     * @return 存储配置信息
     */
    @GetMapping("/config")
    @Operation(
        summary = "获取存储配置信息",
        description = "获取Excel文件的存储配置信息，包括基础路径和临时路径",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "成功获取存储配置",
                content = @Content(schema = @Schema(implementation = Map.class))
            )
        }
    )
    public ResponseEntity<Map<String, Object>> getStorageConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("basePath", excelConfig.getStorage().getBasePath());
        config.put("tempPath", excelConfig.getStorage().getTempPath());
        
        // 添加路径存在状态
        File baseDir = new File(excelConfig.getStorage().getBasePath());
        File tempDir = new File(excelConfig.getStorage().getTempPath());
        
        config.put("basePathExists", baseDir.exists() && baseDir.isDirectory());
        config.put("tempPathExists", tempDir.exists() && tempDir.isDirectory());
        
        return ResponseEntity.ok(config);
    }
    
    /**
     * 更改存储路径
     *
     * @param request 存储路径请求
     * @return 更改结果
     */
    @PostMapping("/path")
    @Operation(
        summary = "更改存储路径",
        description = "更改Excel文件的存储路径，可选择是否迁移现有文件",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "成功更改存储路径",
                content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "更改存储路径失败",
                content = @Content(schema = @Schema(implementation = Map.class))
            )
        }
    )
    public ResponseEntity<Map<String, Object>> changeStoragePath(
            @RequestBody @Validated StoragePathRequest request) {
        
        log.info("更改存储路径: {}, 迁移文件: {}", request.getPath(), request.isMigrateFiles());
        
        boolean success = excelStorageService.changeStoragePath(
                request.getPath(), request.isMigrateFiles());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        
        if (success) {
            response.put("path", excelStorageService.getCurrentStoragePath());
            response.put("basePath", excelConfig.getStorage().getBasePath());
            response.put("tempPath", excelConfig.getStorage().getTempPath());
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "更改存储路径失败");
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取所有Excel文件列表
     *
     * @return Excel文件列表
     */
    @GetMapping("/files")
    @Operation(
        summary = "获取所有Excel文件列表",
        description = "获取当前存储路径下的所有Excel文件信息",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "成功获取文件列表",
                content = @Content(schema = @Schema(implementation = ExcelFileInfo.class))
            )
        }
    )
    public ResponseEntity<List<ExcelFileInfo>> getAllFiles() {
        List<ExcelFileInfo> files = excelStorageService.getAllExcelFiles();
        return ResponseEntity.ok(files);
    }
    
    /**
     * 获取指定路径下的Excel文件列表
     *
     * @param path 指定路径
     * @return Excel文件列表
     */
    @GetMapping("/files/path")
    @Operation(
        summary = "获取指定路径下的Excel文件列表",
        description = "获取指定路径下的所有Excel文件信息",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "成功获取文件列表",
                content = @Content(schema = @Schema(implementation = ExcelFileInfo.class))
            )
        }
    )
    public ResponseEntity<List<ExcelFileInfo>> getFilesByPath(
            @Parameter(description = "指定路径", required = true)
            @RequestParam String path) {
        
        List<ExcelFileInfo> files = excelStorageService.getExcelFilesInPath(path);
        return ResponseEntity.ok(files);
    }
} 