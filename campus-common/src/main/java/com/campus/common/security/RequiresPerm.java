package com.campus.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限校验注解
 *
 * <p>value 支持多个权限码，满足其中任意一个即可通过；超级管理员（*:*:*）直接放行。</p>
 *
 * <pre>
 * &#64;RequiresPerm("system:user:add")
 * </pre>
 *
 * @author campus-mart
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPerm {

    String[] value();

    /** 多个权限是否必须全部满足 */
    boolean requireAll() default false;
}
