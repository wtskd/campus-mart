package com.campus.system.service;

import com.campus.api.dto.LoginInfoDTO;
import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.system.domain.dto.SysUserSaveDTO;
import com.campus.system.domain.dto.SysUserVO;

import java.util.List;

/**
 * 用户管理服务。
 *
 * <p>另外承担 campus-auth 登录所需的内部查询（登录信息、菜单ID、登录记录），
 * 放在这里是因为这些查询都围绕 sys_user 聚合，不必再拆一个服务。</p>
 *
 * @author campus-mart
 */
public interface SysUserService {

    /** 分页查询（不返回密码） */
    PageResult<SysUserVO> page(PageQuery pageQuery, String username, String nickname,
                               String phone, String status, Long deptId);

    /** 详情，含 roleIds */
    SysUserVO detail(Long userId);

    void add(SysUserSaveDTO dto);

    void update(SysUserSaveDTO dto);

    /** 批量逻辑删除（逗号分隔） */
    void remove(String userIds);

    void resetPwd(Long userId, String password);

    /** 启停；停用时清理登录态使令牌立即失效 */
    void changeStatus(Long userId, String status);

    /* ---------------- 内部接口（campus-auth 通过 Feign 调用） ---------------- */

    LoginInfoDTO loginInfoByUsername(String username);

    LoginInfoDTO loginInfoByUserId(Long userId);

    List<Long> menuIds(Long userId);

    /** 记录登录 IP/时间并写登录日志 */
    void recordLogin(Long userId, String ip);
}
