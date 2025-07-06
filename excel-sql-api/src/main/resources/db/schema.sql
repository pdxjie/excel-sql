-- 创建数据库
CREATE DATABASE IF NOT EXISTS excel_sql_metadata DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE excel_sql_metadata;

-- 工作簿表
CREATE TABLE IF NOT EXISTS t_workbook (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(255) NOT NULL COMMENT '工作簿名称',
    file_path VARCHAR(1000) NOT NULL COMMENT '文件路径',
    file_type VARCHAR(10) NOT NULL COMMENT '文件类型',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    last_modified DATETIME NOT NULL COMMENT '最后修改时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除（0-未删除，1-已删除）',
    UNIQUE KEY uk_name (name, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作簿表';

-- 工作表表
CREATE TABLE IF NOT EXISTS t_sheet (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    workbook_id BIGINT NOT NULL COMMENT '所属工作簿ID',
    name VARCHAR(255) NOT NULL COMMENT '工作表名称',
    sheet_index INT NOT NULL COMMENT '工作表索引',
    header_row_index INT NOT NULL DEFAULT 0 COMMENT '表头行索引（从0开始）',
    data_start_row_index INT NOT NULL DEFAULT 1 COMMENT '数据开始行索引（从0开始）',
    total_rows INT NOT NULL DEFAULT 0 COMMENT '总行数',
    indexed BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已创建索引',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除（0-未删除，1-已删除）',
    UNIQUE KEY uk_workbook_name (workbook_id, name, deleted),
    KEY idx_workbook_id (workbook_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作表表';

-- 列表
CREATE TABLE IF NOT EXISTS t_column (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    sheet_id BIGINT NOT NULL COMMENT '所属工作表ID',
    name VARCHAR(255) NOT NULL COMMENT '列名',
    column_index INT NOT NULL COMMENT '列索引（从0开始）',
    data_type VARCHAR(20) NOT NULL COMMENT '数据类型',
    nullable BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否可为空',
    indexed BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为索引列',
    format_pattern VARCHAR(100) DEFAULT NULL COMMENT '格式模式（如日期格式）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除（0-未删除，1-已删除）',
    UNIQUE KEY uk_sheet_name (sheet_id, name, deleted),
    KEY idx_sheet_id (sheet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='列表';

-- 索引表
CREATE TABLE IF NOT EXISTS t_index (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    sheet_id BIGINT NOT NULL COMMENT '所属工作表ID',
    name VARCHAR(255) NOT NULL COMMENT '索引名称',
    index_type VARCHAR(20) NOT NULL COMMENT '索引类型（PRIMARY, SECONDARY, COMPOSITE）',
    columns VARCHAR(1000) NOT NULL COMMENT '索引列（多列索引用逗号分隔）',
    unique_index BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否唯一索引',
    metadata TEXT DEFAULT NULL COMMENT '索引元数据（JSON格式）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除（0-未删除，1-已删除）',
    UNIQUE KEY uk_sheet_name (sheet_id, name, deleted),
    KEY idx_sheet_id (sheet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='索引表';

-- 查询缓存表
CREATE TABLE IF NOT EXISTS t_query_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    workbook_id BIGINT DEFAULT NULL COMMENT '工作簿ID',
    cache_key VARCHAR(255) NOT NULL COMMENT '缓存键',
    sql_query TEXT NOT NULL COMMENT 'SQL查询语句',
    result_json MEDIUMTEXT NOT NULL COMMENT '查询结果（JSON格式）',
    execution_time BIGINT NOT NULL COMMENT '执行时间（毫秒）',
    hit_count INT NOT NULL DEFAULT 0 COMMENT '命中次数',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除（0-未删除，1-已删除）',
    UNIQUE KEY uk_cache_key (cache_key, deleted),
    KEY idx_workbook_id (workbook_id),
    KEY idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查询缓存表'; 