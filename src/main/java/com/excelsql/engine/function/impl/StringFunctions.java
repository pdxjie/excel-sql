package com.excelsql.engine.function.impl;

import com.excelsql.engine.function.SQLFunction;

import java.util.List;
/**
 * @Description: 字符串函数
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:42
 */
public class StringFunctions {

    public static class ConcatFunction implements SQLFunction {
        @Override
        public String getName() { return "CONCAT"; }

        @Override
        public Object execute(List<Object> parameters) {
            StringBuilder result = new StringBuilder();
            for (Object param : parameters) {
                if (param != null) {
                    result.append(param.toString());
                }
            }
            return result.toString();
        }

        @Override
        public int getParameterCount() { return -1; } // Variable

        @Override
        public String getDescription() { return "Concatenate strings"; }
    }

    public static class SubstringFunction implements SQLFunction {
        @Override
        public String getName() { return "SUBSTRING"; }

        @Override
        public Object execute(List<Object> parameters) {
            if (parameters.size() < 2) return null;

            String str = parameters.get(0).toString();
            int start = ((Number) parameters.get(1)).intValue() - 1; // SQL is 1-based

            if (parameters.size() >= 3) {
                int length = ((Number) parameters.get(2)).intValue();
                return str.substring(start, Math.min(start + length, str.length()));
            } else {
                return str.substring(start);
            }
        }

        @Override
        public int getParameterCount() { return 2; } // 2 or 3 parameters

        @Override
        public String getDescription() { return "Extract substring"; }
    }

    public static class UpperFunction implements SQLFunction {
        @Override
        public String getName() { return "UPPER"; }

        @Override
        public Object execute(List<Object> parameters) {
            if (parameters.isEmpty()) return null;
            return parameters.get(0).toString().toUpperCase();
        }

        @Override
        public int getParameterCount() { return 1; }

        @Override
        public String getDescription() { return "Convert to uppercase"; }
    }

    public static class LowerFunction implements SQLFunction {
        @Override
        public String getName() { return "LOWER"; }

        @Override
        public Object execute(List<Object> parameters) {
            if (parameters.isEmpty()) return null;
            return parameters.get(0).toString().toLowerCase();
        }

        @Override
        public int getParameterCount() { return 1; }

        @Override
        public String getDescription() { return "Convert to lowercase"; }
    }

    public static class TrimFunction implements SQLFunction {
        @Override
        public String getName() { return "TRIM"; }

        @Override
        public Object execute(List<Object> parameters) {
            if (parameters.isEmpty()) return null;
            return parameters.get(0).toString().trim();
        }

        @Override
        public int getParameterCount() { return 1; }

        @Override
        public String getDescription() { return "Remove leading and trailing spaces"; }
    }
}
