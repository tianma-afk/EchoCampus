CREATE TABLE IF NOT EXISTS comment (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    landmark_id UUID NOT NULL,
    user_id UUID NOT NULL,
    parent_id UUID,
    content VARCHAR(500) NOT NULL,
    like_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_comment PRIMARY KEY (id)
);
COMMENT ON TABLE comment IS '评论表';
COMMENT ON COLUMN comment.landmark_id IS '所属地标ID';
COMMENT ON COLUMN comment.user_id IS '评论用户ID';
COMMENT ON COLUMN comment.parent_id IS '父评论ID，NULL为顶级评论';
COMMENT ON COLUMN comment.content IS '评论内容';
COMMENT ON COLUMN comment.like_count IS '点赞数';

CREATE TABLE IF NOT EXISTS comment_like (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    comment_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_comment_like PRIMARY KEY (id),
    CONSTRAINT uk_comment_like_user UNIQUE (comment_id, user_id)
);
COMMENT ON TABLE comment_like IS '评论点赞记录';
COMMENT ON COLUMN comment_like.comment_id IS '被点赞的评论ID';
COMMENT ON COLUMN comment_like.user_id IS '点赞用户ID';
