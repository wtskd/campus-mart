package com.campus.common.security;

import com.campus.common.core.exception.ServiceException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 权限校验切面
 *
 * <p>拦截 {@link RequiresPerm} 注解，校验当前登录用户是否具备所需权限标识。</p>
 *
 * @author campus-mart
 */
@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(requiresPerm)")
    public void check(JoinPoint point, RequiresPerm requiresPerm) {
        LoginUser user = SecurityUtils.get();
        if (user == null) {
            throw new ServiceException(401, "未登录或登录已过期");
        }
        if (user.isAdmin()) {
            return;
        }
        String[] perms = requiresPerm.value();
        boolean pass = requiresPerm.requireAll()
                ? Arrays.stream(perms).allMatch(SecurityUtils::hasPerm)
                : Arrays.stream(perms).anyMatch(SecurityUtils::hasPerm);
        if (!pass) {
            throw new ServiceException(403, "没有操作权限：" + String.join(",", perms));
        }
    }
}
