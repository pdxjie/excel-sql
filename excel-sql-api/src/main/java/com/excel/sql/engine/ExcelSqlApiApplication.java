package com.excel.sql.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Excel SQL API 主应用类
 * 提供兼容MySQL语法的Excel文件SQL查询引擎
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class ExcelSqlApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExcelSqlApiApplication.class, args);
    }
} 