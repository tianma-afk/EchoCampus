ALTER TABLE feedback ALTER COLUMN landmark_id DROP NOT NULL;
COMMENT ON COLUMN feedback.landmark_id IS '地标ID（逻辑外键，新增地标建议时可为空）';
