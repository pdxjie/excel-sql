package com.excel.sql.engine.controller;

import com.excel.sql.engine.model.dto.SqlQueryRequest;
import com.excel.sql.engine.model.dto.SqlQueryResult;
import com.excel.sql.engine.service.executor.SqlExecutor;
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

/**
 * SQL查询控制器
 */
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/sql")
@RequiredArgsConstructor
@Tag(name = "SQL查询", description = "SQL查询相关接口")
public class SqlQueryController {
    
    private final SqlExecutor sqlExecutor;
    
    /**
     * 执行SQL查询
     *
     * @param request SQL查询请求
     * @return SQL查询结果
     */
    @PostMapping("/query")
    @Operation(
        summary = "执行SQL查询",
        description = "通过POST请求执行SQL查询，支持复杂查询和大量数据，以及DDL操作如CREATE WORKBOOK, CREATE SHEET, USE WORKBOOK等",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "查询成功",
                content = @Content(schema = @Schema(implementation = SqlQueryResult.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "查询失败",
                content = @Content(schema = @Schema(implementation = SqlQueryResult.class))
            )
        }
    )
    public ResponseEntity<SqlQueryResult> executeQuery(
            @RequestBody @Validated SqlQueryRequest request) {
        log.info("执行SQL查询: {}", request.getSql());
        SqlQueryResult result = sqlExecutor.execute(request);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 执行简单SQL查询（GET方式）
     *
     * @param sql SQL查询语句
     * @param workbook 工作簿名称（可选）
     * @return SQL查询结果
     */
    @GetMapping("/query")
    @Operation(
        summary = "执行简单SQL查询",
        description = "通过GET请求执行简单SQL查询，适用于简单查询和少量数据",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "查询成功",
                content = @Content(schema = @Schema(implementation = SqlQueryResult.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "查询失败",
                content = @Content(schema = @Schema(implementation = SqlQueryResult.class))
            )
        }
    )
    public ResponseEntity<SqlQueryResult> executeSimpleQuery(
            @Parameter(description = "SQL查询语句", required = true)
            @RequestParam String sql,
            @Parameter(description = "工作簿名称", required = false)
            @RequestParam(required = false) String workbook) {
        
        log.info("执行简单SQL查询: {}", sql);
        
        SqlQueryRequest request = SqlQueryRequest.builder()
                .sql(sql)
                .workbook(workbook)
                .build();
        
        SqlQueryResult result = sqlExecutor.execute(request);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
} 