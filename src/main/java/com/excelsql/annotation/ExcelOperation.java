package com.excelsql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记Excel操作相关的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelOperation {

    OperationType type();
    
    enum OperationType {
        READ, WRITE, QUERY, METADATA
    }
}