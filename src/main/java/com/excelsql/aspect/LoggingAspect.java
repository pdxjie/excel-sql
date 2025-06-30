package com.excelsql.aspect;

import com.excelsql.annotation.LogExecution;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("@annotation(logExecution)")
    public Object logExecution(ProceedingJoinPoint joinPoint, LogExecution logExecution) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String annotationValue = logExecution.value().isEmpty() ? methodName : logExecution.value();
        
        if (logger.isInfoEnabled()) {
            String params = logExecution.logParams() ? 
                Arrays.toString(joinPoint.getArgs()) : "[params logging disabled]";
            logger.info("Executing [{}] in {}.{} with params: {}", 
                annotationValue, className, methodName, params);
        }
        
        Object result = joinPoint.proceed();
        
        if (logExecution.logResult() && logger.isInfoEnabled()) {
            logger.info("Method [{}] returned: {}", annotationValue, result);
        }
        
        return result;
    }
}