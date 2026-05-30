CREATE TABLE task (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    task_type varchar(32) NOT NULL,
    alg_task_id VARCHAR(128),
    task_status varchar(32) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT pk_task PRIMARY KEY (id)
)
COMMENT ON TABLE task IS '任务表';
COMMENT ON COLUMN task.task_type IS '任务类型';
COMMENT ON COLUMN task.alg_task_id IS '算法任务ID';
COMMENT ON COLUMN task.task_status IS '任务状态';