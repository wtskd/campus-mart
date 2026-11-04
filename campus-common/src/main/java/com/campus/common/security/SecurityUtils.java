package com.campus.common.security;

/**
 * 基于 ThreadLocal 的当前登录用户持有器
 *
 * <p>由 {@link UserContextInterceptor} 在请求进入时写入，请求结束时清理。</p>
 *
 * @author campus-mart
 */
public final class SecurityUtils {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private SecurityUtils() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static Long getUserId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getUserId();
    }

    public static Long requireUserId() {
        Long id = getUserId();
        if (id == null) {
            throw new com.campus.common.core.exception.ServiceException(401, "未登录或登录已过期");
        }
        return id;
    }

    public static String getUsername() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getUsername();
    }

    public static LoginUser requireUser() {
        LoginUser u = HOLDER.get();
        if (u == null) {
            throw new com.campus.common.core.exception.ServiceException(401, "未登录或登录已过期");
        }
        return u;
    }

    /**
     * 是否具备某个权限标识，支持通配符。
     *
     * <p>规则（按 {@code :} 分段逐段比较）：</p>
     * <ul>
     *   <li>{@code *:*:*} 拥有全部权限（超级管理员）</li>
     *   <li>{@code item:order:*} 匹配 {@code item:order:list/confirm/...}</li>
     *   <li>{@code item:*:list} 匹配 {@code item:item:list}、{@code item:order:list}</li>
     * </ul>
     */
    public static boolean hasPerm(String perm) {
        LoginUser u = HOLDER.get();
        if (u == null) {
            return false;
        }
        if (u.isAdmin()) {
            return true;
        }
        if (perm == null || perm.isEmpty() || u.getPerms() == null) {
            return false;
        }
        if (u.getPerms().contains(perm)) {
            return true;
        }
        for (String owned : u.getPerms()) {
            if (matches(owned, perm)) {
                return true;
            }
        }
        return false;
    }

    /** 判断权限模式 pattern（可含 *）是否覆盖目标权限 target */
    private static boolean matches(String pattern, String target) {
        if (pattern == null || pattern.isEmpty()) {
            return false;
        }
        if (pattern.equals(target)) {
            return true;
        }
        String[] ps = pattern.split(":", -1);
        String[] ts = target.split(":", -1);
        if (ps.length != ts.length) {
            return false;
        }
        for (int i = 0; i < ps.length; i++) {
            if ("*".equals(ps[i])) {
                continue;
            }
            if (!ps[i].equals(ts[i])) {
                return false;
            }
        }
        return true;
    }
}
