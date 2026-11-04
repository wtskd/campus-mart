package com.campus.system.service;

import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.system.domain.entity.SysLoginLog;
import com.campus.system.domain.entity.SysOperLog;

/**
 * 日志服务（操作日志 + 登录日志，二者都是只读查询 + 批量清理，合并成一个服务）。
 *
 * @author campus-mart
 */
public interface SysLogService {

    PageResult<SysOperLog> pageOper(PageQuery pageQuery, String title, String operName,
                                    String status, String businessType, String beginTime, String endTime);

    void removeOper(String operIds);

    /** 清空全部操作日志（物理删除，走自定义 mapper） */
    void cleanOper();

    PageResult<SysLoginLog> pageLogin(PageQuery pageQuery, String userName, String ipaddr,
                                      String status, String beginTime, String endTime);

    void removeLogin(String infoIds);

    void cleanLogin();
}
