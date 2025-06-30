//package com.excelsql.aspect;
//
//import com.excelsql.annotation.ExcelOperation;
//import com.excelsql.annotation.ExcelOperation.OperationType;
//import com.excelsql.engine.storage.ExcelStorage;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.Resource;
//
//@Aspect
//@Component
//public class ExcelOperationAspect {
//
//    @Resource
//    private ExcelStorage excelStorage;
//
//    @Around("@annotation(excelOp)")
//    public Object handleExcelOperation(ProceedingJoinPoint joinPoint, ExcelOperation excelOp) throws Throwable {
//        try {
//            // 对于写操作，获取文件锁
//            if (excelOp.type() == OperationType.WRITE) {
//                String filePath = getFilePathFromArgs(joinPoint.getArgs());
//                if (filePath != null) {
//                    excelStorage.acquireFileLock(filePath);
//                }
//            }
//
//            return joinPoint.proceed();
//        } finally {
//            // 对于写操作，释放文件锁
//            if (excelOp.type() == OperationType.WRITE) {
//                String filePath = getFilePathFromArgs(joinPoint.getArgs());
//                if (filePath != null) {
//                    excelStorage.releaseFileLock(filePath);
//                }
//            }
//        }
//    }
//
//    private String getFilePathFromArgs(Object[] args) {
//        for (Object arg : args) {
//            if (arg instanceof String) {
//                String str = (String) arg;
//                if (str.endsWith(".xlsx") || str.endsWith(".xls") || str.endsWith(".csv")) {
//                    return str;
//                }
//            }
//        }
//        return null;
//    }
//}