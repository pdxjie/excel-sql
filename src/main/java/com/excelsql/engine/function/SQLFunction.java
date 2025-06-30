package com.excelsql.engine.function;

import java.util.List;

/**
 * @Description: 函数接口
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:42
 */
public interface SQLFunction {
    String getName();
    Object execute(List<Object> parameters);
    int getParameterCount();
    String getDescription();
}
