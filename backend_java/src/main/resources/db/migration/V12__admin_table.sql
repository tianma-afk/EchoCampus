CREATE TABLE IF NOT EXISTS admin (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_admin PRIMARY KEY (id),
    CONSTRAINT uk_admin_username UNIQUE (username),
    CONSTRAINT uk_admin_email UNIQUE (email)
);
COMMENT ON TABLE admin IS '管理员表';
COMMENT ON COLUMN admin.username IS '登录用户名';
COMMENT ON COLUMN admin.password_hash IS '密码哈希值';
COMMENT ON COLUMN admin.email IS '邮箱';
