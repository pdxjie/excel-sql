//package com.excelsql.controller;
//
//import com.excelsql.service.ExcelSQLService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import javax.annotation.Resource;
//import java.util.Map;
///**
// * @Description: TODO::Need To Do
// * @Author: IT 派同学
// * @Date: 2025-06-30-22:40
// */
//@RestController
//@RequestMapping("/api/excel-sql")
//@CrossOrigin(origins = "*")
//public class ExcelSQLController {
//
//    @Resource
//    private ExcelSQLService excelSQLService;
//
//    @PostMapping("/execute")
//    public ResponseEntity<Map<String, Object>> executeSQL(@RequestBody Map<String, String> request) {
//        try {
//            String sql = request.get("sql");
//            Map<String, Object> result = excelSQLService.executeSQL(sql);
//            return ResponseEntity.ok(result);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest()
//                    .body(Map.of("error", e.getMessage()));
//        }
//    }
//
//    @PostMapping("/execute-async")
//    public ResponseEntity<Map<String, Object>> executeSQLAsync(@RequestBody Map<String, String> request) {
//        try {
//            String sql = request.get("sql");
//            String taskId = excelSQLService.executeSQLAsync(sql);
//            return ResponseEntity.ok(Map.of("taskId", taskId));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest()
//                    .body(Map.of("error", e.getMessage()));
//        }
//    }
//
//    @GetMapping("/task/{taskId}")
//    public ResponseEntity<Map<String, Object>> getTaskResult(@PathVariable String taskId) {
//        Map<String, Object> result = excelSQLService.getTaskResult(taskId);
//        return ResponseEntity.ok(result);
//    }
//
//    @GetMapping("/workbooks")
//    public ResponseEntity<Map<String, Object>> listWorkbooks() {
//        Map<String, Object> result = excelSQLService.listWorkbooks();
//        return ResponseEntity.ok(result);
//    }
//
//    @GetMapping("/workbooks/{workbook}/sheets")
//    public ResponseEntity<Map<String, Object>> listSheets(@PathVariable String workbook) {
//        Map<String, Object> result = excelSQLService.listSheets(workbook);
//        return ResponseEntity.ok(result);
//    }
//}
