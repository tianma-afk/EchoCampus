CREATE TABLE image (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    landmark_id UUID NOT NULL,
    is_vectorized BOOLEAN DEFAULT FALSE,
    file_ext VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT pk_image PRIMARY KEY (id)
);
COMMENT ON TABLE image IS '图片表';
COMMENT ON COLUMN image.landmark_id IS '逻辑外键，关联landmark表';
COMMENT ON COLUMN image.is_vectorized IS '是否已完成向量化';
COMMENT ON COLUMN image.file_ext IS '文件扩展名（不含点），如jpg、png、webp';

ALTER TABLE landmark ADD COLUMN cover_image_id UUID;
COMMENT ON COLUMN landmark.cover_image_id IS '逻辑外键，关联image表，封面图片';

ALTER TABLE landmark DROP COLUMN cover_img;
