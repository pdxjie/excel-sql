package com.excel.sql.engine.controller;

import com.excel.sql.engine.model.dto.FileUploadRequest;
import com.excel.sql.engine.model.excel.ExcelWorkbook;
import com.excel.sql.engine.service.ExcelFileService;
import com.excel.sql.engine.service.cache.QueryCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件控制器
 */
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "Excel文件上传和管理相关接口")
public class FileController {
    
    private final ExcelFileService excelFileService;
    private final QueryCacheService queryCacheService;
    
    /**
     * 上传Excel文件
     *
     * @param file 文件
     * @param workbookName 工作簿名称（可选）
     * @param headerRowIndex 表头行索引（可选）
     * @param dataStartRowIndex 数据开始行索引（可选）
     * @param overwrite 是否覆盖（可选）
     * @param createIndex 是否创建索引（可选）
     * @return 上传结果
     */
    @PostMapping("/upload")
    @Operation(
        summary = "上传Excel文件",
        description = "上传Excel文件并解析为数据库表结构，支持设置表头和数据行",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "上传成功"
            ),
            @ApiResponse(
                responseCode = "400",
                description = "上传失败"
            )
        }
    )
    public ResponseEntity<Map<String, Object>> uploadFile(
            @Parameter(description = "Excel文件", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "工作簿名称，不提供则使用文件名")
            @RequestParam(required = false) String workbookName,
            @Parameter(description = "表头行索引，默认为0")
            @RequestParam(required = false, defaultValue = "0") Integer headerRowIndex,
            @Parameter(description = "数据开始行索引，默认为1")
            @RequestParam(required = false, defaultValue = "1") Integer dataStartRowIndex,
            @Parameter(description = "是否覆盖同名文件，默认为false")
            @RequestParam(required = false, defaultValue = "false") Boolean overwrite,
            @Parameter(description = "是否创建索引，默认为true")
            @RequestParam(required = false, defaultValue = "true") Boolean createIndex) {
        
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .workbookName(workbookName)
                    .headerRowIndex(headerRowIndex)
                    .dataStartRowIndex(dataStartRowIndex)
                    .overwrite(overwrite)
                    .createIndex(createIndex)
                    .build();
            
            ExcelWorkbook workbook = excelFileService.uploadFile(file, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文件上传成功");
            response.put("workbookName", workbook.getName());
            response.put("sheets", workbook.getSheets().keySet());
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "文件上传失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取所有工作簿
     *
     * @return 工作簿列表
     */
    @GetMapping
    @Operation(
        summary = "获取所有工作簿",
        description = "获取系统中所有已上传的Excel工作簿列表",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "获取成功",
                content = @Content(schema = @Schema(implementation = ExcelWorkbook.class))
            )
        }
    )
    public ResponseEntity<List<ExcelWorkbook>> getAllWorkbooks() {
        List<ExcelWorkbook> workbooks = excelFileService.getAllWorkbooks();
        return ResponseEntity.ok(workbooks);
    }
    
    /**
     * 获取工作簿详情
     *
     * @param workbookName 工作簿名称
     * @return 工作簿详情
     */
    @GetMapping("/{workbookName}")
    @Operation(
        summary = "获取工作簿详情",
        description = "获取指定工作簿的详细信息，包括所有工作表",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "获取成功",
                content = @Content(schema = @Schema(implementation = ExcelWorkbook.class))
            ),
            @ApiResponse(
                responseCode = "404",
                description = "工作簿不存在"
            )
        }
    )
    public ResponseEntity<ExcelWorkbook> getWorkbook(
            @Parameter(description = "工作簿名称", required = true)
            @PathVariable String workbookName) {
        ExcelWorkbook workbook = excelFileService.loadWorkbook(workbookName);
        
        if (workbook != null) {
            return ResponseEntity.ok(workbook);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 删除工作簿
     *
     * @param workbookName 工作簿名称
     * @return 删除结果
     */
    @DeleteMapping("/{workbookName}")
    @Operation(
        summary = "删除工作簿",
        description = "删除指定的工作簿及其所有工作表，同时清除相关缓存",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "操作完成"
            )
        }
    )
    public ResponseEntity<Map<String, Object>> deleteWorkbook(
            @Parameter(description = "工作簿名称", required = true)
            @PathVariable String workbookName) {
        boolean success = excelFileService.deleteWorkbook(workbookName);
        
        // 清除缓存
        if (success) {
            queryCacheService.clearCache(workbookName);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "工作簿删除成功" : "工作簿删除失败");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 清除缓存
     *
     * @param workbookName 工作簿名称
     * @param sheetName 工作表名称（可选）
     * @return 清除结果
     */
    @DeleteMapping("/{workbookName}/cache")
    @Operation(
        summary = "清除查询缓存",
        description = "清除指定工作簿或工作表的查询缓存",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "缓存清除成功"
            )
        }
    )
    public ResponseEntity<Map<String, Object>> clearCache(
            @Parameter(description = "工作簿名称", required = true)
            @PathVariable String workbookName,
            @Parameter(description = "工作表名称，不提供则清除整个工作簿的缓存")
            @RequestParam(required = false) String sheetName) {
        
        if (sheetName != null) {
            queryCacheService.clearCache(workbookName, sheetName);
        } else {
            queryCacheService.clearCache(workbookName);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "缓存清除成功");
        
        return ResponseEntity.ok(response);
    }
} 