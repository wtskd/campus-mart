package com.campus.system.mapper;

import org.apache.ibatis.annotations.Update;

/**
 * 日志清理专用 Mapper。
 *
 * <p>框架装了 BlockAttackInnerInterceptor（禁止不带 where 的 delete/update），
 * 而「清空日志」天然就是全表操作，所以用 TRUNCATE 走 DDL 通道绕开该拦截器，
 * 同时 TRUNCATE 比 DELETE 更快且能重置自增主键。</p>
 *
 * @author campus-mart
 */
public interface SysLogCustomMapper {

    @Update("TRUNCATE TABLE sys_oper_log")
    void truncateOperLog();

    @Update("TRUNCATE TABLE sys_login_log")
    void truncateLoginLog();
}
