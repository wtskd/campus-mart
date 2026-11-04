package com.campus.api.feign;

import com.campus.api.dto.MenuNodeDTO;
import com.campus.common.core.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 调用 campus-system 的菜单内部接口（auth 生成动态路由用）
 *
 * @author campus-mart
 */
@FeignClient(name = "campus-system", contextId = "systemMenuClient", path = "/system/menu")
public interface SystemMenuClient {

    /** 当前用户可见的菜单树 */
    @GetMapping("/inner/tree/{userId}")
    Result<List<MenuNodeDTO>> getUserMenuTree(@PathVariable("userId") Long userId);
}
