package com.smartcrew.agent.common.auth;

/**
 * 当前线程认证上下文。
 */
public final class AuthContextHolder {

    /**
     * 线程级用户上下文。
     */
    private static final ThreadLocal<AuthenticatedUser> HOLDER = new ThreadLocal<>();

    private AuthContextHolder() {
    }

    /**
     * 设置当前用户。
     */
    public static void set(AuthenticatedUser user) {
        HOLDER.set(user);
    }

    /**
     * 获取当前用户。
     */
    public static AuthenticatedUser get() {
        return HOLDER.get();
    }

    /**
     * 清理当前线程上下文。
     */
    public static void clear() {
        HOLDER.remove();
    }
}
