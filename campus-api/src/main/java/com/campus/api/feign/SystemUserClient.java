package com.campus.api.feign;

import com.campus.api.dto.LoginInfoDTO;
import com.campus.common.core.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 调用 campus-system 服务（认证专用内部接口）
 *
 * @author campus-mart
 */
@FeignClient(name = "campus-system", contextId = "systemUserClient", path = "/system/user")
public interface SystemUserClient {

    /** 按用户名查询登录信息（含密码哈希、角色、权限） */
    @GetMapping("/inner/login-info/{username}")
    Result<LoginInfoDTO> getLoginInfoByUsername(@PathVariable("username") String username);

    /** 按用户ID查询登录信息 */
    @GetMapping("/inner/login-info/id/{userId}")
    Result<LoginInfoDTO> getLoginInfoByUserId(@PathVariable("userId") Long userId);

    /** 查询用户拥有的菜单ID集合（动态菜单） */
    @GetMapping("/inner/menu-ids/{userId}")
    Result<List<Long>> getMenuIds(@PathVariable("userId") Long userId);

    /** 记录登录信息（IP、时间） */
    @GetMapping("/inner/record-login/{userId}")
    Result<Void> recordLogin(@PathVariable("userId") Long userId,
                             @RequestParam(value = "ip", required = false) String ip);
}
