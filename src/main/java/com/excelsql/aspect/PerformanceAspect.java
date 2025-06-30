package com.excelsql.aspect;

import com.excelsql.annotation.MeasureTime;
import com.excelsql.annotation.MeasureTime.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PerformanceAspect {
    private static final org.slf4j.Logger logger = 
        org.slf4j.LoggerFactory.getLogger(PerformanceAspect.class);

    @Around("@annotation(measureTime)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint, MeasureTime measureTime) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = measureTime.name().isEmpty() ? 
            signature.getMethod().getName() : measureTime.name();
        
        long startTime = System.nanoTime();
        Object result = joinPoint.proceed();
        long duration = System.nanoTime() - startTime;
        
        double convertedDuration = convertDuration(duration, measureTime.unit());
        String unitName = measureTime.unit().name().toLowerCase();
        
        logger.debug("Method [{}] executed in {:.2f} {}", 
            methodName, convertedDuration, unitName);
        
        return result;
    }

    private double convertDuration(long nanos, TimeUnit unit) {
        switch (unit) {
            case NANOSECONDS:
                return nanos;
            case MICROSECONDS:
                return nanos / 1000.0;
            case SECONDS:
                return nanos / 1_000_000_000.0;
            case MILLISECONDS:
            default:
                return nanos / 1_000_000.0;
        }
    }
}