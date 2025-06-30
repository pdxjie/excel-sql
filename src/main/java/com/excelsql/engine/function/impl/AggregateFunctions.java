package com.excelsql.engine.function.impl;

import com.excelsql.engine.function.SQLFunction;

import java.util.List;
/**
 * @Description: 聚合函数
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:42
 */
public class AggregateFunctions {

    public static class CountFunction implements SQLFunction {
        @Override
        public String getName() { return "COUNT"; }

        @Override
        public Object execute(List<Object> parameters) {
            if (parameters == null) return 0L;
            return (long) parameters.stream().mapToInt(p -> p != null ? 1 : 0).sum();
        }

        @Override
        public int getParameterCount() { return -1; } // Variable

        @Override
        public String getDescription() { return "Count non-null values"; }
    }

    public static class SumFunction implements SQLFunction {
        @Override
        public String getName() { return "SUM"; }

        @Override
        public Object execute(List<Object> parameters) {
            double sum = 0.0;
            for (Object param : parameters) {
                if (param instanceof Number) {
                    sum += ((Number) param).doubleValue();
                }
            }
            return sum;
        }

        @Override
        public int getParameterCount() { return -1; } // Variable

        @Override
        public String getDescription() { return "Sum numeric values"; }
    }

    public static class AvgFunction implements SQLFunction {
        @Override
        public String getName() { return "AVG"; }

        @Override
        public Object execute(List<Object> parameters) {
            double sum = 0.0;
            int count = 0;
            for (Object param : parameters) {
                if (param instanceof Number) {
                    sum += ((Number) param).doubleValue();
                    count++;
                }
            }
            return count > 0 ? sum / count : 0.0;
        }

        @Override
        public int getParameterCount() { return -1; } // Variable

        @Override
        public String getDescription() { return "Average of numeric values"; }
    }

    public static class MaxFunction implements SQLFunction {
        @Override
        public String getName() { return "MAX"; }

        @Override
        public Object execute(List<Object> parameters) {
            return parameters.stream()
                    .filter(p -> p instanceof Comparable)
                    .map(p -> (Comparable) p)
                    .max(Comparable::compareTo)
                    .orElse(null);
        }

        @Override
        public int getParameterCount() { return -1; } // Variable

        @Override
        public String getDescription() { return "Maximum value"; }
    }

    public static class MinFunction implements SQLFunction {
        @Override
        public String getName() { return "MIN"; }

        @Override
        public Object execute(List<Object> parameters) {
            return parameters.stream()
                    .filter(p -> p instanceof Comparable)
                    .map(p -> (Comparable) p)
                    .min(Comparable::compareTo)
                    .orElse(null);
        }

        @Override
        public int getParameterCount() { return -1; } // Variable

        @Override
        public String getDescription() { return "Minimum value"; }
    }
}
