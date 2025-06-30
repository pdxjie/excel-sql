//package com.excelsql.aspect;
//
//import com.excelsql.exception.ExcelSQLException;
//import com.excelsql.exception.ParseException;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Component;
//
//@Aspect
//@Component
//public class ExceptionHandlingAspect {
//    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandlingAspect.class);
//
//    @Around("execution(* com.excelsql.controller..*(..))")
//    public Object handleControllerExceptions(ProceedingJoinPoint joinPoint) throws Throwable {
//        try {
//            return joinPoint.proceed();
//        } catch (ExcelSQLException e) {
//            // 业务异常，已处理过
//            throw e;
//        } catch (ParseException e) {
//            // SQL解析异常
//            throw new ExcelSQLException("SQL syntax error: " + e.getMessage(),
//                HttpStatus.BAD_REQUEST, e);
//        } catch (Exception e) {
//            // 其他未捕获异常
//            logger.error("Unhandled exception in controller: ", e);
//            throw new ExcelSQLException("Internal server error",
//                HttpStatus.INTERNAL_SERVER_ERROR, e);
//        }
//    }
//
//    @Around("execution(* com.excelsql.service..*(..))")
//    public Object handleServiceExceptions(ProceedingJoinPoint joinPoint) throws Throwable {
//        try {
//            return joinPoint.proceed();
//        } catch (ExcelSQLException e) {
//            throw e; // 重新抛出已处理的异常
//        } catch (Exception e) {
//            // 服务层异常转换为业务异常
//            throw new ExcelSQLException("Service error: " + e.getMessage(),
//                HttpStatus.INTERNAL_SERVER_ERROR, e);
//        }
//    }
//}