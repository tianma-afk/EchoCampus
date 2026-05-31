ALTER TABLE task ADD COLUMN search_result JSONB;
COMMENT ON COLUMN task.search_result IS '图像搜索结果JSON';
