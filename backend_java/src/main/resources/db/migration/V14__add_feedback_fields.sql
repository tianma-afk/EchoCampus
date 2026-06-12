ALTER TABLE feedback ADD COLUMN IF NOT EXISTS upload_url VARCHAR(500);
ALTER TABLE feedback ADD COLUMN IF NOT EXISTS correct_landmark_name VARCHAR(200);

COMMENT ON COLUMN feedback.upload_url IS '反馈图片URL';
COMMENT ON COLUMN feedback.correct_landmark_name IS '用户校正的地标名称';
