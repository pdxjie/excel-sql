package com.excelsql.controller;

import com.excelsql.service.AsyncExcelService;
import com.excelsql.service.ExcelSQLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
/**
 * @Description: TODO::Need To Do
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:40
 */
@RestController
@RequestMapping("/api/excel-sql")
@CrossOrigin(origins = "*")
public class ExcelSQLController {

    @Autowired
    private ExcelSQLService excelSQLService;

    @Autowired
    private AsyncExcelService asyncExcelService;

    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeSQL(@RequestBody Map<String, String> request) {
        try {
            String sql = request.get("sql");
            if (sql == null || sql.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("SQL statement is required"));
            }

            Object result = excelSQLService.executeSQL(sql);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/execute-async")
    public ResponseEntity<Map<String, Object>> executeSQLAsync(@RequestBody Map<String, String> request) {
        try {
            String sql = request.get("sql");
            if (sql == null || sql.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("SQL statement is required"));
            }

            CompletableFuture<Object> future = asyncExcelService.executeQueryAsync(sql);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Query submitted for async execution");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = excelSQLService.getQueryStatistics();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", stats);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> clearCache() {
        try {
            excelSQLService.clearCache();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cache cleared successfully");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", "healthy");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}