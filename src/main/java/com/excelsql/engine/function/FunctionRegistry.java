package com.excelsql.engine.function;

import com.excelsql.engine.function.impl.AggregateFunctions;
import com.excelsql.engine.function.impl.DateFunctions;
import com.excelsql.engine.function.impl.StringFunctions;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
/**
 * @Description: 函数注册器
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:42
 */
@Component
public class FunctionRegistry {

    private final Map<String, SQLFunction> functions = new HashMap<>();

    @PostConstruct
    public void registerFunctions() {
        // Register aggregate functions
        register(new AggregateFunctions.CountFunction());
        register(new AggregateFunctions.SumFunction());
        register(new AggregateFunctions.AvgFunction());
        register(new AggregateFunctions.MaxFunction());
        register(new AggregateFunctions.MinFunction());

        // Register string functions
        register(new StringFunctions.ConcatFunction());
        register(new StringFunctions.SubstringFunction());
        register(new StringFunctions.UpperFunction());
        register(new StringFunctions.LowerFunction());
        register(new StringFunctions.TrimFunction());

        // Register date functions
        register(new DateFunctions.NowFunction());
        register(new DateFunctions.TodayFunction());
        register(new DateFunctions.DateFormatFunction());
    }

    public void register(SQLFunction function) {
        functions.put(function.getName().toUpperCase(), function);
    }

    public SQLFunction getFunction(String name) {
        return functions.get(name.toUpperCase());
    }

    public boolean hasFunction(String name) {
        return functions.containsKey(name.toUpperCase());
    }
}
