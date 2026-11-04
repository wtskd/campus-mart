package com.campus.common.log;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注在 Controller 方法上，自动记录到 sys_oper_log
 *
 * @author campus-mart
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {

    /** 模块标题，如「用户管理」 */
    String title();

    /** 业务类型：0 其它 1 新增 2 修改 3 删除 4 授权 5 导出 6 导入 7 强退 8 生成代码 9 清空 */
    int businessType() default 0;

    /** 是否保存请求参数 */
    boolean saveRequestData() default true;

    /** 是否保存响应结果 */
    boolean saveResponseData() default false;
}
