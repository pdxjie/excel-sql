package com.excelsql.engine.function.impl;

/**
 * @Description: 日期函数
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:42
 */
import com.excelsql.engine.function.SQLFunction;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DateFunctions {

    public static class NowFunction implements SQLFunction {
        @Override
        public String getName() { return "NOW"; }

        @Override
        public Object execute(List<Object> parameters) {
            return LocalDateTime.now();
        }

        @Override
        public int getParameterCount() { return 0; }

        @Override
        public String getDescription() { return "Current date and time"; }
    }

    public static class TodayFunction implements SQLFunction {
        @Override
        public String getName() { return "TODAY"; }

        @Override
        public Object execute(List<Object> parameters) {
            return LocalDate.now();
        }

        @Override
        public int getParameterCount() { return 0; }

        @Override
        public String getDescription() { return "Current date"; }
    }

    public static class DateFormatFunction implements SQLFunction {
        @Override
        public String getName() { return "DATE_FORMAT"; }

        @Override
        public Object execute(List<Object> parameters) {
            if (parameters.size() < 2) return null;

            Object dateObj = parameters.get(0);
            String pattern = parameters.get(1).toString();

            if (dateObj instanceof LocalDateTime) {
                return ((LocalDateTime) dateObj).format(DateTimeFormatter.ofPattern(pattern));
            } else if (dateObj instanceof LocalDate) {
                return ((LocalDate) dateObj).format(DateTimeFormatter.ofPattern(pattern));
            }

            return dateObj.toString();
        }

        @Override
        public int getParameterCount() { return 2; }

        @Override
        public String getDescription() { return "Format date with pattern"; }
    }
}
