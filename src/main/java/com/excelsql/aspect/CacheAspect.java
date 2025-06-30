//package com.excelsql.aspect;
//
//import com.excelsql.annotation.CacheableResult;
//import com.excelsql.engine.cache.ExcelCacheManager;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.reflect.MethodSignature;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.Resource;
//import java.lang.reflect.Method;
//import java.util.Arrays;
//
//@Aspect
//@Component
//public class CacheAspect {
//
//    @Resource
//    private ExcelCacheManager cacheManager;
//
//    @Around("@annotation(cacheable)")
//    public Object cacheResult(ProceedingJoinPoint joinPoint, CacheableResult cacheable) throws Throwable {
//        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
//        Method method = signature.getMethod();
//
//        String cacheName = cacheable.cacheName().isEmpty() ?
//            method.getDeclaringClass().getName() + "." + method.getName() :
//            cacheable.cacheName();
//
//        String cacheKey = generateCacheKey(joinPoint.getArgs(), cacheable.keyGenerator());
//
//        // 尝试从缓存获取结果
//        Object cachedResult = cacheManager.getCachedResult(cacheName, cacheKey);
//        if (cachedResult != null) {
//            return cachedResult;
//        }
//
//        // 缓存未命中，执行方法
//        Object result = joinPoint.proceed();
//
//        // 缓存结果
//        if (result != null) {
//            cacheManager.cacheResult(cacheName, cacheKey, result, cacheable.ttl());
//        }
//
//        return result;
//    }
//
//    private String generateCacheKey(Object[] args, String keyGenerator) {
//        // 简单实现：使用参数哈希值作为缓存键
//        // 实际应用中可以使用更复杂的策略
//        return Arrays.deepHashCode(args) + "";
//    }
//}