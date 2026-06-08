CREATE TABLE IF NOT EXISTS feedback (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    landmark_id UUID NOT NULL,
    user_id UUID NOT NULL,
    feedback_type VARCHAR(32) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    admin_id UUID,
    resolve_time TIMESTAMPTZ,
    resolve_note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_feedback PRIMARY KEY (id)
);
COMMENT ON TABLE feedback IS '用户反馈表';
COMMENT ON COLUMN feedback.landmark_id IS '地标ID（逻辑外键）';
COMMENT ON COLUMN feedback.user_id IS '用户ID（逻辑外键）';
COMMENT ON COLUMN feedback.feedback_type IS '反馈类型';
COMMENT ON COLUMN feedback.content IS '反馈正文';
COMMENT ON COLUMN feedback.status IS '状态';
COMMENT ON COLUMN feedback.admin_id IS '处理管理员ID（逻辑外键）';
COMMENT ON COLUMN feedback.resolve_time IS '处理时间';
COMMENT ON COLUMN feedback.resolve_note IS '处理备注';
