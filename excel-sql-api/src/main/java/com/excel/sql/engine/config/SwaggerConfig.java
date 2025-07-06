package com.excel.sql.engine.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger配置类 (使用 SpringDoc OpenAPI)
 */
@Configuration
public class SwaggerConfig {
    
    /**
     * OpenAPI配置
     */
    @Bean
    public OpenAPI excelSqlOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Excel SQL API")
                        .description("兼容MySQL语法的Excel文件SQL引擎")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Excel SQL Team")
                                .email("excel-sql@example.com")
                                .url("https://github.com/yourusername/excel-sql-api"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("Excel SQL API文档")
                        .url("https://github.com/yourusername/excel-sql-api/docs"));
    }
} 