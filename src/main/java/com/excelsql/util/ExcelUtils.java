package com.excelsql.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * @Description: Excel 工具类
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:44
 */
public class ExcelUtils {

    public static Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        return (long) numericValue;
                    } else {
                        return numericValue;
                    }
                }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return null;
            default:
                return cell.toString();
        }
    }

    public static String getCellValueAsString(Cell cell) {
        Object value = getCellValue(cell);
        return value != null ? value.toString() : "";
    }

    public static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
            return;
        }

        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof LocalDateTime) {
            LocalDateTime dateTime = (LocalDateTime) value;
            Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
            cell.setCellValue(date);
        } else if (value instanceof LocalDate) {
            LocalDate localDate = (LocalDate) value;
            Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            cell.setCellValue(date);
        } else {
            cell.setCellValue(value.toString());
        }
    }
}

