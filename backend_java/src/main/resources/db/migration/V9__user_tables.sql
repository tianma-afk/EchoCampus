CREATE TABLE IF NOT EXISTS "user" (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    nickname VARCHAR(50) NOT NULL,
    email VARCHAR(200) NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_user PRIMARY KEY (id),
    CONSTRAINT uk_user_email UNIQUE (email)
);
COMMENT ON TABLE "user" IS '用户表';
COMMENT ON COLUMN "user".nickname IS '用户昵称';
COMMENT ON COLUMN "user".email IS '邮箱';
COMMENT ON COLUMN "user".password_hash IS '密码哈希值';
