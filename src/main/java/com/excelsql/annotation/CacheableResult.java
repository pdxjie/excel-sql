package com.excelsql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标记方法的返回值可以被缓存
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheableResult {

    String cacheName() default "";

    long ttl() default 60; // 默认60秒

    String keyGenerator() default "";
}