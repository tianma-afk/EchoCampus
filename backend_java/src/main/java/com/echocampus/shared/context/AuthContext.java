package com.echocampus.shared.context;

import com.echocampus.shared.enums.RoleEnum;

public class AuthContext {

    private static final ThreadLocal<AuthContext> HOLDER = new ThreadLocal<>();

    private final String userId;
    private final RoleEnum role;

    private AuthContext(String userId, RoleEnum role) {
        this.userId = userId;
        this.role = role;
    }

    public static void set(String userId, RoleEnum role) {
        HOLDER.set(new AuthContext(userId, role));
    }

    public static AuthContext get() {
        AuthContext ctx = HOLDER.get();
        if (ctx == null) {
            throw new IllegalStateException("未设置鉴权上下文");
        }
        return ctx;
    }

    public static void clear() {
        HOLDER.remove();
    }

    public String getUserId() { return userId; }
    public RoleEnum getRole() { return role; }

    public boolean isSuperAdmin() {
        return role == RoleEnum.SUPER_ADMIN;
    }
}
