package com.excel.sql.engine.model.excel;

/**
 * Excel文件类型枚举
 */
public enum ExcelFileType {
    /**
     * Excel 2007+ (.xlsx)
     */
    XLSX,
    
    /**
     * Excel 97-2003 (.xls)
     */
    XLS,
    
    /**
     * CSV文件 (.csv)
     */
    CSV;
    
    /**
     * 根据文件扩展名判断文件类型
     *
     * @param fileName 文件名
     * @return 文件类型
     */
    public static ExcelFileType fromFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".xlsx")) {
            return XLSX;
        } else if (lowerFileName.endsWith(".xls")) {
            return XLS;
        } else if (lowerFileName.endsWith(".csv")) {
            return CSV;
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + fileName);
        }
    }
    
    /**
     * 获取文件扩展名
     *
     * @return 文件扩展名
     */
    public String getExtension() {
        switch (this) {
            case XLSX:
                return ".xlsx";
            case XLS:
                return ".xls";
            case CSV:
                return ".csv";
            default:
                return "";
        }
    }
} 