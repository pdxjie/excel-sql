package com.excelsql.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @Description: 类型转换器
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:44
 *
 */

@Component
public class TypeConverter {

    public Object convertValue(Object value, String targetType) {
        if (value == null) {
            return null;
        }

        String valueStr = value.toString();

        switch (targetType.toUpperCase()) {
            case "INT":
            case "INTEGER":
                return Integer.parseInt(valueStr);
            case "LONG":
                return Long.parseLong(valueStr);
            case "DOUBLE":
            case "DECIMAL":
                return Double.parseDouble(valueStr);
            case "BOOLEAN":
                return Boolean.parseBoolean(valueStr);
            case "DATE":
                return LocalDate.parse(valueStr);
            case "DATETIME":
                return LocalDateTime.parse(valueStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            case "VARCHAR":
            case "STRING":
            default:
                return valueStr;
        }
    }
}